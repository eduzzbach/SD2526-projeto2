package sd2526.trab.impl.zoho;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoEmail;

public class ZohoMessages implements Messages, AdminMessages {
	private static final Logger LOG = Logger.getLogger(ZohoMessages.class.getName());

	// Subject format:
	// SD||<msgId>||<creationTime>||<sender>||<dest1;dest2>||<originalSubject>
	private static final String SD_PREFIX = "SD||";
	private static final String SD_SEP = "||";

	private static ZohoMessages instance;
	private int counter = 0;
	private final String thisDomain;

	private ZohoMessages() {
		var host = IP.hostname();
		var dot = host.indexOf('.');
		thisDomain = dot >= 0 ? host.substring(dot + 1) : host;
	}

	public static synchronized ZohoMessages getInstance() {
		if (instance == null)
			instance = new ZohoMessages();
		return instance;
	}

	private static String recipientNameOf(String address) {
		if (address == null)
			return "";
		var clean = address.trim().toLowerCase(Locale.ROOT);
		int at = clean.indexOf('@');
		return at < 0 ? clean : clean.substring(0, at);
	}

	private Result<Void> validateUser(String name, String pwd) {
		if (name == null || name.isBlank() || pwd == null)
			return Result.error(Result.ErrorCode.BAD_REQUEST);
		return Clients.UsersClient.get().getUser(name, pwd).mapToVoid();
	}

	private synchronized String nextMsgId() {
		counter++;
		return String.format("%s+%04d", thisDomain, counter);
	}

	private static String encodeSubject(Message msg) {
		var dests = msg.getDestination().stream().collect(Collectors.joining(";"));
		var subj = msg.getSubject() == null ? "" : msg.getSubject();
		return SD_PREFIX + msg.getId() + SD_SEP + msg.getCreationTime() + SD_SEP + msg.getSender() + SD_SEP + dests + SD_SEP + subj;
	}

