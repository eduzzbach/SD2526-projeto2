package sd2526.trab.impl.rest.servers;

import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.kafka.KafkaSubscriber;

public class RestRepMessagesServer extends AbstractRestServer {
	public static final int PORT = 4568;
	private static final String DEFAULT_KAFKA_ADDR = "localhost:9092,kafka:9092";
	private static final String DEFAULT_REPLICATION_TOPIC = "messages-replication";
	
	private static final Logger Log = Logger.getLogger(RestRepMessagesServer.class.getName());

	RestRepMessagesServer() throws UnknownHostException{
		super(Log, Messages.SERVICE_NAME, PORT);
	}

	@Override
	void registerResources(ResourceConfig config) {
		config.register(RestMessagesResource.class);
	}

	private static void startKafkaReplicationReceiver() {
		var kafkaAddr = System.getProperty("kafka.addr", DEFAULT_KAFKA_ADDR);
		var topic = System.getProperty("kafka.rep.topic", DEFAULT_REPLICATION_TOPIC);

		KafkaSubscriber.createSubscriber(kafkaAddr, List.of(topic)).start(
			record -> Log.info(() -> "Replication event topic=%s key=%s offset=%d"
				.formatted(record.topic(), record.key(), record.offset()))
		);
	}

	public static void main(String[] args) throws UnknownHostException{
		startKafkaReplicationReceiver();
		new RestRepMessagesServer().start();
	}
}