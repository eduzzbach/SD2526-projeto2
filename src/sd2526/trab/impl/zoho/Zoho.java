package sd2526.trab.impl.zoho;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import sd2526.trab.impl.zoho.zoho.ZohoServiceFactory;
import sd2526.trab.impl.zoho.zoho.ZohoTokenManager;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoAccount;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoAccountReply;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoEmail;
import sd2526.trab.impl.zoho.zoho.msgs.ZohoEmailReply;
import sd2526.trab.impl.zoho.zoho.msgs.SendMailRequest;
import utils.JSON;

import java.util.List;

public class Zoho {
	static final String MAIL_API_BASE = "https://mail.zoho.eu/api";

	static final String CLIENT_ID     = "1000.9NM4UAXMY6W0COEKS2N44S92SLYW7I";
    static final String CLIENT_SECRET = "a2dbebf05778ad21c3ef253f7a5c2a8eb4f6546c23";
    static final String REFRESH_TOKEN = "1000.96c17788444d3219ecb99d343182c0e8.c25768306a96c6b729cbd579521144b3";
	private static final String ACCOUNTS = "/accounts";

    final OAuth20Service service;
    final ZohoTokenManager tokenManager;

    static Zoho instance;
    private volatile String accountId;
    
    private Zoho() {
    	service = ZohoServiceFactory.buildService(CLIENT_ID, CLIENT_SECRET);
        tokenManager = new ZohoTokenManager(service, REFRESH_TOKEN);
    }
 
    synchronized public static Zoho getInstance() {
    	if( instance == null )
    		instance = new Zoho();
    	return instance;
    }

    private String getAccountId() throws Exception {
        if (accountId == null) {
            synchronized (this) {
                if (accountId == null) {
                    var account = getAccount();
                    if (account == null || account.accountId() == null)
                        throw new IllegalStateException("Cannot resolve Zoho accountId");
                    accountId = account.accountId();
                }
            }
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
        		System.err.println( response.getCode() + "/" + response.getBody() );
        		return null;
        	}
        }
    }

    //TODO: Send Email POST
    public void postEmail() throws Exception {
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );

        OAuthRequest request = new OAuthRequest(Verb.POST, MAIL_API_BASE + ACCOUNTS + "/" + getAccountId() + "/messages");
        request.addHeader( "Content-Type", "application/json; charset=utf-8");
        request.setPayload( JSON.encode( new SendMailRequest("rd.cunha@zohomail.eu", "rd.cunha@zohomail.eu", "Testing SendEmail", "Did it work?") ) );
        //request.setPayload( JSON.encode( new SendMailRequest("rd.cunha@zohomail.eu", "rui.ddc@gmail.com", "Testing SendEmail", "Did it work?") ) );
        System.out.println("request: " + request.getStringPayload());
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
        	if( response.isSuccessful() ) {
                System.out.println("SUCCESFULL POST: " + response.getBody());
        		//var data = JSON.decode(body, ZohoEmailReply.class).data();
        		//if (data == null || data.isEmpty()) return null;
        		//return data;
        	}
        	else {
        		System.err.println("FAILED POST: " + response.getCode() + "/" + response.getBody() );
        		//return null;
        	}
        }
    }

    //TODO Delete Email DELETE
    public void deleteEmail() throws Exception {
        var accessToken = new OAuth2AccessToken( tokenManager.getValidAccessToken() );

        // Hardcoded folder and messge ids
        // FolderId: 8529913000000002014, MessageId 1777075237623004300
        OAuthRequest request = new OAuthRequest(Verb.DELETE, MAIL_API_BASE + ACCOUNTS + "/" + getAccountId() + "/folders/8529913000000002014/messages/1777075237623004300");
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
        	if( response.isSuccessful() ) {
                System.out.println("SUCCESFULL DELETE: " + response.getBody());
        		//var data = JSON.decode(body, ZohoEmailReply.class).data();
        		//if (data == null || data.isEmpty()) return null;
        		//return data;
        	}
        	else {
        		System.err.println("FAILED DELETE: " + response.getCode() + "/" + response.getBody() );
        		//return null;
        	}
        }
    }
}