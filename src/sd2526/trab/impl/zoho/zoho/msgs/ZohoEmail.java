package sd2526.trab.impl.zoho.zoho.msgs;

public record ZohoEmail(
        String summary,
        String subject,
        String folderId,
        String messageId,
        String toAddress,
        String fromAddress,
        String content
) {}