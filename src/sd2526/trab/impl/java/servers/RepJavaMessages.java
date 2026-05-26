package sd2526.trab.impl.java.servers;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import static sd2526.trab.api.java.Result.ErrorCode.BAD_REQUEST;
import static sd2526.trab.api.java.Result.ErrorCode.FORBIDDEN;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;
import static sd2526.trab.api.java.Result.ErrorCode.NOT_FOUND;
import static sd2526.trab.api.java.Result.error;
import static sd2526.trab.api.java.Result.ok;
import sd2526.trab.impl.db.DB;
import sd2526.trab.impl.kafka.ReplicationEvent;
import sd2526.trab.impl.rest.servers.RestRepMessagesServer;
import sd2526.trab.impl.rest.servers.VersionHeaderHandler;
import sd2526.trab.impl.utils.Sleep;
import sd2526.trab.impl.utils.SyncPoint;

public class RepJavaMessages extends JavaMessages {

	private static final Logger Log = Logger.getLogger(RepJavaMessages.class.getName());
	private static final long REMOVED_ENTRY_TOMBSTONE_MS = 120000;
	private static final long DELETED_MESSAGE_TOMBSTONE_MS = 300000;
	private static final int INBOX_ENTRY_WAIT_RETRIES = 15;
	private static final int INBOX_ENTRY_WAIT_MS = 30;

