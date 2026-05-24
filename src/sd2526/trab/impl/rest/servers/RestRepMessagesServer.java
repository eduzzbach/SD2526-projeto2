package sd2526.trab.impl.rest.servers;

import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.java.servers.RepJavaMessages;
import sd2526.trab.impl.kafka.KafkaPublisher;
import sd2526.trab.impl.kafka.KafkaSubscriber;
import sd2526.trab.impl.kafka.KafkaUtils;
import sd2526.trab.impl.kafka.ReplicationEvent;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.utils.JSON;
import sd2526.trab.impl.utils.SyncPoint;

public class RestRepMessagesServer extends AbstractRestServer {
	public static final int PORT = 4568;
	private static final String DEFAULT_KAFKA_ADDR = "localhost:9092,kafka:9092";
	private static final String REPLICATION_TOPIC_PREFIX = "messages-replication-";

	private static final Logger Log = Logger.getLogger(RestRepMessagesServer.class.getName());

	private static KafkaPublisher publisher;
	private static String replicationTopic;

	RestRepMessagesServer() throws UnknownHostException {
		super(Log, Messages.SERVICE_NAME, PORT);
	}

	@Override
	void registerResources(ResourceConfig config) {
		config.register(RestRepMessagesResource.class);
		config.register(VersionHeaderHandler.class);
		config.register(PrimaryRedirectFilter.class);
	}

	public static boolean isPrimary() {
		if (Boolean.parseBoolean(System.getProperty("rep.primary", "false")))
			return true;
		return IP.hostname().startsWith("messages0.");
	}

	public static long publishReplication(ReplicationEvent event) {
		return publisher.publish(replicationTopic, JSON.encode(event));
	}

	private static void onReplicationRecord(ConsumerRecord<String, String> record) {
		try {
			var event = JSON.decode(record.value(), ReplicationEvent.class);
			String result;
			if (ReplicationEvent.POST.equals(event.getOp())) {
				var r = RepJavaMessages.getInstance().applyReplicationPost(event);
				result = r.isOK() ? r.value() : r.error().name();
			} else {
				var r = RepJavaMessages.getInstance().applyReplication(event);
				result = r.isOK() ? "OK" : r.error().name();
			}

			SyncPoint.getSyncPoint().setResult(record.offset(), result);
			Log.info(() -> "Applied %s offset=%d -> %s".formatted(event.getOp(), record.offset(), result));
		} catch (Exception x) {
			x.printStackTrace();
			SyncPoint.getSyncPoint().setResult(record.offset(), "INTERNAL_ERROR");
		}
	}

	private static void startKafkaReplication() {
		var kafkaAddr = System.getProperty("kafka.addr", DEFAULT_KAFKA_ADDR);
		replicationTopic = System.getProperty("kafka.rep.topic", REPLICATION_TOPIC_PREFIX + IP.domain());

		KafkaUtils.createTopic(replicationTopic, kafkaAddr);
		publisher = KafkaPublisher.createPublisher(kafkaAddr);

		KafkaSubscriber.createSubscriber(kafkaAddr, List.of(replicationTopic))
				.start(RestRepMessagesServer::onReplicationRecord);
	}

	public static void main(String[] args) throws UnknownHostException {
		startKafkaReplication();
		new RestRepMessagesServer().start();
	}
}
