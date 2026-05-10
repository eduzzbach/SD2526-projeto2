package sd2526.trab.impl.grpc.clients;

import static sd2526.trab.impl.grpc.common.DataModelAdaptor.GrpcMessage_to_Message;
import static sd2526.trab.impl.grpc.common.DataModelAdaptor.Message_to_GrpcMessage;

import java.util.List;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.impl.grpc.generated_java.GrpcMessagesGrpc;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.DeleteMessageArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.GetAllInboxMessagesArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.GetInboxMessageArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.PostMessageArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.RemoveInboxMessageArgs;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.SearchInboxArgs;

public class GrpcMessagesClient extends GrpcClient implements Messages {

	final GrpcMessagesGrpc.GrpcMessagesBlockingStub stub;

	
	public GrpcMessagesClient(String serverUrl) {
		super(serverUrl);
		this.stub = GrpcMessagesGrpc.newBlockingStub( super.channel );	
	}

	@Override
	public Result<String> postMessage(String pwd, Message msg) {
		var args = PostMessageArgs.newBuilder()
				.setPwd(pwd)
				.setMessage( Message_to_GrpcMessage(msg) )
				.build();
		
		return super.toJavaResult( () -> stub.postMessage( args ).getMid() );

	}

	@Override
	public Result<Message> getInboxMessage(String name, String mid, String pwd) {
		var args = GetInboxMessageArgs.newBuilder()
				.setPwd(pwd)
				.setUser( name )
				.setMid( mid )
				.build();
				
		return super.toJavaResult( () -> GrpcMessage_to_Message(stub.getInboxMessage( args ) ) );
	}

	@Override
	public Result<List<String>> getAllInboxMessages(String name, String pwd) {
		var args = GetAllInboxMessagesArgs.newBuilder()
				.setPwd(pwd)
				.setUser( name )
				.build();
				
		return super.toJavaResult( () -> stub.getAllInboxMessages(args).getMidsList());
	}

	@Override
	public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
		var args = RemoveInboxMessageArgs.newBuilder()
				.setPwd(pwd)
				.setUser( name )
				.setMid( mid )
				.build();
				
		return super.toJavaResult( () -> {
			stub.removeInboxMessage( args )	;
		});
	}

	@Override
	public Result<Void> deleteMessage(String name, String mid, String pwd) {
		var args = DeleteMessageArgs.newBuilder()
				.setPwd(pwd)
				.setUser( name )
				.setMid( mid )
				.build();
				
		return super.toJavaResult( () -> {
			stub.deleteMessage(args);
		} );
	}

	@Override
	public Result<List<String>> searchInbox(String name, String pwd, String query) {
		var args = SearchInboxArgs.newBuilder()
				.setPwd(pwd)
				.setUser( name )
				.setQuery( query )
				.build();
				
		return super.toJavaResult( () -> stub.searchInbox( args ).getMidsList() );
	}
}
