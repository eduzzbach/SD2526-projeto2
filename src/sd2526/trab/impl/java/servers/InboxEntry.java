package sd2526.trab.impl.java.servers;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ForeignKey;
import sd2526.trab.api.Message;

@Entity
public class InboxEntry {

	@Id 
	String mid;
	
	@Id
	String recipient;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "id", 
        insertable = false, 
        updatable = false,
        foreignKey = @ForeignKey(
            name = "FK_INBOX_MESSAGE",
            foreignKeyDefinition = "FOREIGN KEY (id) REFERENCES Message(id) ON DELETE RESTRICT"
        )
    )
    private Message dummy;
	
	public InboxEntry() {}
	
	public InboxEntry(String mid, String recipient) {
		this.mid = mid;
		this.recipient = recipient;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(mid, recipient);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InboxEntry other = (InboxEntry) obj;
		return Objects.equals(mid, other.mid) && Objects.equals(recipient, other.recipient);
	}

	public String getMid() {
		return mid;
	}

	public void setMid(String mid) {
		this.mid = mid;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	@Override
	public String toString() {
		return "InboxEntry [mid=" + mid + ", recipient=" + recipient + "]";
	}
}