	private final Cache<String, String> removedInboxEntries = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofMillis(REMOVED_ENTRY_TOMBSTONE_MS))
			.build();

	private final Cache<String, String> deletedMessageIds = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofMillis(DELETED_MESSAGE_TOMBSTONE_MS))
			.build();

	private RepJavaMessages() {
		super();
	}

	private void bumpCounterFromMessageId(String id) {
		var prefix = THIS_DOMAIN + "+";
		if (id == null || !id.startsWith(prefix))
			return;
		try {
			var n = Long.parseLong(id.substring(prefix.length()));
			counter.updateAndGet(cur -> Math.max(cur, n));
		} catch (NumberFormatException ignored) {
		}
	}

	private static String inboxKey(String mid, String recipient) {
		return mid + "|" + recipient;
	}

	private static String normalizeMid(String mid) {
		return mid == null ? null : mid.replace(' ', '+');
	}

	private void waitClientVersion() {
		var v = VersionHeaderHandler.clientVersion();
		if (v != null && v > 0)
			SyncPoint.getSyncPoint().waitForVersion(v);
	}

	private Result<String> replicatePostAndWait(ReplicationEvent event) {
		var offset = RestRepMessagesServer.publishReplication(event);
		if (offset < 0)
			return error(INTERNAL_ERROR);

		var res = SyncPoint.getSyncPoint().waitForResult(offset);
		if (res == null) {
			Log.warning(() -> "Missing replication result for offset " + offset);
			return error(INTERNAL_ERROR);
		}

		try {
			return error(ErrorCode.valueOf(res));
		} catch (IllegalArgumentException x) {
			return ok(res);
		}
	}

	private Result<Void> replicateVoidAndWait(ReplicationEvent event) {
		var offset = RestRepMessagesServer.publishReplication(event);
		if (offset < 0)
			return error(INTERNAL_ERROR);

		var res = SyncPoint.getSyncPoint().waitForResult(offset);
		if (res == null) {
			Log.warning(() -> "Missing replication result for offset " + offset);
			return error(INTERNAL_ERROR);
		}

		if ("OK".equals(res))
			return ok();

		try {
			return error(ErrorCode.valueOf(res));
		} catch (IllegalArgumentException x) {
			return error(INTERNAL_ERROR);
		}
	}

	@Override
	public Result<String> postMessage(String pwd, Message msg) {
		return getUser(msg.getSender(), pwd)
				.thenWith(user -> prepareAndReplicatePost(user, msg));
	}

	private Result<String> prepareAndReplicatePost(User sender, Message msg) {
		final String origin = msg.originId();
		return getCachedMessage(origin).mapValue(Message::getId).orElse(() -> {
			msg.setSender("%s <%s@%s>".formatted(sender.getDisplayName(), sender.getName(), sender.getDomain()));

			var localAddresses = getLocalRecipientAddresses(msg);
			var remoteAddresses = getRemoteRecipientAddresses(msg);

			Set<String> knownLocal = Set.of();
			Set<String> unknownLocal = Set.of();
			if (!localAddresses.isEmpty()) {
				var check = checkUsers(localAddresses);
				if (!check.isOK())
					return check.mapValue(x -> null);
				unknownLocal = check.value();
				knownLocal = new HashSet<>(localAddresses);
				knownLocal.removeAll(unknownLocal);
			}

			// Publish WITHOUT an ID; the final ID is derived from the Kafka offset
			// inside applyReplicationPost, ensuring uniqueness across all replicas.
			var event = ReplicationEvent.post(new Message(msg), knownLocal, unknownLocal);
			var result = replicatePostAndWait(event);
			if (!result.isOK())
				return result;

			// Now that we have the globally-unique ID, update the message and caches.
			msg.setId(result.value());
			messagesCache.put(origin, new Message(msg));
			messagesCache.put(msg.getId(), new Message(msg));

			if (!remoteAddresses.isEmpty())
				scheduleRemoteDelivery(msg, remoteAddresses);

			return result;
		});
	}

	private void scheduleRemoteDelivery(Message msg, Set<String> remoteAddresses) {
		var remoteTargets = remoteAddresses.stream().collect(
				Collectors.groupingBy(this::getDomain, Collectors.mapping(address -> address, Collectors.toSet())));

		for (var e : remoteTargets.entrySet()) {
			var domain = e.getKey();
			var domainRecipientAddresses = e.getValue();

			jobs.submit(domain, () -> {
				var res = reTry(() -> sd2526.trab.impl.java.clients.Clients.AdminMessagesClient.get(domain)
						.remotePostMessage(msg), REMOTE_COMM_DEADLINE);
				if (res.error() == ErrorCode.TIMEOUT) {
					for (var address : domainRecipientAddresses)
						postToLocalInboxes(Set.of(msg.senderAddress()), msg.cloneWithTimeout(address));
				}
			});
		}
	}

	private Result<Void> ensureKnownLocalRecipients(Set<String> addresses, Message msg) {
		if (deletedMessageIds.getIfPresent(msg.getId()) != null)
			return ok();

		return DB.transaction(hibernate -> {
			var existingMsg = hibernate.getOne(msg.getId(), Message.class);
			if (!existingMsg.isOK()) {
				if (existingMsg.error() != ErrorCode.NOT_FOUND)
					return error(existingMsg.error());

				var persistedMsg = hibernate.persistOne(new Message(msg));
				if (!persistedMsg.isOK() && persistedMsg.error() != ErrorCode.CONFLICT)
					return error(persistedMsg.error());
			}

			for (var address : addresses) {
				var recipient = getName(address);
				if (removedInboxEntries.getIfPresent(inboxKey(msg.getId(), recipient)) != null)
					continue;

				var entry = new InboxEntry(msg.getId(), recipient);
				var existingEntry = hibernate.getOne(entry, InboxEntry.class);
				if (existingEntry.isOK())
					continue;
				if (existingEntry.error() != ErrorCode.NOT_FOUND)
					return error(existingEntry.error());

				var persistedEntry = hibernate.persistOne(entry);
				if (!persistedEntry.isOK() && persistedEntry.error() != ErrorCode.CONFLICT)
					return error(persistedEntry.error());
			}

			return ok();
		});
	}

	private Result<Void> ensureMessageExists(Message msg) {
		if (deletedMessageIds.getIfPresent(msg.getId()) != null)
			return ok();

		return DB.transaction(hibernate -> {
			var existingMsg = hibernate.getOne(msg.getId(), Message.class);
			if (existingMsg.isOK())
				return ok();
			if (existingMsg.error() != ErrorCode.NOT_FOUND)
				return error(existingMsg.error());

			var persistedMsg = hibernate.persistOne(new Message(msg));
			if (!persistedMsg.isOK() && persistedMsg.error() != ErrorCode.CONFLICT)
				return error(persistedMsg.error());

			return ok();
		});
	}

	public Result<String> applyReplicationPost(ReplicationEvent event, long offset) {
		var msg = event.getMessage();
		if (msg == null)
			return error(BAD_REQUEST);

		// Local messages arrive without an ID; assign one from the Kafka offset so
		// all replicas derive the same, collision-free ID for the same event.
		if (msg.getId() == null)
			msg.setId("%s+%04d".formatted(THIS_DOMAIN, offset));

		if (deletedMessageIds.getIfPresent(msg.getId()) != null)
			return ok(msg.getId());

		bumpCounterFromMessageId(msg.getId());

		var persisted = ensureMessageExists(msg);
		if (!persisted.isOK())
			return error(persisted.error());

		if (!event.getKnownLocal().isEmpty()) {
			var delivery = ensureKnownLocalRecipients(event.getKnownLocal(), msg);
			if (!delivery.isOK())
				return error(delivery.error());
		}

		if (!event.getUnknownLocal().isEmpty() && messagesCache.getIfPresent(msg.getId()) == null)
			reportUnknownLocalRecipients(event.getUnknownLocal(), msg);

		messagesCache.put(msg.getId(), new Message(msg));

		return ok(msg.getId());
	}

	@Override
	public Result<Void> remotePostMessage(Message msg) {
		if (msg == null || msg.getId() == null)
			return error(BAD_REQUEST);

		var localAddresses = getLocalRecipientAddresses(msg);
		if (localAddresses.isEmpty())
			return ok();

		var check = checkUsers(localAddresses);
		if (!check.isOK())
			return check.mapToVoid();

		var unknownLocal = check.value();
		var knownLocal = new HashSet<>(localAddresses);
		knownLocal.removeAll(unknownLocal);

		var event = ReplicationEvent.post(new Message(msg), knownLocal, unknownLocal);
		return replicatePostAndWait(event).mapToVoid();
	}

	@Override
	public Result<Message> getInboxMessage(String name, String mid, String pwd) {
		waitClientVersion();
		final String normalizedMid = normalizeMid(mid);
		if (badParams(name, normalizedMid, pwd))
			return error(BAD_REQUEST);

		return getUser(name, pwd)
				.then(() -> awaitInboxEntry(normalizedMid, name))
				.then(() -> DB.getOne(normalizedMid, Message.class)
						.orElse(() -> {
							var cached = messagesCache.getIfPresent(normalizedMid);
							if (cached == null)
								return error(NOT_FOUND);
							return ok(new Message(cached));
						}));
	}

	private Result<Void> awaitInboxEntry(String mid, String recipient) {
		var key = inboxKey(mid, recipient);
		for (int i = 0; i <= INBOX_ENTRY_WAIT_RETRIES; i++) {
			if (removedInboxEntries.getIfPresent(key) != null)
				return error(NOT_FOUND);

			var entry = DB.getOne(new InboxEntry(mid, recipient), InboxEntry.class);
			if (entry.isOK())
				return ok();
			if (entry.error() != ErrorCode.NOT_FOUND)
				return error(entry.error());

			if (i < INBOX_ENTRY_WAIT_RETRIES)
				Sleep.ms(INBOX_ENTRY_WAIT_MS);
		}

		return error(NOT_FOUND);
	}

	@Override
	public Result<List<String>> getAllInboxMessages(String name, String pwd) {
		waitClientVersion();
		return super.getAllInboxMessages(name, pwd);
	}

	@Override
	public Result<List<String>> searchInbox(String name, String pwd, String query) {
		waitClientVersion();
		return super.searchInbox(name, pwd, query);
	}

	@Override
	public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
		final String normalizedMid = normalizeMid(mid);
		if (badParams(name, normalizedMid, pwd))
			return error(BAD_REQUEST);

		return getUser(name, pwd)
				.then(() -> replicateVoidAndWait(ReplicationEvent.remove(name, normalizedMid)));
	}

	public Result<Void> applyReplicationRemove(ReplicationEvent event) {
		if (event.getRecipient() == null || event.getMid() == null)
			return error(BAD_REQUEST);

		removedInboxEntries.put(inboxKey(event.getMid(), event.getRecipient()), event.getMid());

		var r = DB.deleteOne(new InboxEntry(event.getMid(), event.getRecipient()));
		if (r.isOK() || r.error() == ErrorCode.NOT_FOUND) {
			gcDeletedMessageCache.put(event.getMid(), event.getMid());
			return ok();
		}
		return error(r.error());
	}

	@Override
	public Result<Void> deleteMessage(String name, String mid, String pwd) {
		final String normalizedMid = normalizeMid(mid);
		return getUser(name, pwd)
				.then(() -> {
					var msgResult = DB.getOne(normalizedMid, Message.class);
					if (msgResult.error() == ErrorCode.NOT_FOUND) {
						var cachedMsg = messagesCache.getIfPresent(normalizedMid);
						if (cachedMsg == null)
							return ok();
						msgResult = ok(cachedMsg);
					} else if (!msgResult.isOK()) {
						return error(msgResult.error());
					}

					return msgResult.thenWith(msg -> name.equals(getName(msg.senderAddress())) ? ok(msg) : error(FORBIDDEN))
							.thenWith(msg -> replicateVoidAndWait(ReplicationEvent.delete(normalizedMid))
									.thenWith(v -> {
										scheduleAsyncDelete(msg);
										return ok();
									}));
				});
	}

	private void scheduleAsyncDelete(Message msg) {
		var domains = msg.getDestination().stream().map(r -> r.split("@")[1]).collect(Collectors.toSet());
		for (var domain : domains) {
			if (domain.equals(THIS_DOMAIN))
				continue;
			jobs.submit(domain, () -> reTry(
					() -> sd2526.trab.impl.java.clients.Clients.AdminMessagesClient.get(domain)
							.remoteDeleteMessage(msg.getId()),
					REMOTE_COMM_DEADLINE));
		}
	}

	public Result<Void> applyReplicationDelete(ReplicationEvent event) {
		if (event.getMid() == null)
			return error(BAD_REQUEST);

		deletedMessageIds.put(event.getMid(), event.getMid());

		return deleteFromLocalInbox(event.getMid());
	}

	@Override
	public Result<Void> remoteDeleteMessage(String mid) {
		if (mid == null || mid.isBlank())
			return error(BAD_REQUEST);

		return replicateVoidAndWait(ReplicationEvent.delete(mid));
	}

	public Result<Void> applyReplication(ReplicationEvent event) {
		return switch (event.getOp()) {
			case ReplicationEvent.REMOVE -> applyReplicationRemove(event);
			case ReplicationEvent.DELETE -> applyReplicationDelete(event);
			default -> error(INTERNAL_ERROR);
		};
	}

	private static RepJavaMessages instance;

	public static synchronized RepJavaMessages getInstance() {
		if (instance == null)
			instance = new RepJavaMessages();
		return instance;
	}
}
