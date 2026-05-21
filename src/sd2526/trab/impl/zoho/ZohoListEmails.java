package sd2526.trab.impl.zoho;

import java.util.List;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoEmail;

public class ZohoListEmails {

    public static void main(String[] args) throws Exception {

        List emails = Zoho.getInstance().listEmails();
        if (emails != null) {
            ZohoEmail currEmail;
            for (int i = 0; i < emails.size(); i++) {
                currEmail = (ZohoEmail) emails.get(i);
                System.out.printf("Email #%d\n", i);
                System.out.printf("FromAddress: %s\n", currEmail.fromAddress());
                System.out.printf("ToAddress: %s\n", currEmail.toAddress());
                System.out.printf("Summary: %s\n", currEmail.summary());
                System.out.printf("Subject: %s\n", currEmail.subject());
                System.out.printf("FolderId: %s, MessageId %s\n\n", currEmail.folderId(), currEmail.messageId());
            }
        } else
            System.err.println("Error...");
    }
}
