package sd2526.trab.impl.java.servers;

import static sd2526.trab.api.java.Result.error;
import static sd2526.trab.api.java.Result.ok;
import static sd2526.trab.api.java.Result.ErrorCode.BAD_REQUEST;
import static sd2526.trab.api.java.Result.ErrorCode.FORBIDDEN;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.db.DB;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.utils.IP;

public class JavaMessages extends JavaBaseService implements Messages, AdminMessages {
	
	protected static final int REMOTE_COMM_DEADLINE = 90000;
	private static final long MESSAGES_CACHE_EXPIRATION = 30000;
	private static final long DIRTY_INBOX_CACHE_EXPIRATION = 10000;

	protected final JobDispatcher jobs;
	protected final AtomicLong counter = new AtomicLong(0L);	
	
	protected final Cache<String, Message> messagesCache = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofMillis(MESSAGES_CACHE_EXPIRATION))
			.build();
	
	protected final Cache<String, String> gcDeletedMessageCache = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofMillis(DIRTY_INBOX_CACHE_EXPIRATION))
			.removalListener( (removed) -> {
				
				// When triggered, removes any orphaned messages in the database, 
				// i.e. messages that are no longer referenced by any inbox...
				
				var sqlExpr = """
						SELECT * FROM Message m
							WHERE NOT EXISTS 
								(SELECT 1 FROM InboxEntry e WHERE e.mid = m.id)
						""";
						
				DB.transaction( (hibernate) -> 
					hibernate.select( sqlExpr, Message.class )
						.thenWith( (orphans) -> hibernate.deleteMany( orphans ))	
				);
			})
			.build();
	
	protected JavaMessages() {
		this.jobs = new JobDispatcher();
	}

	@Override
	public Result<String> postMessage(String pwd, Message msg) {
		return getUser(msg.getSender(), pwd)					
				.thenWith( (user) -> doAsyncPost( user, msg ));			
	}

	@Override
	public Result<Message> getInboxMessage(String name, String mid, String pwd) {
		if( badParams( name, mid, pwd ) )
			return error(BAD_REQUEST);
		
		return getUser(name, pwd)
				.then( () -> DB.getOne( new InboxEntry(mid, name), InboxEntry.class))
				.then(() -> DB.getOne( mid, Message.class));			
	}

	@Override
	public Result<List<String>> getAllInboxMessages(String name, String pwd) {
		var sqlExpr = """
				SELECT m.id FROM Message m
				INNER JOIN InboxEntry e
				ON e.mid = m.id
				AND e.recipient = '%s'
				ORDER BY m.creationTime DESC, m.id DESC
				""".formatted(sqlLiteral(name));
		return getUser(name, pwd )
					.then( () -> DB.select( sqlExpr, String.class));		
	}
	
	private static String sqlLiteral(String value) {
		return value.replace("'", "''");
	}

	@Override
	public Result<List<String>> searchInbox(String name, String pwd, String query) {
		var safeQuery = sqlLiteral(query.toUpperCase());
		var sqlExpr = """
				SELECT m.id FROM Message m
				INNER JOIN InboxEntry e
				ON e.mid = m.id 
				AND e.recipient = '%s'
				WHERE (upper(m.subject) LIKE '%%%s%%' OR upper(m.contents) LIKE '%%%s%%')
				ORDER BY m.creationTime DESC, m.id DESC
				""".formatted(sqlLiteral(name), safeQuery, safeQuery);

		return getUser(name, pwd )
				.then( () -> DB.select( sqlExpr, String.class));		
	}
	
	@Override
	public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
		return getUser(name, pwd )
				.then( () -> DB.deleteOne( new InboxEntry(mid, name) ) ).mapToVoid()
				.then( () -> {
					gcDeletedMessageCache.put( mid, mid );
				});
	}

	@Override
	public Result<Void> deleteMessage(String name, String mid, String pwd) {
		return getUser(name, pwd )
			.then( () -> getCachedMessage(mid))
			.thenWith(msg -> name.equals( getName(msg.senderAddress())) ? ok(msg) : error(FORBIDDEN) )
			.thenWith((msg) -> doAsyncDelete(msg));
	}
	
	
	protected Result<User> getUser( String user, String pwd) {
		try {
			var name = user.split("@", 2)[0];
			return Clients.UsersClient.get().getUser( name, pwd);
		} catch (Exception x) {
			x.printStackTrace();
			return Result.error(INTERNAL_ERROR);
		}
	}

	protected Result<Set<String>> checkUsers( Collection<String> addresses ) {
		return Clients.AdminUsersClient.get().checkUsers(addresses);
	}

	protected void deliverToKnownLocalRecipients(Collection<String> addresses, Message msg) {
		DB.transaction((hibernate) -> {
			hibernate.persistOne( msg );
			for( var address : addresses )
				hibernate.persistOne( new InboxEntry( msg.getId(), getName(address) ));
			
			return ok();
		});
		
	}
		
	protected void reportUnknownLocalRecipients(Collection<String> addresses, Message msg) {
		var senderDomain = super.getDomain( msg.senderAddress() );
		
		try {
			for( var recipientAddress : addresses ) {
				var errorMsg = msg.cloneWithUserNotFound( recipientAddress );
				if( super.isLocalDomain( senderDomain ) ) {
					DB.transaction((hibernate)-> {
						hibernate.persistOne( new InboxEntry( errorMsg.getId(), msg.senderName() )) ;						
						hibernate.persistOne(errorMsg);							
						return ok();
					});
				}
				else doAsyncRemotePost(senderDomain, errorMsg);
			}
		} catch( Exception x ) {
			x.printStackTrace();			
		}
	}	
		
	protected Result<Void> postToLocalInboxes( Collection<String> addresses, Message msg) {
		return checkUsers(addresses)
				.thenWith( unknownAddresses -> {

					var knownAddresses = new HashSet<>( addresses );
					knownAddresses.removeAll( unknownAddresses );
	
					if( knownAddresses.size() > 0 ) 
						deliverToKnownLocalRecipients(knownAddresses, msg);
					
					if( unknownAddresses.size() > 0 )
						reportUnknownLocalRecipients( unknownAddresses, msg );
					
					return ok();
				});
	}
	
	@Override
	public Result<Void> remotePostMessage(Message msg) {
		var localAddresses = getLocalRecipientAddresses(msg);		
		return postToLocalInboxes(localAddresses, msg);
	}

	protected Result<Void> deleteFromLocalInbox(String mid) {
		var sql = "SELECT * FROM InboxEntry e WHERE e.mid = '%s'".formatted(mid); 
		
		return DB.transaction( hibernate -> {
			
			hibernate.getOne(mid, Message.class)
				.thenWith(msg -> hibernate.deleteOne(msg));
			
			return hibernate.select(sql, InboxEntry.class)			
				.thenWith( (entries) -> hibernate.deleteMany( entries ) );				
		});		
	}
	
	@Override
	public Result<Void> remoteDeleteMessage(String mid) {
		return deleteFromLocalInbox(mid);
	}
	
	protected Result<Message> getCachedMessage( String mid ) {
		var msg = messagesCache.getIfPresent( mid );
		return msg != null ? ok( msg ) : error( FORBIDDEN );
	}
	
	public final class JobDispatcher {

	    private final ConcurrentHashMap<String, ExecutorService> executors = new ConcurrentHashMap<>();

	    public void submit(String domain, Runnable job) {
	        ExecutorService executor = executors.computeIfAbsent(
	            domain,
	            d -> Executors.newSingleThreadExecutor(r -> {
	                Thread t = new Thread(r);
	                t.setUncaughtExceptionHandler((thr, ex) -> {
	                	ex.printStackTrace();
	                });
	                return t;
	            })
	        );
	        executor.submit( job );
	    }
	}
	
	public Result<String> doAsyncPost(User sender, Message msg) {

		return getCachedMessage(msg.originId()).mapValue(Message::getId).orElse(() -> {
			
			
			msg.setId("%s+%04d".formatted(THIS_DOMAIN, counter.incrementAndGet()));
			
			messagesCache.put(msg.originId(), new Message( msg )); // For ensuring idempotency...
			
			msg.setSender("%s <%s@%s>".formatted(sender.getDisplayName(), sender.getName(), sender.getDomain()));

			messagesCache.put(msg.getId(), msg); // For enabling delete of messages...

			var localAdresses = getLocalRecipientAddresses(msg);
			var remoteAddresses = getRemoteRecipientAddresses(msg);

			if (localAdresses.size() > 0)
				postToLocalInboxes(localAdresses, msg);

			if (remoteAddresses.size() > 0) {

				var remoteTargets = remoteAddresses.stream().collect(
						Collectors.groupingBy( super::getDomain, Collectors.mapping( address -> address, Collectors.toSet())));

				for (var e : remoteTargets.entrySet()) {
					var domain = e.getKey();
					var domainRecipientAddressess = e.getValue();
					
					jobs.submit(domain, () -> {
						var res = super.reTry(() -> Clients.AdminMessagesClient.get(domain).remotePostMessage(msg), REMOTE_COMM_DEADLINE);
						if (res.error() == ErrorCode.TIMEOUT) {
							for (var address : domainRecipientAddressess)
								postToLocalInboxes(Set.of(msg.senderAddress()), msg.cloneWithTimeout(address));
						}
					});
					
				}
			}
			return Result.ok(msg.getId());
		});
	}
		
		public Result<Void> doAsyncDelete( Message msg ) {
			var domains = msg.getDestination().stream().map( r -> r.split("@")[1]).collect( Collectors.toSet() );
			for( var domain : domains )
				if( domain.equals( IP.domain() ))
					deleteFromLocalInbox( msg.getId() );
				else
					jobs.submit(domain, () -> {
						super.reTry(()-> Clients.AdminMessagesClient.get(domain).remoteDeleteMessage(msg.getId()), REMOTE_COMM_DEADLINE);			
					});				
			return Result.ok();
		}
		
		public void doAsyncRemotePost( String remoteDomain, Message msg ) {
			jobs.submit(remoteDomain, () -> {
				super.reTry(() -> Clients.AdminMessagesClient.get(remoteDomain).remotePostMessage(msg), REMOTE_COMM_DEADLINE);			
			});				
		}
		
		@Override
		public Result<Void> remoteDeleteUserInbox(String name) {
			var sqlExpr = "SELECT * FROM InboxEntry e WHERE e.recipient = '%s'".formatted(name); 
			
			return DB.transaction( hibernate -> {
				
				return hibernate.select(sqlExpr, InboxEntry.class)			
					.thenWith( (entries) -> {
						hibernate.deleteMany( entries );
						for( var e: entries )
							gcDeletedMessageCache.put( e.mid, e.mid);
						
						return ok();
					} );
			});		
			
		}	
		
		
		protected List<String> getLocalRecipientAddresses(  Message msg ) {
			return msg.getDestination().stream().filter( super::isLocalAddress ).toList();			
		} 

		protected Set<String> getRemoteRecipientAddresses(  Message msg ) {
			return msg.getDestination().stream().filter( Predicate.not(super::isLocalAddress)).collect( Collectors.toSet() );			
		} 
		
		
		static JavaMessages instance;
		
		public static synchronized JavaMessages getInstance() {
			if( instance == null )
				instance = new JavaMessages();
			return instance;
		}
	}

