package sd2526.trab.impl.zoho.zoho;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;

public class ZohoServiceFactory {

    public static OAuth20Service buildService(String clientId, String clientSecret) {
        return new ServiceBuilder(clientId)
                .apiSecret(clientSecret)
                .defaultScope("ZohoMail.messages.ALL,ZohoMail.accounts.READ")
                .debug()
                .build(ZohoApi20.instance());
    }
}