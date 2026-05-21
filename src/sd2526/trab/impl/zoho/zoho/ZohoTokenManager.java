package sd2526.trab.impl.zoho.zoho;

import java.util.logging.Logger;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;


public class ZohoTokenManager {
	private static Logger Log = Logger.getLogger(ZohoTokenManager.class.getName());

    private final OAuth20Service service;
    private final String refreshToken;
    private OAuth2AccessToken cachedToken;
    private long tokenIssuedAt;

    public ZohoTokenManager(OAuth20Service service, String refreshToken) {
        this.service = service;
        this.refreshToken = refreshToken;
    }

    public String getValidAccessToken() throws Exception {
        if (cachedToken == null || isExpiredSoon(cachedToken)) {
            cachedToken = service.refreshAccessToken(refreshToken);
            tokenIssuedAt = System.currentTimeMillis();
            Log.info(() -> "Zoho access token refreshed...");
        }
        return cachedToken.getAccessToken();
    }

    private boolean isExpiredSoon(OAuth2AccessToken token) {
        if (token.getExpiresIn() == null) return true;
        long elapsedSeconds = (System.currentTimeMillis() - tokenIssuedAt) / 1000;
        return elapsedSeconds >= token.getExpiresIn() - 120;
    }
}