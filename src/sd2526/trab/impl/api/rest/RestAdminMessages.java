package sd2526.trab.impl.api.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import sd2526.trab.api.Message;

public interface RestAdminMessages {
	final String ADMIN = "/admin";
	final String MID = "mid";
	final String NAME = "name";
	final String INBOX = "inbox";
	
	@POST
	@Path(ADMIN)
	@Consumes(MediaType.APPLICATION_JSON)
	void remotePostMessage(Message m);

	@DELETE
	@Path(ADMIN + "/{" + MID + "}")
	void remoteDeleteMessage(@PathParam(MID) String mid);
	
	@DELETE
	@Path(ADMIN + "/" + INBOX + "/{" + NAME + "}")
	void remoteDeleteUserInbox(@PathParam(NAME) String name);

}
