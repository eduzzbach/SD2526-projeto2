package sd2526.trab.impl.api.java;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Result;

public interface AdminMessages {

	Result<Void> remotePostMessage(Message m);

	Result<Void> remoteDeleteMessage(String mid);

	Result<Void> remoteDeleteUserInbox(String name);
}
