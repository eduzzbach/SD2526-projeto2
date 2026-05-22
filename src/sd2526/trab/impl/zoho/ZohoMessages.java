package sd2526.trab.impl.zoho;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoEmail;

public class ZohoMessages implements Messages, AdminMessages {
	private static final Logger LOG = Logger.getLogger(ZohoMessages.class.getName());

	private static ZohoMessages instance;

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
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toCollection(HashSet::new));

		return parsed.isEmpty() ? Set.of() : parsed;
	}

	private static Message toMessage(ZohoEmail email) {
		return new Message(email.messageId(), email.fromAddress(), parseAddresses(email.toAddress()), email.subject(), email.summary());
	}

	private static String recipientNameOf(String address) {
		if (address == null)
			return "";
		var clean = address.trim().toLowerCase(Locale.ROOT);
		int at = clean.indexOf('@');
		return at < 0 ? clean : clean.substring(0, at);
	}

	@Override
	public Result<String> postMessage(String pwd, Message msg) {
		if (msg == null){
      LOG.log(Level.SEVERE, "Null message received.");
			return Result.error(Result.ErrorCode.BAD_REQUEST);
    }
		try {
			return Zoho.getInstance().postEmail(msg);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Zoho postMessage failed: {0}", e.getMessage());
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<Message> getInboxMessage(String name, String mid, String pwd) {
		if (mid == null || mid.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		try {
			var email = Zoho.getInstance().getEmail(mid);
			if (!email.isOK())
				return Result.error(email);
			return Result.ok(toMessage(email.value()));
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Zoho getInboxMessage failed: {0}", e.getMessage());
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<List<String>> getAllInboxMessages(String name, String pwd) {
		try {
			var emails = Zoho.getInstance().listEmails();
			if (emails == null || emails.isEmpty())
				return Result.ok(List.of());

			var mids = emails.stream()
					.map(ZohoEmail::messageId)
					.filter(mid -> mid != null && !mid.isBlank())
					.toList();

			return Result.ok(mids);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Zoho getAllInboxMessages failed: {0}", e.getMessage());
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
		if (mid == null || mid.isBlank())
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		try {
			var email = Zoho.getInstance().getEmail(mid);
			if (!email.isOK())
				return Result.error(email);

			var deleted = Zoho.getInstance().deleteEmail(toMessage(email.value()));
			return deleted.isOK() ? Result.ok() : Result.error(deleted);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Zoho removeInboxMessage failed: {0}", e.getMessage());
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<Void> deleteMessage(String name, String mid, String pwd) {
		return removeInboxMessage(name, mid, pwd);
	}

	@Override
	public Result<List<String>> searchInbox(String name, String pwd, String query) {
		if (query == null)
			return Result.error(Result.ErrorCode.BAD_REQUEST);

		try {
			var emails = Zoho.getInstance().listEmails();
			if (emails == null || emails.isEmpty())
				return Result.ok(List.of());

			var q = query.toLowerCase(Locale.ROOT);
			var mids = emails.stream()
					.filter(e -> {
						var subject = e.subject() == null ? "" : e.subject().toLowerCase(Locale.ROOT);
						var summary = e.summary() == null ? "" : e.summary().toLowerCase(Locale.ROOT);
						return subject.contains(q) || summary.contains(q);
					})
					.map(ZohoEmail::messageId)
					.filter(mid -> mid != null && !mid.isBlank())
					.toList();

			return Result.ok(mids);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Zoho searchInbox failed: {0}", e.getMessage());
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<Void> remotePostMessage(Message m) {
		return postMessage("", m).mapToVoid();
	}

	@Override
	public Result<Void> remoteDeleteMessage(String mid) {
		return deleteMessage("", mid, "");
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
					var deleted = Zoho.getInstance().deleteEmail(toMessage(email));
					if (!deleted.isOK())
						return Result.error(deleted);
				}
			}

			return Result.ok();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Zoho remoteDeleteUserInbox failed: {0}", e.getMessage());
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}
}