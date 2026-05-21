package sd2526.trab.impl.zoho.zoho;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.oauth2.bearersignature.BearerSignature;

public class ZohoBearerSignature implements BearerSignature {

    private static final ZohoBearerSignature INSTANCE = new ZohoBearerSignature();

    private ZohoBearerSignature() {}

    public static ZohoBearerSignature instance() {
        return INSTANCE;
    }

    @Override
    public void signRequest(String accessToken, OAuthRequest request) {
        request.addHeader("Authorization", "Zoho-oauthtoken " + accessToken);
    }
}