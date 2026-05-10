package sd2526.trab.impl.api.rest;

import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public interface RestAdminUsers {
	
	String ADMIN = "admin";

	@POST
	@Path(ADMIN)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Set<String> checkUsers( Set<String> addresses);
}
