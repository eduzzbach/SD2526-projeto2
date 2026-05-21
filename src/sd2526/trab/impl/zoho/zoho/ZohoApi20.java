package sd2526.trab.impl.zoho.zoho;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth2.bearersignature.BearerSignature;
import com.github.scribejava.core.oauth2.clientauthentication.ClientAuthentication;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;

public class ZohoApi20 extends DefaultApi20 {

    private ZohoApi20() {}

    private static class InstanceHolder {
        private static final ZohoApi20 INSTANCE = new ZohoApi20();
    }

    public static ZohoApi20 instance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return "https://accounts.zoho.eu/oauth/v2/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://accounts.zoho.eu/oauth/v2/auth";
    }

    @Override
    public BearerSignature getBearerSignature() {
        return ZohoBearerSignature.instance();
    }

    @Override
    public ClientAuthentication getClientAuthentication() {
        // Zoho expects client_id/secret in the request body, not Basic auth header
        return RequestBodyAuthenticationScheme.instance();
    }
}