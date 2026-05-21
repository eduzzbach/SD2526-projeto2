package sd2526.trab.impl.zoho.zoho.msgs;

public record SendMailRequest(
        String fromAddress,
        String toAddress,
        String subject,
        String content
) {}
