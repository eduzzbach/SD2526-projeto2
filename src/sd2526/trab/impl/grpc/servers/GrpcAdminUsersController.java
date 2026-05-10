package sd2526.trab.impl.grpc.servers;

import java.util.HashSet;

import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.impl.api.java.AdminUsers;
import sd2526.trab.impl.grpc.generated_java.AdminUsersProtoBuf.CheckUsersArgs;
import sd2526.trab.impl.grpc.generated_java.AdminUsersProtoBuf.CheckUsersResult;
import sd2526.trab.impl.grpc.generated_java.GrpcAdminUsersGrpc;
import sd2526.trab.impl.java.servers.JavaUsers;

public class GrpcAdminUsersController extends GrpcController implements GrpcAdminUsersGrpc.AsyncService {

	AdminUsers impl = JavaUsers.getInstance();

	@Override
	public final ServerServiceDefinition bindService() {
		return GrpcAdminUsersGrpc.bindService(this);
	}

	@Override
	public void checkUsers(CheckUsersArgs request, StreamObserver<CheckUsersResult> responseObserver) {
		var names = new HashSet<>(request.getNamesList());
		super.toGrpcResult(responseObserver, impl.checkUsers( names ), 
			(res) -> CheckUsersResult.newBuilder().addAllUnknown( res ).build());
	}
}


