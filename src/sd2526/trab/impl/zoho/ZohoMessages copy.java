package sd2526.trab.impl.zoho;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
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
	private static final String THIS_DOMAIN = IP.domain();

	private static ZohoMessages instance;
	private final AtomicLong counter = new AtomicLong(0L);
	private final ConcurrentMap<String, String> serviceToZohoIds = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> zohoToServiceIds = new ConcurrentHashMap<>();

	private ZohoMessages() {}

	public static synchronized ZohoMessages getInstance() {
		if (instance == null) {
			instance = new ZohoMessages();
		}
		return instance;
	}

	private static Set<String> parseAddresses(String addresses) {
		if (addresses == null || addresses.isBlank())
			return Set.of();

		var parsed = Arrays.stream(addresses.split("[,;]"))
				.map(String::trim)
				.map(ZohoMessages::normalizeAddress)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toCollection(HashSet::new));

		return parsed.isEmpty() ? Set.of() : parsed;
	}

	private static String normalizeAddress(String address) {
		if (address == null)
			return "";
		var clean = address.trim();
		int start = clean.indexOf('<');
		int end = clean.indexOf('>');
		if (start >= 0 && end > start)
			clean = clean.substring(start + 1, end).trim();
		return clean;
	}

	private static Message toMessage(ZohoEmail email, String serviceMid) {
		return new Message(serviceMid, email.fromAddress(), parseAddresses(email.toAddress()), email.subject(), email.summary());
	}

	private static String recipientNameOf(String address) {
		if (address == null)
			return "";
		var clean = normalizeAddress(address).toLowerCase(Locale.ROOT);
		int at = clean.indexOf('@');
		return at < 0 ? clean : clean.substring(0, at);
	}

	private static String senderNameOf(String sender) {
		if (sender == null)
			return "";
		var address = normalizeAddress(sender).toLowerCase(Locale.ROOT);
		int at = address.indexOf('@');
		return at < 0 ? address : address.substring(0, at);
	}

	private static boolean belongsToUser(ZohoEmail email, String user) {
		if (user == null || user.isBlank())
			return false;
		var targetName = user.toLowerCase(Locale.ROOT);
		return parseAddresses(email.toAddress()).stream()
				.map(ZohoMessages::recipientNameOf)
				.anyMatch(targetName::equals);
	}

	private Result<Void> validateUser(String name, String pwd) {
		if (name == null || name.isBlank() || pwd == null)
			return Result.error(Result.ErrorCode.BAD_REQUEST);
		return Clients.UsersClient.get().getUser(name, pwd).mapToVoid();
	}

	private String ensureServiceMid(Message msg) {
		if (msg.getId() == null || msg.getId().isBlank())
			msg.setId("%s+%04d".formatted(THIS_DOMAIN, counter.incrementAndGet()));
		return msg.getId();
	}

	private void rememberMidMapping(String serviceMid, String zohoMid) {
		if (serviceMid == null || serviceMid.isBlank() || zohoMid == null || zohoMid.isBlank())
			return;
		serviceToZohoIds.put(serviceMid, zohoMid);
		zohoToServiceIds.put(zohoMid, serviceMid);
	}

	private Result<String> sendToZoho(Message msg) {
		if (msg == null)
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		var serviceMid = ensureServiceMid(msg);
		try {
			var posted = Zoho.getInstance().postEmail(msg);
			if (!posted.isOK())
				return Result.error(posted);

			rememberMidMapping(serviceMid, posted.value());
			return Result.ok(serviceMid);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Zoho postMessage failed: {0}", e.getMessage());
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<String> postMessage(String pwd, Message msg) {
		if (msg == null || msg.getSender() == null || msg.getSender().isBlank()) {
			LOG.log(Level.SEVERE, "Null message/sender received.");
			return Result.error(Result.ErrorCode.BAD_REQUEST);
		}

		var senderName = senderNameOf(msg.getSender());
		return validateUser(senderName, pwd).thenWith(ignored -> sendToZoho(msg));
	}

	@Override
	public Result<Message> getInboxMessage(String name, String mid, String pwd) {
		if (mid == null || mid.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		return validateUser(name, pwd).thenWith(ignored -> {
			var zohoMid = serviceToZohoIds.get(mid);
			if (zohoMid == null)
				return Result.error(Result.ErrorCode.NOT_FOUND);

			try {
				var email = Zoho.getInstance().getEmail(zohoMid);
				if (!email.isOK())
					return Result.error(email);
				if (!belongsToUser(email.value(), name))
					return Result.error(Result.ErrorCode.NOT_FOUND);
				return Result.ok(toMessage(email.value(), mid));
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Zoho getInboxMessage failed: {0}", e.getMessage());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		});
	}

	@Override
	public Result<List<String>> getAllInboxMessages(String name, String pwd) {
		return validateUser(name, pwd).thenWith(ignored -> {
			try {
				var emails = Zoho.getInstance().listEmails();
				if (emails == null || emails.isEmpty())
					return Result.ok(List.of());

				var mids = emails.stream()
						.filter(email -> belongsToUser(email, name))
						.map(ZohoEmail::messageId)
						.map(zohoToServiceIds::get)
						.filter(mapped -> mapped != null && !mapped.isBlank())
						.toList();

				return Result.ok(mids);
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Zoho getAllInboxMessages failed: {0}", e.getMessage());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		});
	}

	@Override
	public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
		if (mid == null || mid.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		return validateUser(name, pwd).thenWith(ignored -> {
			var zohoMid = serviceToZohoIds.get(mid);
			if (zohoMid == null)
				return Result.error(Result.ErrorCode.NOT_FOUND);

			try {
				var email = Zoho.getInstance().getEmail(zohoMid);
				if (!email.isOK())
					return Result.error(email);
				if (!belongsToUser(email.value(), name))
					return Result.error(Result.ErrorCode.NOT_FOUND);

				var deleted = Zoho.getInstance().deleteEmail(toMessage(email.value(), mid));
				if (!deleted.isOK())
					return Result.error(deleted);

				serviceToZohoIds.remove(mid);
				zohoToServiceIds.remove(zohoMid);
				return Result.ok();
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Zoho removeInboxMessage failed: {0}", e.getMessage());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		});
	}

	@Override
	public Result<Void> deleteMessage(String name, String mid, String pwd) {
		return removeInboxMessage(name, mid, pwd);
	}

	@Override
	public Result<List<String>> searchInbox(String name, String pwd, String query) {
		if (query == null)
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		return validateUser(name, pwd).thenWith(ignored -> {
			try {
				var emails = Zoho.getInstance().listEmails();
				if (emails == null || emails.isEmpty())
					return Result.ok(List.of());

				var q = query.toLowerCase(Locale.ROOT);
				var mids = emails.stream()
						.filter(email -> belongsToUser(email, name))
						.filter(e -> {
							var subject = e.subject() == null ? "" : e.subject().toLowerCase(Locale.ROOT);
							var summary = e.summary() == null ? "" : e.summary().toLowerCase(Locale.ROOT);
							return subject.contains(q) || summary.contains(q);
						})
						.map(ZohoEmail::messageId)
						.map(zohoToServiceIds::get)
						.filter(mapped -> mapped != null && !mapped.isBlank())
						.toList();

				return Result.ok(mids);
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Zoho searchInbox failed: {0}", e.getMessage());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		});
	}

	@Override
	public Result<Void> remotePostMessage(Message m) {
		return sendToZoho(m).mapToVoid();
	}

	@Override
	public Result<Void> remoteDeleteMessage(String mid) {
		if (mid == null || mid.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		var zohoMid = serviceToZohoIds.get(mid);
		if (zohoMid == null)
			return Result.ok();

		try {
			var email = Zoho.getInstance().getEmail(zohoMid);
			if (!email.isOK())
				return Result.error(email);

			var deleted = Zoho.getInstance().deleteEmail(toMessage(email.value(), mid));
			if (!deleted.isOK())
				return Result.error(deleted);

			serviceToZohoIds.remove(mid);
			zohoToServiceIds.remove(zohoMid);
			return Result.ok();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Zoho remoteDeleteMessage failed: {0}", e.getMessage());
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<Void> remoteDeleteUserInbox(String name) {
		if (name == null || name.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		try {
			var emails = Zoho.getInstance().listEmails();
			if (emails == null || emails.isEmpty())
				return Result.ok();

			var targetName = name.toLowerCase(Locale.ROOT);
			for (var email : emails) {
				boolean belongsToUser = parseAddresses(email.toAddress()).stream()
						.map(ZohoMessages::recipientNameOf)
						.anyMatch(targetName::equals);

				if (belongsToUser) {
					var zohoMid = email.messageId();
					var serviceMid = zohoToServiceIds.getOrDefault(zohoMid, zohoMid);
					var deleted = Zoho.getInstance().deleteEmail(toMessage(email, serviceMid));
					if (!deleted.isOK())
						return Result.error(deleted);
					if (serviceMid != null)
						serviceToZohoIds.remove(serviceMid);
					if (zohoMid != null)
						zohoToServiceIds.remove(zohoMid);
				}
			}

			return Result.ok();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Zoho remoteDeleteUserInbox failed: {0}", e.getMessage());
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}
}