package sd2526.trab.impl.grpc.common;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.impl.grpc.generated_java.AdminMessagesProtoBuf.GrpcAdminMessage;
import sd2526.trab.impl.grpc.generated_java.MessagesProtoBuf.GrpcMessage;
import sd2526.trab.impl.grpc.generated_java.UsersProtoBuf.GrpcUser;

public class DataModelAdaptor {

	public static User GrpcUser_to_User( GrpcUser from )  {
		var b = new User();
		b.setName( from.getName().isEmpty() ? null : from.getName());
		setOptionalField(from::hasPwd, from::getPwd, b::setPwd );
		setOptionalField(from::hasDomain, from::getDomain, b::setDomain );
		setOptionalField(from::hasDisplayName, from::getDisplayName, b::setDisplayName );
		
		return b;
	}

	public static GrpcUser User_to_GrpcUser( User from )  {
		var b = GrpcUser.newBuilder();
		setOptionalFieldIfNotNull( from::getPwd, b::setPwd);
		setOptionalFieldIfNotNull( from::getName, b::setName);
		setOptionalFieldIfNotNull( from::getDomain, b::setDomain);
		setOptionalFieldIfNotNull( from::getDisplayName, b::setDisplayName);
		return b.build();
	}
	
	public static Message GrpcMessage_to_Message( GrpcMessage from )  {
		var b = new Message();
		
		b.setId( from.getId() );
		b.setSender(  from.getSender() );
		b.setSubject( from.getSubject() );
		b.setContents( from.getContents() );
		b.setCreationTime( from.getCreationTime() );
		b.setDestination( Set.copyOf(from.getDestinationList()));
		return b;
	}
	
	public static GrpcMessage Message_to_GrpcMessage( Message from )  {
		var b = GrpcMessage.newBuilder();
		
		setOptionalFieldIfNotNull( from::getId, b::setId);
		setOptionalFieldIfNotNull( from::getSender, b::setSender);
		setOptionalFieldIfNotNull( from::getSubject, b::setSubject);
		setOptionalFieldIfNotNull( from::getContents, b::setContents);
		setOptionalFieldIfNotNull( from::getCreationTime, b::setCreationTime);
		
		b.addAllDestination( from.getDestination() );
				
		return b.build();
	}
	
	public static Message GrpcAdminMessage_to_Message( GrpcAdminMessage from )  {
		var b = new Message();
		
		b.setId( from.getId() );
		b.setSender(  from.getSender() );
		b.setSubject( from.getSubject() );
		b.setContents( from.getContents() );
		b.setCreationTime( from.getCreationTime() );
		b.setDestination( Set.copyOf(from.getDestinationList()));
		return b;
	}
	
	public static GrpcAdminMessage Message_to_GrpcAdminMessage( Message from )  {
		var b = GrpcAdminMessage.newBuilder();
		
		setOptionalFieldIfNotNull( from::getId, b::setId);
		setOptionalFieldIfNotNull( from::getSender, b::setSender);
		setOptionalFieldIfNotNull( from::getSubject, b::setSubject);
		setOptionalFieldIfNotNull( from::getContents, b::setContents);
		setOptionalFieldIfNotNull( from::getCreationTime, b::setCreationTime);
		
		b.addAllDestination( from.getDestination() );
				
		return b.build();
	}
	
	
	private static <T> void setOptionalFieldIfNotNull( Supplier<T> t, Consumer<T> b) {
		var v = t.get();
		if( v != null )
			b.accept(v);
	}
	
	private static <T> void setOptionalField(Supplier<Boolean> cond, Supplier<T> from, Consumer<T> to ) {
		if( cond.get() )
			to.accept( from.get() );
	}
}
