package sd2526.trab.impl.java.servers;

import static sd2526.trab.api.java.Result.error;
import static sd2526.trab.api.java.Result.ok;
import static sd2526.trab.api.java.Result.ErrorCode.BAD_REQUEST;
import static sd2526.trab.api.java.Result.ErrorCode.FORBIDDEN;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;
import static sd2526.trab.api.java.Result.ErrorCode.NOT_IMPLEMENTED;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.impl.db.DB;
import sd2526.trab.impl.kafka.ReplicationEvent;
import sd2526.trab.impl.rest.servers.RestRepMessagesServer;
import sd2526.trab.impl.rest.servers.VersionHeaderHandler;
import sd2526.trab.impl.utils.SyncPoint;

public class RepJavaMessages extends JavaMessages {

	private static final Logger Log = Logger.getLogger(RepJavaMessages.class.getName());

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
		if (res == null)
			return error(INTERNAL_ERROR);

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
		if (res == null)
			return error(INTERNAL_ERROR);

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
		if (!RestRepMessagesServer.isPrimary())
			return error(NOT_IMPLEMENTED);

		Log.info(() -> "postMessage : pwd = %s, msg = %s\n".formatted(pwd, msg));

		return getUser(msg.getSender(), pwd)
				.thenWith(user -> prepareAndReplicatePost(user, msg));
	}

	private Result<String> prepareAndReplicatePost(User sender, Message msg) {
		return getCachedMessage(msg.originId()).mapValue(Message::getId).orElse(() -> {
			syncCounterFromDatabase();
			msg.setId("%s+%04d".formatted(THIS_DOMAIN, counter.incrementAndGet()));
			messagesCache.put(msg.originId(), new Message(msg));
			msg.setSender("%s <%s@%s>".formatted(sender.getDisplayName(), sender.getName(), sender.getDomain()));
			messagesCache.put(msg.getId(), msg);

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

			var event = ReplicationEvent.post(new Message(msg), knownLocal, unknownLocal);
			var result = replicatePostAndWait(event);
			if (!result.isOK())
				return result;

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

	private void syncCounterFromDatabase() {
		var prefix = THIS_DOMAIN + "+";
		var sql = "SELECT m.id FROM Message m WHERE m.id LIKE '%s%%'".formatted(prefix);
		var ids = DB.select(sql, String.class);
		if (!ids.isOK())
			return;
		for (var id : ids.value())
			bumpCounterFromMessageId(id);
	}

	public Result<String> applyReplicationPost(ReplicationEvent event) {
		var msg = event.getMessage();
		if (msg == null || msg.getId() == null)
			return error(BAD_REQUEST);

		bumpCounterFromMessageId(msg.getId());

		var existing = DB.getOne(msg.getId(), Message.class);
		if (existing.isOK())
			return ok(msg.getId());

		if (!event.getKnownLocal().isEmpty())
			deliverToKnownLocalRecipients(event.getKnownLocal(), msg);

		if (!event.getUnknownLocal().isEmpty())
			reportUnknownLocalRecipients(event.getUnknownLocal(), msg);

		return ok(msg.getId());
	}

	@Override
	public Result<Message> getInboxMessage(String name, String mid, String pwd) {
		waitClientVersion();
		return super.getInboxMessage(name, mid, pwd);
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
		if (badParams(name, mid, pwd))
			return error(BAD_REQUEST);

		return getUser(name, pwd)
				.then(() -> {
					var entry = DB.getOne(new InboxEntry(mid, name), InboxEntry.class);
					if (!entry.isOK()) {
						if (entry.error() == ErrorCode.NOT_FOUND)
							return ok();
						return error(entry.error());
					}
					return replicateVoidAndWait(ReplicationEvent.remove(name, mid));
				});
	}

	public Result<Void> applyReplicationRemove(ReplicationEvent event) {
		var r = DB.deleteOne(new InboxEntry(event.getMid(), event.getRecipient()));
		if (r.isOK() || r.error() == ErrorCode.NOT_FOUND) {
			gcDeletedMessageCache.put(event.getMid(), event.getMid());
			return ok();
		}
		return error(r.error());
	}

	@Override
	public Result<Void> deleteMessage(String name, String mid, String pwd) {
		return getUser(name, pwd)
				.then(() -> DB.getOne(mid, Message.class))
				.thenWith(msg -> name.equals(getName(msg.senderAddress())) ? ok(msg) : error(FORBIDDEN))
				.thenWith(msg -> replicateVoidAndWait(ReplicationEvent.delete(mid))
						.thenWith(v -> {
							scheduleAsyncDelete(msg);
							return ok();
						}));
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
		return deleteFromLocalInbox(event.getMid());
	}

	public Result<Void> applyReplication(ReplicationEvent event) {
		return switch (event.getOp()) {
			case ReplicationEvent.POST -> applyReplicationPost(event).mapToVoid();
			case ReplicationEvent.REMOVE -> applyReplicationRemove(event);
			case ReplicationEvent.DELETE -> applyReplicationDelete(event);
			default -> error(NOT_IMPLEMENTED);
		};
	}

	private static RepJavaMessages instance;

	public static synchronized RepJavaMessages getInstance() {
		if (instance == null)
			instance = new RepJavaMessages();
		return instance;
	}
}
