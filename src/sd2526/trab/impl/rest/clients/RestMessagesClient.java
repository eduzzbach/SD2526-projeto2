package sd2526.trab.impl.rest.clients;

import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.rest.RestMessages;

public class RestMessagesClient extends RestClient implements Messages {

		public RestMessagesClient( String serverURI ) {
			super(serverURI, RestMessages.PATH);
		}

		@Override
		public Result<String> postMessage(String pwd, Message msg) {
			return super.reTry( () -> doPostMessage(pwd, msg) );
		}

		@Override
		public Result<Message> getInboxMessage(String name, String mid, String pwd) {
			return super.reTry( () -> doGetInboxMessage(name, mid, pwd) );
		}

		@Override
		public Result<List<String>> getAllInboxMessages(String name, String pwd) {
			return super.reTry( () -> doGetAllInboxMessages(name, pwd) );
		}

		@Override
		public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
			return super.reTry( () -> doRemoveInboxMessage(name, mid, pwd) );
		}

		@Override
		public Result<Void> deleteMessage(String name, String mid, String pwd) {
			return super.reTry( () -> doDeleteMessage(name, mid, pwd) );
		}

		@Override
		public Result<List<String>> searchInbox(String name, String pwd, String query) {
			return super.reTry( () -> doSearchInbox(name, pwd, query) );
		}


		private Result<String> doPostMessage(String pwd, Message msg) {
			return super.toJavaResult( target
					.queryParam( RestMessages.PWD, pwd )
					.request()
					.accept( MediaType.APPLICATION_JSON )
					.post( Entity.entity(msg, MediaType.APPLICATION_JSON )), String.class);
		}

		private Result<Message> doGetInboxMessage(String name, String mid, String pwd) {
			return super.toJavaResult( target
					.path(RestMessages.MBOX)
					.path( name)
					.path( mid )
					.queryParam( RestMessages.PWD, pwd )
					.request()
					.accept( MediaType.APPLICATION_JSON )
					.get(), Message.class);
		}

		private Result<List<String>> doGetAllInboxMessages(String name, String pwd) {
			return super.toJavaResult( target
					.path(RestMessages.MBOX)
					.path( name)
					.queryParam( RestMessages.PWD, pwd )
					.request()
					.accept( MediaType.APPLICATION_JSON )
					.get(), new GenericType<List<String>>() {});
		}

		private Result<Void> doRemoveInboxMessage(String name, String mid, String pwd) {
			return super.toJavaResult( target
					.path(RestMessages.MBOX)
					.path( name)
					.path( mid )
					.queryParam( RestMessages.PWD, pwd )
					.request()
					.delete());
		}

		private Result<Void> doDeleteMessage(String name, String mid, String pwd) {
			return super.toJavaResult( target
					.path( name)
					.path( mid )
					.queryParam( RestMessages.PWD, pwd )
					.request()
					.delete());
		}
		
		public Result<List<String>> doSearchInbox(String name, String pwd, String query) {
			return super.toJavaResult( target
					.path(RestMessages.MBOX)
					.path( name)
					.queryParam( RestMessages.PWD, pwd )
					.queryParam( RestMessages.QUERY, query )
					.request()
					.accept( MediaType.APPLICATION_JSON )
					.get(), new GenericType<List<String>>() {});
		}
}

