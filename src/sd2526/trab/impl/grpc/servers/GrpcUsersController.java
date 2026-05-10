package sd2526.trab.impl.grpc.servers;

import static sd2526.trab.impl.grpc.common.DataModelAdaptor.GrpcUser_to_User;
import static sd2526.trab.impl.grpc.common.DataModelAdaptor.User_to_GrpcUser;

import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.java.Users;
import sd2526.trab.impl.grpc.generated_java.GrpcUsersGrpc;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.DeleteUserArgs;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.DeleteUserResult;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.GetUserArgs;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.GetUserResult;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.GrpcUser;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.PostUserResult;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.SearchUsersArgs;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.UpdateUserArgs;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.UpdateUserResult;
import sd2526.trab.impl.java.servers.JavaUsers;

public class GrpcUsersController extends GrpcController implements GrpcUsersGrpc.AsyncService {

	Users impl = JavaUsers.getInstance();

	@Override
	public final ServerServiceDefinition bindService() {
		return GrpcUsersGrpc.bindService(this);
	}

	public void postUser(GrpcUser user, StreamObserver<PostUserResult> responseObserver) {
		super.toGrpcResult(responseObserver,
			impl.postUser(GrpcUser_to_User( user )),
			(userAddress) -> PostUserResult.newBuilder().setUserAddress(userAddress).build());
	}

	@Override
	public void getUser(GetUserArgs request, StreamObserver<GetUserResult> responseObserver) {
		super.toGrpcResult(responseObserver, 
				impl.getUser(request.getName(), request.getPwd()),
				(user) -> GetUserResult.newBuilder().setUser(User_to_GrpcUser(user)).build());
	}

	@Override
	public void updateUser(UpdateUserArgs request, StreamObserver<UpdateUserResult> responseObserver) {
		super.toGrpcResult(responseObserver,
				impl.updateUser(request.getName(), request.getPwd(), GrpcUser_to_User(request.getInfo())),
				(user) -> UpdateUserResult.newBuilder().setUser( User_to_GrpcUser(user)).build());
	}

	@Override
	public void deleteUser(DeleteUserArgs request, StreamObserver<DeleteUserResult> responseObserver) {
		super.toGrpcResult(responseObserver, 
				impl.deleteUser(request.getName(), request.getPwd()),
				(user) -> DeleteUserResult.newBuilder().setUser(User_to_GrpcUser(user)).build());
	}

	@Override
	public void searchUsers(SearchUsersArgs request, StreamObserver<GrpcUser> responseObserver) {
		super.toGrpcResultCollection(responseObserver,
				impl.searchUsers(request.getName(), request.getPwd(), request.getQuery()),
				(user) -> User_to_GrpcUser(user));
	}
}


