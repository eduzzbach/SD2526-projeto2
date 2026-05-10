package sd2526.trab.impl.rest.clients;

import java.util.Collection;
import java.util.Set;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.impl.api.java.AdminUsers;
import sd2526.trab.impl.api.rest.RestAdminUsers;

public class RestAdminUsersClient extends RestClient implements AdminUsers {

	public RestAdminUsersClient(String serverURI) {
		super(serverURI, RestUsers.PATH);
	}

	@Override
	public Result<Set<String>> checkUsers(Collection<String> names) {
		return super.reTry( () -> doCheckUsers(names));
	}

	
	private Result<Set<String>> doCheckUsers(Collection<String> names) {
		return super.toJavaResult( target
				.path(RestAdminUsers.ADMIN)
				.request()
				.accept( MediaType.APPLICATION_JSON)
				.post( Entity.json( names )), new GenericType<Set<String>>() {});
	}

}
