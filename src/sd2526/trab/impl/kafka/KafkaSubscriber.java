package sd2526.trab.impl.kafka;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaSubscriber {
	static public KafkaSubscriber createSubscriber( String addr, List<String> topics) {

		Properties props = new Properties();

		// List of pairs hostname:port that allows to contact kafka servers
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, addr);

		// Values for setting the subscription mode (check the documentation on https://kafka.apache.org)
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		//props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

		props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());

		// Class that can serialize the key format (string)
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		// Class that can serialize the value of events (string)
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		// generates a new Kafka Subscriber with its own Kafka Consumer
		return new KafkaSubscriber( new KafkaConsumer<String, String>(props), topics);
	}

	private static final long POLL_TIMEOUT = 1L;

	final KafkaConsumer<String, String> consumer;

	public KafkaSubscriber(KafkaConsumer<String, String> consumer, List<String> topics) {
		this.consumer = consumer;
		this.consumer.subscribe(topics);
	}

	public void consume(SubscriberListener listener) {
		for (;;) {
			ConsumerRecords<String,String> records = consumer.poll(Duration.ofSeconds(POLL_TIMEOUT));
			Iterator<ConsumerRecord<String,String>> iterator = records.iterator();
			while(iterator.hasNext() ) {
				ConsumerRecord<String, String> r = iterator.next();
				listener.onReceive(r.topic(), r.key(), r.value());
			};
		}
	}
	
	public static interface SubscriberListener {
		void onReceive(String topic, String key, String value);
	}

	public void start(RecordProcessor recordProcessor) {
		new Thread( () -> {
			for (;;) {
				ConsumerRecords<String,String> records = consumer.poll(Duration.ofSeconds(POLL_TIMEOUT));
				Iterator<ConsumerRecord<String,String>> iterator = records.iterator();
				
				while(iterator.hasNext()) {
					ConsumerRecord<String,String> r = iterator.next();
					recordProcessor.onReceive(r);
				}
			}
		}).start();
	}
}