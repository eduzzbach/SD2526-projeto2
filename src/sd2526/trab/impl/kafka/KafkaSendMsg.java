package sd2526.trab.impl.kafka;

import java.util.Scanner;

public class KafkaSendMsg
{
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);

		System.out.println("Provide the topic name:");
		String topic = sc.nextLine().trim();

		System.out.println("Provide the message to send:");
		String value = sc.nextLine().trim();

		sc.close();

		KafkaUtils.createTopic(topic); //Attempts to create a topic (might already exist)
		
		KafkaPublisher publisher = KafkaPublisher.createPublisher("localhost:9092, kafka:9092");
		long offset = publisher.publish(topic, value);
	
		if(offset >= 0) {
			System.out.println("Message published with offset (sequece number): " + offset);
		} else {
			System.out.println("Failed to publish message");
		}
	}

}