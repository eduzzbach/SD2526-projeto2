package sd2526.trab.impl.zoho.zoho.msgs;

public record ZohoAccount(
	    String incomingUserName,
	    String firstName,
	    String accountId,
	    String mailboxAddress,
	    String accountDisplayName,
	    String role,
	    String gender,
	    String accountName,
	    String displayName,
	    String primaryEmailAddress,
	    boolean enabled
	) {}