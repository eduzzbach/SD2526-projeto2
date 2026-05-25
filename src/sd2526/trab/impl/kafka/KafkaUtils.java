package sd2526.trab.impl.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;

public class KafkaUtils {

	public static void createTopics(List<String> topics) {
		for(String topic: topics) {
			createTopic(topic);
		}
	}

	private static final String DEFAULT_BOOTSTRAP = "localhost:9092,kafka:9092";

	public static void createTopic(String topic) {
		createTopic(topic, DEFAULT_BOOTSTRAP);
	}

	public static void createTopic(String topic, String bootstrapServers) {
		createTopic(topic, bootstrapServers, 1, 1);
	}

	public static void createTopic(String topic, int numPartitions, int replicationFactor) {
		createTopic(topic, DEFAULT_BOOTSTRAP, numPartitions, replicationFactor);
	}

	public static void createTopic(String topic, String bootstrapServers, int numPartitions, int replicationFactor) {

		try (AdminClient client = create(bootstrapServers)) {

			List<NewTopic> list = new ArrayList<NewTopic>();
			list.add(new NewTopic(topic, numPartitions, (short) replicationFactor));
			
			CreateTopicsResult result = client.createTopics(list);
			
			result.all().get();
			System.err.printf("Topic %s was created successfully\n", topic);


		} catch (ExecutionException x) {
			if (x.getCause() instanceof TopicExistsException) {
				System.err.printf("Topic: %s already exists...\n", topic);
			} else {
				x.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static private AdminClient create(String bootstrapServers) {
		Properties props = new Properties();
		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
		return AdminClient.create(props);
	}
	
}