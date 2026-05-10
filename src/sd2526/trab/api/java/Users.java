package sd2526.trab.api.java;

import java.util.List;

import sd2526.trab.api.User;

public interface Users {
	String SERVICE_NAME = "Users";
	
	/**
	 * Creates a new user in the local domain.
	 * 
	 * The operation succeeds when the name is not already in use; or, when the same user
	 * is already present in the system.
	 *  
	 * @param user - User to be created
	 * @return (OK, the user address of the user: name@domain)
	 * FORBIDDEN if the domain in the user does not match the domain of the server;
	 * CONFLICT if the name is already associated with a different user (i.e., password or display name differ);
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters, or a malformed user).
	 */
	Result<String> postUser(User user);
	
	/**
	 * Obtains the information on the user identified by name
	 * 
	 * @param name - the name of the user
	 * @param pwd - password of the user
	 * 
	 * @return (OK, the user object, if the name exists and pwd matches the existing password)
	 * FORBIDDEN if the password is incorrect or the user does not exist; 
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters).
	 */
	Result<User> getUser(String name, String pwd);
	
	/**
	 * Modifies the information of a user. Values of null in any field of the user will be 
	 * considered as if the the fields is not to be modified (the name cannot be modified).
	 * 
	 * @param name - the name of the user
	 * @param pwd - password of the user
	 * @param info - Updated information
	 * @return (OK, the updated user object, if the name exists and pwd matches the existing password);
	 * FORBIDDEN if the password is incorrect or the user does not exist; 
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters).
	 */
	Result<User> updateUser(String name, String pwd, User info);
	
	/**
	 * Deletes the user identified by name. 
	 * All resources associated with this user in other services should also be eventually released, without waiting for completion.
	 * 
	 * @param name - the name of the user
	 * @param pwd - password of the user
	 * @return (OK, the deleted user object, if the name exists and pwd matches the existing password) 
	 * FORBIDDEN if the password is incorrect or the user does not exist 
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters).
	 */
	Result<User> deleteUser(String name, String pwd);
	
	/**
	 * Returns the list of users for which the pattern is a substring of the name, case-insensitive. 
	 * The password of the users returned by the query must be set to the empty string "".
	 * Only existing users can perform a search.
	 * 
	 * @param name - the name of the user performing the search
	 * @param pwd - password of the user
	 * @param query - substring to search
	 * @return (OK, list of hits, may be empty.) . 
	 * FORBIDDEN if the password is incorrect or the user does not exist 
	 * BAD_REQUEST if the parameters are invalid (eg., null parameters).
	 */
	Result<List<User>> searchUsers(String name, String pwd, String query);	
	
}
