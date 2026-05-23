package sd2526.trab.impl.kafka;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaPublisher {

	static public KafkaPublisher createPublisher(String addr) {
		Properties props = new Properties();

		// List of pairs hostname:port that allows to contact kafka servers
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, addr);

		// Class that can serialize the key format (string)
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		// Class that can serialize the value of events (string)
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		return new KafkaPublisher(new KafkaProducer<String, String>(props));
	}
	
	private final KafkaProducer<String, String> producer;

	private KafkaPublisher( KafkaProducer<String, String> producer ) {
		this.producer = producer;
	}

	public void close() {
		this.producer.close();
	}

	//This method is to publish to a topic that relies on partitioning.
	public long publish(String topic, String key, String value) {
		try {
			Future<RecordMetadata> promise = producer.send(new ProducerRecord<String, String>(topic, key, value));
			RecordMetadata rec = promise.get();
			System.out.println("Published to topic " + topic + " with offset " + rec.offset());
			return rec.offset();
		} catch (ExecutionException | InterruptedException x) {
			x.printStackTrace();
			return -1;
		}
	}
	
	//This method is to publish to a topic that has no partitioning.
	public long publish(String topic, String value) {
		try {
			Future<RecordMetadata> promise = producer.send(new ProducerRecord<String, String>(topic, value));
			RecordMetadata rec = promise.get();
			System.out.println("Published to topic " + topic + " with offset " + rec.offset());
			return rec.offset();
		} catch (ExecutionException | InterruptedException x) {
			x.printStackTrace();
			return -1;
		}
	}
}