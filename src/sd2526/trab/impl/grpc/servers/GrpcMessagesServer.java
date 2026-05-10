package sd2526.trab.impl.grpc.servers;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import sd2526.trab.api.java.Messages;

public class GrpcMessagesServer extends AbstractGrpcServer {
public static final int PORT = 14567;
	
	private static Logger Log = Logger.getLogger(GrpcMessagesServer.class.getName());

	public GrpcMessagesServer() {
		super( Log, Messages.SERVICE_NAME, PORT);
	}
	
	@Override
	protected List<GrpcController> controllers(String uri) {
		return List.of( new GrpcMessagesController(), new GrpcAdminMessagesController() );
	}
	
	public static void main(String[] args) {
		try {
			new GrpcMessagesServer().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
}
