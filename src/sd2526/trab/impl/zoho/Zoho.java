package sd2526.trab.impl.zoho;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Result;
import sd2526.trab.impl.utils.JSON;
import sd2526.trab.impl.zoho.zoho.ZohoServiceFactory;
import sd2526.trab.impl.zoho.zoho.ZohoTokenManager;
import sd2526.trab.impl.zoho.zoho.msgs.SendMailRequest;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoAccount;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoAccountReply;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoEmail;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoEmailReply;

public class Zoho {
	static final String MAIL_API_BASE = "https://mail.zoho.eu/api";
    private static final Logger LOG = Logger.getLogger(Zoho.class.getName());

    //David credentials
     
	static final String CLIENT_ID     = "1000.9NM4UAXMY6W0COEKS2N44S92SLYW7I";
    static final String CLIENT_SECRET = "a2dbebf05778ad21c3ef253f7a5c2a8eb4f6546c23";
     static final String REFRESH_TOKEN = "1000.96c17788444d3219ecb99d343182c0e8.c25768306a96c6b729cbd579521144b3";
    
    //Eduardo credentials
    /* 
    static final String CLIENT_ID     = "1000.IEOBWL4ZGFX57B6S42MVQYKON5RQRA";
    static final String CLIENT_SECRET = "92daa571c5e4851dc5d02f8528ec151724afea9e58";
    static final String REFRESH_TOKEN = "1000.96c17788444d3219ecb99d343182c0e8.c25768306a96c6b729cbd579521144b3";
    */

	private static final String ACCOUNTS = "/accounts";

    final OAuth20Service service;
    final ZohoTokenManager tokenManager;

    private static final Zoho instance = new Zoho();
    private String accountId;

    private Zoho() {
    	service = ZohoServiceFactory.buildService(CLIENT_ID, CLIENT_SECRET);
        tokenManager = new ZohoTokenManager(service, REFRESH_TOKEN);
    }
 
    public static Zoho getInstance() {
	    return instance;
    }

    private synchronized String getAccountId() throws Exception {
        if (accountId == null) {
            var account = getAccount();
            if (account == null || account.accountId() == null)
                throw new IllegalStateException("Cannot resolve Zoho accountId");
            accountId = account.accountId();
        }
        return accountId;
    }

    public ZohoAccount getAccount() throws Exception {
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );

        OAuthRequest request = new OAuthRequest(Verb.GET, MAIL_API_BASE + ACCOUNTS);
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
        	if( response.isSuccessful() ) {
        		var body = response.getBody();
        		var data = JSON.decode(body, ZohoAccountReply.class).data();
        		if (data == null || data.isEmpty()) return null;
        		return data.get(0);
        	}
        	else {
        		System.err.println( response.getCode() + "/" + response.getBody() );
        		return null;
        	}
        }
    }

    public List<ZohoEmail> listEmails() throws Exception {
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );

        OAuthRequest request = new OAuthRequest(Verb.GET, MAIL_API_BASE + ACCOUNTS + "/" + getAccountId() + "/messages/view");
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
        	if( response.isSuccessful() ) {
        		var body = response.getBody();
        		var data = JSON.decode(body, ZohoEmailReply.class).data();
        		if (data == null || data.isEmpty()) return null;
        		return data;
        	}
        	else {
        		System.err.println( response.getCode() + "/" + response.getBody());
        		return null;
        	}
        }
    }

    public Result<ZohoEmail> getEmail(String messageId) throws Exception {
        if (messageId == null || messageId.isBlank())
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        var emails = listEmails();
        if (emails == null || emails.isEmpty())
            return Result.error(Result.ErrorCode.NOT_FOUND);

        for (var email : emails) {
            if (messageId.equals(email.messageId())) {
                if (email.folderId() != null && !email.folderId().isBlank())
                    return getEmail(email.folderId(), messageId);
                return Result.ok(email);
            }
        }

        return Result.error(Result.ErrorCode.NOT_FOUND);
    }

    public Result<ZohoEmail> getEmail(String folderId, String messageId) throws Exception {
        if (folderId == null || folderId.isBlank() || messageId == null || messageId.isBlank())
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());
        var url = MAIL_API_BASE + ACCOUNTS + "/" + getAccountId() + "/folders/" + folderId + "/messages/" + messageId;
        OAuthRequest request = new OAuthRequest(Verb.GET, url);
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (response.isSuccessful()) {
                var data = JSON.decode(response.getBody(), ZohoEmailReply.class).data();
                if (data != null && !data.isEmpty())
                    return Result.ok(data.get(0));
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            if (response.getCode() == 404)
                return Result.error(Result.ErrorCode.NOT_FOUND);

            LOG.log(Level.SEVERE, "FAILED GET EMAIL: {0}/{1}", new Object[] { response.getCode(), response.getBody() });
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }


    public Result<String> postEmail(Message msg) throws Exception {
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );
        OAuthRequest request = new OAuthRequest(Verb.POST, MAIL_API_BASE + ACCOUNTS + "/" + getAccountId() + "/messages");

        request.addHeader( "Content-Type", "application/json; charset=utf-8");
        request.setPayload( JSON.encode( new SendMailRequest(
                msg.senderAddress(),
                msg.getDestination().isEmpty() ? null : msg.getDestination().iterator().next(),
                msg.getSubject(),
                msg.getContents() ) ) );

        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if( response.isSuccessful() ) {
                var data = JSON.decode(response.getBody(), ZohoEmailReply.class).data();
                if (data != null && !data.isEmpty()) {
                    var created = data.get(0);
                if (created.messageId() != null && !created.messageId().isBlank()) {
                        LOG.log(Level.INFO, "SUCCESFULL POST: {0}", response.getBody());
                        return Result.ok(created.messageId());
                }
                }
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            else {
                LOG.log(Level.SEVERE, "FAILED POST: {0}/{1}", new Object[] { response.getCode(), response.getBody() });
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
    }


    public Result<String> deleteEmail(Message msg) throws Exception {
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );
        OAuthRequest request = new OAuthRequest(Verb.DELETE, MAIL_API_BASE + ACCOUNTS + "/" + getAccountId() + "/messages");

         request.addHeader( "Content-Type", "application/json; charset=utf-8");
        request.setPayload( JSON.encode( new SendMailRequest(
                msg.senderAddress(),
                msg.getDestination().isEmpty() ? null : msg.getDestination().iterator().next(),
                msg.getSubject(),
                msg.getContents() ) ) );

        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if( response.isSuccessful() ) {
                var data = JSON.decode(response.getBody(), ZohoEmailReply.class).data();
                if (data != null && !data.isEmpty()) {
                    var created = data.get(0);
                if (created.messageId() != null && !created.messageId().isBlank()) {
                        LOG.log(Level.INFO, "SUCCESFULL DELETE: {0}", response.getBody());
                        return Result.ok(created.messageId());
                }
                }
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            else {
                LOG.log(Level.SEVERE, "FAILED DELETE: {0}/{1}", new Object[] { response.getCode(), response.getBody() });
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
    }
}