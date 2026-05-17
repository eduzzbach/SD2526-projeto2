package sd2526.trab.impl.rest.servers;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.impl.discovery.Discovery;
import sd2526.trab.impl.java.servers.AbstractServer;
import sd2526.trab.impl.utils.IP;



public abstract class AbstractRestServer extends AbstractServer {
	// changed from http to https
	private static final String SERVER_BASE_URI = "https://%s:%s%s";
	private static final String REST_CTX = "/rest";
	private int port;

	protected AbstractRestServer(Logger log, String service, int port) throws UnknownHostException {
		// Changed from IP address to hostname
		// BEFORE: super(log, service, String.format(SERVER_BASE_URI, IP.hostAddress(), port, REST_CTX));
		super(log, 
		service, 
		SERVER_BASE_URI.formatted(InetAddress.getLocalHost().getHostName(), port, REST_CTX));
		this.port = port;
	}

	protected void start() {
		try{
		ResourceConfig config = new ResourceConfig();
		
		registerResources( config );
		
		// Added SSL context
		// BEFORE: JdkHttpServerFactory.createHttpServer( URI.create(serverURI.replace(IP.hostAddress(), INETADDR_ANY)), config, );
		var uri = URI.create("https://0.0.0.0:%s/rest".formatted(port));
 		JdkHttpServerFactory.createHttpServer( uri, config, javax.net.ssl.SSLContext.getDefault());

		if( service != null )
			Discovery.getInstance().announce(serviceName(), super.serverURI);
		
		Log.info(String.format("%s Server ready @ %s\n",  service, serverURI));
		} catch (Exception e) {
			Log.severe("Failed to start server: %s".formatted(e.getMessage()));
		}
	}
	
	abstract void registerResources( ResourceConfig config );
}