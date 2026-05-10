package sd2526.trab.api.rest;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import sd2526.trab.api.User;

@Path(RestUsers.PATH)
public interface RestUsers {

	final String PATH = "/users";
	final String QUERY = "query";
	final String NAME = "name";
	final String PWD = "pwd";
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	String postUser(User user);
	
	@GET
	@Path("/{" + NAME +"}")
	@Produces(MediaType.APPLICATION_JSON)
	User getUser(@PathParam(NAME) String name, @QueryParam(PWD) String pwd);
	
	@PUT
	@Path("/{" + NAME +"}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	User updateUser(@PathParam(NAME) String name, @QueryParam(PWD) String pwd, User info);
	
	@DELETE
	@Path("/{" + NAME +"}")
	@Produces(MediaType.APPLICATION_JSON)
	User deleteUser(@PathParam(NAME) String name, @QueryParam(PWD) String pwd);
	
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	List<User> searchUsers(@QueryParam(NAME) String name, @QueryParam(PWD) String pwd, @QueryParam(QUERY) String pattern);	
}
