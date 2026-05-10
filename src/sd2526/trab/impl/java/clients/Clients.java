package sd2526.trab.impl.java.clients;

import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Users;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.api.java.AdminUsers;
import sd2526.trab.impl.grpc.clients.GrpcAdminMessagesClient;
import sd2526.trab.impl.grpc.clients.GrpcAdminUsersClient;
import sd2526.trab.impl.grpc.clients.GrpcMessagesClient;
import sd2526.trab.impl.grpc.clients.GrpcUsersClient;
import sd2526.trab.impl.rest.clients.RestAdminMessagesClient;
import sd2526.trab.impl.rest.clients.RestAdminUsersClient;
import sd2526.trab.impl.rest.clients.RestMessagesClient;
import sd2526.trab.impl.rest.clients.RestUsersClient;

public class Clients {
	public static final ClientFactory<Users> UsersClient = new ClientFactory<>(Users.SERVICE_NAME, RestUsersClient::new, GrpcUsersClient::new);

	public static final ClientFactory<Messages> MessagesClient = new ClientFactory<>(Messages.SERVICE_NAME, RestMessagesClient::new, GrpcMessagesClient::new);


	public static final ClientFactory<AdminUsers> AdminUsersClient = new ClientFactory<>(Users.SERVICE_NAME, RestAdminUsersClient::new, GrpcAdminUsersClient::new);

	public static final ClientFactory<AdminMessages> AdminMessagesClient = new ClientFactory<>(Messages.SERVICE_NAME, RestAdminMessagesClient::new, GrpcAdminMessagesClient::new);

}
