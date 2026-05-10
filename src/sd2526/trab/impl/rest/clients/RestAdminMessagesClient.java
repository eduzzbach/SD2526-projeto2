package sd2526.trab.impl.rest.clients;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.api.rest.RestAdminMessages;

public class RestAdminMessagesClient extends RestClient implements AdminMessages {

	public RestAdminMessagesClient(String serverURI) {
		super(serverURI, RestMessages.PATH);
	}

	@Override
	public Result<Void> remotePostMessage(Message m) {
		return super.reTry( () -> doRemotePostMessage(m) );
	}

	@Override
	public Result<Void> remoteDeleteMessage(String mid) {
		return super.reTry( () -> doRemoteDeleteMessage(mid) );
	}

	@Override
	public Result<Void> remoteDeleteUserInbox(String name) {
		return super.reTry( () -> doRemoteDeleteUserInbox(name) );
	}
	
	private Result<Void> doRemotePostMessage(Message msg) {
		return super.toJavaResult( target
				.path(RestAdminMessages.ADMIN)
				.request()
				.post( Entity.entity(msg, MediaType.APPLICATION_JSON )));
	}

	private Result<Void> doRemoteDeleteMessage(String mid) {
		return super.toJavaResult( target
				.path(RestAdminMessages.ADMIN)
				.path( mid )
				.request()
				.delete());
	}
	
	private Result<Void> doRemoteDeleteUserInbox(String name) {
		return super.toJavaResult( target
				.path(RestAdminMessages.ADMIN)
				.path(RestAdminMessages.INBOX)
				.path( name )
				.request()
				.delete());
	}
}
