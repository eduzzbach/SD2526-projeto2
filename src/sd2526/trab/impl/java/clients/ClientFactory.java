package sd2526.trab.impl.java.clients;

import java.net.URI;
import java.util.function.Function;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import sd2526.trab.api.java.Result;
import sd2526.trab.impl.discovery.Discovery;
import sd2526.trab.impl.utils.IP;


public class ClientFactory<T> {

	private static final String REST = "/rest";
	private static final String GRPC = "/grpc";

	private final String serviceName;
	private final Function<String, T> restClientFunc;
	private final Function<String, T> grpcClientFunc;
	
	private LoadingCache<URI, T> clients = CacheBuilder.newBuilder()
			.build(new CacheLoader<>() {
				@Override
				public T load(URI uri) throws Exception {
					return newClient( uri.toString() );
				}
			});
	
	ClientFactory( String serviceName, Function<String, T> restClientFunc, Function<String, T> grpcClientFunc) {
		this.restClientFunc = restClientFunc;
		this.grpcClientFunc = grpcClientFunc;
		this.serviceName = serviceName;
	}
	
	public T get() {
		return get( IP.domain() );
	}
	
	public T get(String domain) {
		var sn = "%s@%s".formatted(serviceName, domain);
		return get(Discovery.getInstance().knownUrisOf(sn, 1)[0]);
	}
	
	private T newClient( String serverURI ) {
		if (serverURI.endsWith(REST))
			return restClientFunc.apply( serverURI );
		else if (serverURI.endsWith(GRPC))
			return grpcClientFunc.apply( serverURI );
		else
			throw new RuntimeException("Unknown service type..." + serverURI);	
	}
	
	public T async() {
		return get(Discovery.getInstance().knownUrisOf(serviceName, 1)[0]);
	}
	
	public T get(URI uri) {
		try {
			return clients.get(uri);
		} catch (Exception x) {
			x.printStackTrace();
			throw new RuntimeException( Result.ErrorCode.INTERNAL_ERROR.toString());
		}
	}	
			
}
