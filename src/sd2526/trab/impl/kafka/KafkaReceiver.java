package sd2526.trab.impl.kafka;

import java.util.List;
import java.util.Scanner;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaReceiver
{
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);

		System.out.println("Provide the topic name:");
		String topic = sc.nextLine().trim();

		sc.close();
	
		KafkaUtils.createTopic(topic); //Attempts to create a topic (might already exist)
		
		KafkaSubscriber subscriber = KafkaSubscriber.createSubscriber("localhost:9092, kafka:9092", List.of(topic));
		
		subscriber.start( new RecordProcessor() {
			@Override
			public void onReceive(ConsumerRecord<String, String> r) {
				System.out.println( r.topic() + " , " +  r.offset() + " -> " + r.value());
			}
		});
	}
}