	private static Message decodeEmail(ZohoEmail email) {
		var subject = email.subject();
		if (subject == null || !subject.startsWith(SD_PREFIX))
			return null;

		var parts = subject.substring(SD_PREFIX.length()).split("\\|\\|", 5);
		if (parts.length < 5)
			return null;

		try {
			var msgId = parts[0];
			var creationTime = Long.parseLong(parts[1]);
			var sender = unescapeHtml(parts[2]);
			var dests = new HashSet<>(Arrays.asList(parts[3].split(";")));
			var msgSubject = unescapeHtml(parts[4]);
			var body = email.content() != null ? email.content() : email.summary();
			var msg = new Message(msgId, sender, dests, msgSubject, body == null ? "" : unescapeHtml(body));
			msg.setCreationTime(creationTime);
			return msg;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "decodeEmail failed: {0}", e.getMessage());
			return null;
		}
	}

	private static String unescapeHtml(String value) {
		if (value == null || value.isEmpty())
			return value;
		return value
				.replace("&lt;", "<")
				.replace("&gt;", ">")
				.replace("&quot;", "\"")
				.replace("&#39;", "'")
				.replace("&amp;", "&");
	}

	private List<ZohoEmail> listSdEmails() {
		try {
			var all = Zoho.getInstance().listEmails();
			if (all == null)
				return List.of();
			return all.stream().filter(e -> e.subject() != null && e.subject().startsWith(SD_PREFIX)).toList();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Zoho listEmails failed: {0}", e.getMessage());
			return List.of();
		}
	}

	private void storeInZoho(Message msg) {
		try {
			// Avoid stale collisions across runs (e.g., old ourorg0+0001 entries).
			deleteFromZoho(msg.getId());

			var accountEmail = Zoho.getInstance().getAccountEmail();
			if (accountEmail == null || accountEmail.isBlank()) {
				LOG.log(Level.SEVERE, "No Zoho account email, cannot persist message");
				return;
			}

			var syntheticMsg = new Message(accountEmail, accountEmail, encodeSubject(msg), msg.getContents() == null ? "" : msg.getContents());
			var result = Zoho.getInstance().postEmail(syntheticMsg);
			if (!result.isOK())
				LOG.log(Level.WARNING, "Zoho postEmail failed for msgId={0}", msg.getId());
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "storeInZoho failed: {0}", e.getMessage());
		}
	}

	private void deleteFromZoho(String msgId) {
		for (var email : listSdEmails()) {
			var rest = email.subject().substring(SD_PREFIX.length());
			var sep = rest.indexOf(SD_SEP);
			var storedId = sep >= 0 ? rest.substring(0, sep) : rest;
			if (msgId.equals(storedId)) {
				try {
					Zoho.getInstance().deleteEmailById(email.folderId(), email.messageId());
				} catch (Exception ex) {
					LOG.log(Level.WARNING, "deleteFromZoho failed for {0}: {1}", new Object[] { msgId, ex.getMessage() });
				}
			}
		}
	}

	@Override
	public Result<String> postMessage(String pwd, Message msg) {
		if (msg == null)
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		var userCheck = validateUser(msg.senderName(), pwd);
		if (!userCheck.isOK())
			return Result.error(userCheck);

		msg.setId(nextMsgId());
		storeInZoho(msg);
		return Result.ok(msg.getId());
	}

	@Override
	public Result<Message> getInboxMessage(String name, String mid, String pwd) {
		if (mid == null || mid.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		var userCheck = validateUser(name, pwd);
		if (!userCheck.isOK())
			return Result.error(userCheck);

		var target = name.toLowerCase(Locale.ROOT);
		for (var email : listSdEmails()) {
			var parts = email.subject().substring(SD_PREFIX.length()).split("\\|\\|", 5);
			if (parts.length < 4 || !mid.equals(parts[0]))
				continue;

			boolean belongs = Arrays.stream(parts[3].split(";")).map(ZohoMessages::recipientNameOf).anyMatch(target::equals);
			if (!belongs)
				continue;

			try {
				var full = Zoho.getInstance().getEmail(email.folderId(), email.messageId());
				if (full.isOK()) {
					var decoded = decodeEmail(full.value());
					if (decoded != null)
						return Result.ok(decoded);
				}
			} catch (Exception e) {
				LOG.log(Level.WARNING, "getEmail failed, using list entry: {0}", e.getMessage());
			}

			var decoded = decodeEmail(email);
			if (decoded != null)
				return Result.ok(decoded);
		}
		return Result.error(Result.ErrorCode.NOT_FOUND);
	}

	@Override
	public Result<List<String>> getAllInboxMessages(String name, String pwd) {
		var userCheck = validateUser(name, pwd);
		if (!userCheck.isOK())
			return Result.error(userCheck);

		var target = name.toLowerCase(Locale.ROOT);
		var mids = listSdEmails().stream().filter(e -> {
			var parts = e.subject().substring(SD_PREFIX.length()).split("\\|\\|", 5);
			if (parts.length < 4)
				return false;
			return Arrays.stream(parts[3].split(";")).map(ZohoMessages::recipientNameOf).anyMatch(target::equals);
		}).map(e -> e.subject().substring(SD_PREFIX.length()).split("\\|\\|", 2)[0]).sorted().toList();

		return Result.ok(mids);
	}

	@Override
	public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
		if (mid == null || mid.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		var userCheck = validateUser(name, pwd);
		if (!userCheck.isOK())
			return Result.error(userCheck);

		var target = name.toLowerCase(Locale.ROOT);
		boolean removed = false;
		for (var email : listSdEmails()) {
			var parts = email.subject().substring(SD_PREFIX.length()).split("\\|\\|", 5);
			if (parts.length < 4 || !mid.equals(parts[0]))
				continue;

			boolean belongs = Arrays.stream(parts[3].split(";")).map(ZohoMessages::recipientNameOf).anyMatch(target::equals);
			if (!belongs)
				continue;

			try {
				Zoho.getInstance().deleteEmailById(email.folderId(), email.messageId());
				removed = true;
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Zoho deleteEmailById failed: {0}", e.getMessage());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		}
		return removed ? Result.ok() : Result.error(Result.ErrorCode.NOT_FOUND);
	}

	@Override
	public Result<Void> deleteMessage(String name, String mid, String pwd) {
		return removeInboxMessage(name, mid, pwd);
	}

	@Override
	public Result<List<String>> searchInbox(String name, String pwd, String query) {
		if (query == null)
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		var userCheck = validateUser(name, pwd);
		if (!userCheck.isOK())
			return Result.error(userCheck);

		var target = name.toLowerCase(Locale.ROOT);
		var q = query.toLowerCase(Locale.ROOT);
		var mids = listSdEmails().stream().filter(e -> {
			var parts = e.subject().substring(SD_PREFIX.length()).split("\\|\\|", 5);
			if (parts.length < 5)
				return false;
			boolean belongs = Arrays.stream(parts[3].split(";")).map(ZohoMessages::recipientNameOf).anyMatch(target::equals);
			if (!belongs)
				return false;
			var msgSubj = parts[4].toLowerCase(Locale.ROOT);
			var summary = e.summary() == null ? "" : e.summary().toLowerCase(Locale.ROOT);
			return msgSubj.contains(q) || summary.contains(q);
		}).map(e -> e.subject().substring(SD_PREFIX.length()).split("\\|\\|", 2)[0]).sorted().toList();

		return Result.ok(mids);
	}

	@Override
	public Result<Void> remotePostMessage(Message m) {
		if (m == null)
			return Result.error(Result.ErrorCode.BAD_REQUEST);
		storeInZoho(m);
		return Result.ok();
	}

	@Override
	public Result<Void> remoteDeleteMessage(String mid) {
		if (mid == null || mid.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);
		deleteFromZoho(mid);
		return Result.ok();
	}

	@Override
	public Result<Void> remoteDeleteUserInbox(String name) {
		if (name == null || name.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		var target = name.toLowerCase(Locale.ROOT);
		listSdEmails().stream().filter(e -> {
			var parts = e.subject().substring(SD_PREFIX.length()).split("\\|\\|", 5);
			if (parts.length < 4)
				return false;
			return Arrays.stream(parts[3].split(";")).map(ZohoMessages::recipientNameOf).anyMatch(target::equals);
		}).forEach(email -> {
			try {
				Zoho.getInstance().deleteEmailById(email.folderId(), email.messageId());
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Zoho deleteEmailById failed: {0}", e.getMessage());
			}
		});

		return Result.ok();
	}
}
