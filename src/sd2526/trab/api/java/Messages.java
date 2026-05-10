package sd2526.trab.api.java;

import java.util.List;

import sd2526.trab.api.Message;

public interface Messages {
	String SERVICE_NAME = "Messages";

	/**
	 * Posts a new message to the server, associating it to the inbox of every individual destination.
	 * An outgoing message should be modified before delivering it, by assigning it an ID, and by changing the
	 * sender to be in the format "display name <name@domain>".
	 * 
	 * Posting exactly the same message more than once, should have no effect but still succeed by returning its ID.
	 * 
	 * NOTE: there might be some destinations that are not from the local domain (see grading for 
	 * how addressing this feature is valued).
	 * 
	 * @param msg the message object to be posted to the server
	 * @param pwd password of the user posting the message
	 * 
	 * @return (OK, the unique identifier for the posted message)
	 * FORBIDDEN - if the sender does not exist, or, if the pwd is not correct (NOTE: sender can be in the form 
	 * "name" or "name@domain");
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters, or a malformed message).
	 */
	Result<String> postMessage(String pwd, Message msg);
	
	/**
	 * Obtains a single message from the inbox of a user.
	 * 
	 * @param name the owner of the inbox
	 * @param mid the identifier of the message to be retrieved
	 * @param pwd password of the owner of the inbox
	 * @return (OK, the message if it exists);
	 *  FORBIDEN - if the name does not exist or if the password is not correct;
	 *  NOT_FOUND - if the message does not exist;
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters).
	 */
	Result<Message> getInboxMessage(String name, String mid, String pwd);

	/**
	 * Returns all the messages from the inbox of a user.
	 * 
	 * @param name the owner of the inbox
	 * @param pwd password of the owner of the inbox
	 * @return (OK, list of ids, potentially empty);
	 *  FORBIDDEN if the name does not exist or if the password is not correct.
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters).
	 */
	Result<List<String>> getAllInboxMessages(String name, String pwd);

	/**
	 * Removes a single message from the inbox of a user.
	 * 
	 * @param name the owner of the inbox
	 * @param mid the identifier of the message to be deleted
	 * @param pwd password of the owner of the inbox
	 * @return NO_CONTENT if ok
	 *  FORBIDDEN if the name does not exist or if the password is not correct.
	 * NOT_FOUND -  if the message does not exist;
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters).
	 */
	Result<Void> removeInboxMessage(String name, String mid, String pwd);

	/**
	 * Deletes a message from all inboxes and servers where it is stored. A message should only be deleted if it was
	 * posted less than 30 seconds ago.
	 * 
	 * The deletion can be executed asynchronously and does not generate an error message if the
	 * message does not exist.
	 * 
	 * @param name the sender of the message to be deleted
	 * @param mid the identifier of the message to be deleted
	 * @param pwd password of the sender
	 * @return NO_CONTENT if ok
	 * FORBIDDEN if the sender does not exist or if the password is not correct;
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters).
	 */
	Result<Void> deleteMessage(String name, String mid, String pwd);

	/**
	 * Returns the list of identifiers of the messages in the inbox of a user that match a query. A message matches 
	 * when the query is a substring of the subject or contents, case-insensitive. 
	 * 
	 * @param name the owner of the inbox
	 * @param pwd password of the owner of the inbox
	 * @param query - substring to search
	 * @return (OK, list of hits, may be empty.) . 
	 * FORBIDDEN if the name does not exist or if the password is not correct.
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters).
	 */
	Result<List<String>> searchInbox(String name, String pwd, String query);	

}
