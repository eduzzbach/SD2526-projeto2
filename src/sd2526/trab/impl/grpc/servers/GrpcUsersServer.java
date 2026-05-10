package sd2526.trab.impl.grpc.servers;

import java.io.IOException;
import java.util.List;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;

import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import sd2526.trab.impl.grpc.servers.AbstractGrpcServer;

import sd2526.trab.api.java.Users;

public class GrpcUsersServer extends AbstractGrpcServer {
	public static final int PORT = 13456;

	private static final String GRPC_CTX = "/grpc";
	private static final String SERVER_BASE_URI = "grpc://%s:%s%s";
	
	private static Logger Log = Logger.getLogger(GrpcUsersServer.class.getName());

	public GrpcUsersServer() {
		super( Log, Users.SERVICE_NAME, PORT);
	}
	
	@Override
	protected List<GrpcController> controllers(String uri) {
		return List.of( new GrpcUsersController(), new GrpcAdminUsersController() );
	}
	
	public static void main(String[] args) throws Exception {
		// Accessing values of the default JVM properties regarding filenames containing server keystore and respective password
		String keyStoreFilename = System.getProperty("javax.net.ssl.keyStore");
		String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

		// Init instance of keystore and load with info from server keystore
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			try(FileInputStream input = new FileInputStream(keyStoreFilename)) {
			keystore.load(input, keyStorePassword.toCharArray());
		}

		// Create keymanagerfactory and load the keystore
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keystore, keyStorePassword.toCharArray());

		// SSL context that uses keymanagerfactory to access private and public key certificate of the server
		SslContext context = GrpcSslContexts.configure(SslContextBuilder.forServer(keyManagerFactory)).build();
 		GrpcUsersController stub = new GrpcUsersController();
 
		// Creates server instance
 		Server server = NettyServerBuilder.forPort(PORT).addService(stub).sslContext(context).build();
		
		// URL announced by the server using hostname instead of ip address
		String serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostName(), PORT, GRPC_CTX);

		Log.info(String.format("Users gRPC Server ready @ %s\n", serverURI));
		// Start gRPC server
 		server.start().awaitTermination();
		
		try {
			new GrpcUsersServer().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}