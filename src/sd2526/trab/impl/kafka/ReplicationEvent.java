package sd2526.trab.impl.kafka;

import java.util.HashSet;
import java.util.Set;

import sd2526.trab.api.Message;

public class ReplicationEvent {

	public static final String POST = "POST";
	public static final String REMOVE = "REMOVE";
	public static final String DELETE = "DELETE";

	private String op;
	private Message message;
	private Set<String> knownLocal = new HashSet<>();
	private Set<String> unknownLocal = new HashSet<>();
	private String recipient;
	private String mid;

	public ReplicationEvent() {}

	public static ReplicationEvent post(Message message, Set<String> knownLocal, Set<String> unknownLocal) {
		var e = new ReplicationEvent();
		e.op = POST;
		e.message = message;
		e.knownLocal = knownLocal != null ? new HashSet<>(knownLocal) : new HashSet<>();
		e.unknownLocal = unknownLocal != null ? new HashSet<>(unknownLocal) : new HashSet<>();
		return e;
	}

	public static ReplicationEvent remove(String recipient, String mid) {
		var e = new ReplicationEvent();
		e.op = REMOVE;
		e.recipient = recipient;
		e.mid = mid;
		return e;
	}

	public static ReplicationEvent delete(String mid) {
		var e = new ReplicationEvent();
		e.op = DELETE;
		e.mid = mid;
		return e;
	}

	public String getOp() {
		return op;
	}

	public Message getMessage() {
		return message;
	}

	public Set<String> getKnownLocal() {
		return knownLocal;
	}

	public Set<String> getUnknownLocal() {
		return unknownLocal;
	}

	public String getRecipient() {
		return recipient;
	}

	public String getMid() {
		return mid;
	}
}
