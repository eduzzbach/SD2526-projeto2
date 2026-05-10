package sd2526.trab.impl.grpc.clients;

import static sd2526.trab.impl.grpc.common.DataModelAdaptor.GrpcUser_to_User;
import static sd2526.trab.impl.grpc.common.DataModelAdaptor.User_to_GrpcUser;

import java.util.List;

import com.google.common.collect.Lists;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.impl.grpc.common.DataModelAdaptor;
import sd2526.trab.impl.grpc.generated_java.GrpcUsersGrpc;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.DeleteUserArgs;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.GetUserArgs;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.SearchUsersArgs;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.UpdateUserArgs;

public class GrpcUsersClient extends GrpcClient implements Users {

	final GrpcUsersGrpc.GrpcUsersBlockingStub stub;

	public GrpcUsersClient(String serverURI) {
		super(serverURI);
		this.stub = GrpcUsersGrpc.newBlockingStub( super.channel );	
	}

	@Override
	public Result<String> postUser(User user) {
		return super.toJavaResult( () -> stub.postUser( User_to_GrpcUser(user) ).getUserAddress() );
	}

	@Override
	public Result<User> getUser(String name, String pwd) {
		return super.toJavaResult( () -> GrpcUser_to_User(stub.getUser( GetUserArgs.newBuilder().setName(name).setPwd(pwd).build() ).getUser()));
	}

	@Override
	public Result<User> updateUser(String name, String pwd, User info) {
		return super.toJavaResult( () -> GrpcUser_to_User( stub.updateUser( 
				UpdateUserArgs.newBuilder()
				.setName(name)
				.setPwd( pwd )
				.setInfo( User_to_GrpcUser(info) ).build()).getUser() ));
	}

	@Override
	public Result<User> deleteUser(String name, String pwd) {
		return super.toJavaResult( () -> GrpcUser_to_User( stub.deleteUser( 
				DeleteUserArgs.newBuilder()
				.setName(name)
				.setPwd( pwd )
				.build()).getUser()));
	}

	@Override
	public Result<List<User>> searchUsers(String name, String pwd, String query) {
		return super.toJavaResult(() -> Lists.newArrayList(stub.searchUsers( SearchUsersArgs.newBuilder()
				.setName( name)
				.setPwd(pwd)
				.setQuery( query ).build())).stream().map( DataModelAdaptor::GrpcUser_to_User ).toList());
	}
}
