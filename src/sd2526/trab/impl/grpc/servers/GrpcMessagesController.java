package sd2526.trab.impl.grpc.servers;

import static sd2526.trab.impl.grpc.common.DataModelAdaptor.GrpcMessage_to_Message;
import static sd2526.trab.impl.grpc.common.DataModelAdaptor.Message_to_GrpcMessage;

import com.google.protobuf.Empty;

import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.grpc.generated_java.GrpcMessagesGrpc;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.DeleteMessageArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.GetAllInboxMessagesArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.GetAllInboxMessagesResult;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.GetInboxMessageArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.GrpcMessage;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.PostMessageArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.PostMessageResult;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.RemoveInboxMessageArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.SearchInboxArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.SearchInboxResult;
import sd2526.trab.impl.java.servers.JavaMessages;

public class GrpcMessagesController extends GrpcController implements GrpcMessagesGrpc.AsyncService {

	Messages impl = JavaMessages.getInstance();
	
	@Override
	public ServerServiceDefinition bindService() {
		return GrpcMessagesGrpc.bindService(this);
	}
	
	public void postMessage(PostMessageArgs request, StreamObserver<PostMessageResult> responseObserver) {
		super.toGrpcResult(responseObserver,
				impl.postMessage(request.getPwd(), GrpcMessage_to_Message(request.getMessage())),
				(mid) -> PostMessageResult.newBuilder().setMid(mid).build());
	}

	@Override
	public void getInboxMessage(GetInboxMessageArgs request, StreamObserver<GrpcMessage> responseObserver) {
		super.toGrpcResult(responseObserver,
				impl.getInboxMessage(request.getUser(), request.getMid(), request.getPwd()),
				(msg) -> Message_to_GrpcMessage( msg ));
	}

	@Override
	public void getAllInboxMessages(GetAllInboxMessagesArgs request, StreamObserver<GetAllInboxMessagesResult> responseObserver) {
		super.toGrpcResult(responseObserver,
				impl.getAllInboxMessages(request.getUser(), request.getPwd()),
				(mids) -> GetAllInboxMessagesResult.newBuilder().addAllMids(mids).build());
	}

	@Override
	public void removeInboxMessage(RemoveInboxMessageArgs request, StreamObserver<Empty> responseObserver) {
		super.toGrpcResult(responseObserver,
				impl.removeInboxMessage(request.getUser(), request.getMid(), request.getPwd()),
				(__) -> Empty.newBuilder().build());
	}

	@Override
	public void deleteMessage(DeleteMessageArgs request, StreamObserver<Empty> responseObserver) {
		super.toGrpcResult(responseObserver,
				impl.deleteMessage(request.getUser(), request.getMid(), request.getPwd()),
				(__) -> Empty.newBuilder().build());
	}

	@Override
	public void searchInbox(SearchInboxArgs request, StreamObserver<SearchInboxResult> responseObserver) {
		super.toGrpcResult(responseObserver,
				impl.searchInbox(request.getUser(), request.getPwd(), request.getQuery()),
				(hits) -> SearchInboxResult.newBuilder().addAllMids(hits).build());
	}
}
