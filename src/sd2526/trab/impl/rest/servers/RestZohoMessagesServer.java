package sd2526.trab.impl.rest.servers;

import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.impl.zoho.ZohoMessages;

public class RestZohoMessagesServer extends AbstractRestServer {
	public static final int PORT = 4567;
	
	private static final Logger log = Logger.getLogger(RestZohoMessagesServer.class.getName());

	RestZohoMessagesServer() throws UnknownHostException{
		super(log, ZohoMessages.SERVICE_NAME, PORT);
	}

	@Override
	void registerResources(ResourceConfig config) {
		config.register(ZohoMessagesResource.class);
	}

	public static void main(String[] args) throws UnknownHostException{
		new RestZohoMessagesServer().start();
	}
}