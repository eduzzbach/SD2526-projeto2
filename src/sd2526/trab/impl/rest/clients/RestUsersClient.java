package sd2526.trab.impl.rest.clients;

import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.rest.RestUsers;

public class RestUsersClient extends RestClient implements Users {

	public RestUsersClient( String serverURI ) {
		super(serverURI, RestUsers.PATH);
	}
	
	@Override
	public Result<String> postUser(User user) {
		return super.reTry( () -> doPostUser( user ) );
	}

	@Override
	public Result<User> getUser(String name, String pwd) {
		return super.reTry( () -> doGetUser( name, pwd ));
	}

	@Override
	public Result<User> updateUser(String name, String pwd, User info) {
		return super.reTry( () -> doUpdateUser( name, pwd, info));
	}

	@Override
	public Result<User> deleteUser(String name, String pwd) {
		return super.reTry( () -> doDeleteUser( name, pwd));
	}

	@Override
	public Result<List<User>> searchUsers(String name, String pwd, String pattern) {
		return super.reTry( () -> doSearchUsers( name, pwd, pattern ));
	}
	

	private Result<String> doPostUser(User user) {
		var r = target.request()
				.accept( MediaType.APPLICATION_JSON)
				.post(Entity.entity(user, MediaType.APPLICATION_JSON));
				
		return super.toJavaResult(r, String.class);
	}
		
	private Result<User> doGetUser(String userId, String password) {
		var r = target.path( userId)
				.queryParam(RestUsers.PWD, password)
				.request()
				.accept( MediaType.APPLICATION_JSON)
				.get();
				
		return super.toJavaResult(r, User.class);
	}

	private Result<User> doUpdateUser(String user, String password, User info) {
		var r = target.path( user)
				.queryParam(RestUsers.PWD, password)
				.request()
				.accept( MediaType.APPLICATION_JSON)
				.put(Entity.entity(info, MediaType.APPLICATION_JSON));
				
		return super.toJavaResult(r, User.class);
	}

	private Result<User> doDeleteUser(String user, String password) {
		var r = target.path( user)
				.queryParam(RestUsers.PWD, password)
				.request()
				.accept( MediaType.APPLICATION_JSON)
				.delete();
				
		return super.toJavaResult(r, User.class);
	}

	private Result<List<User>> doSearchUsers(String user, String pwd, String pattern) {
		var r = target
				.queryParam(RestUsers.PWD, pwd)
				.queryParam(RestUsers.NAME, user)
				.queryParam(RestUsers.QUERY, pattern)
				.request()
				.accept( MediaType.APPLICATION_JSON)
				.get();
		
		return super.toJavaResult(r, new GenericType<List<User>>() {});
	}
}
