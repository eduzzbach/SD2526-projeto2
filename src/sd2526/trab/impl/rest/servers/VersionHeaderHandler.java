package sd2526.trab.impl.rest.servers;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import sd2526.trab.impl.utils.SyncPoint;

@Provider
public class VersionHeaderHandler implements ContainerRequestFilter, ContainerResponseFilter {

	public static final String HEADER_VERSION = "X-MESSAGES-version";

	public static final ThreadLocal<Long> version = new ThreadLocal<>();

	public static Long clientVersion() {
		return version.get();
	}

	@Override
	public void filter(ContainerRequestContext reqCtx) throws IOException {
		String value = reqCtx.getHeaderString(HEADER_VERSION);
		if (value != null && !value.isEmpty())
			version.set(Long.valueOf(value));
		else
			version.remove();
	}

	@Override
	public void filter(ContainerRequestContext reqCtx, ContainerResponseContext resCtx) throws IOException {
		var v = SyncPoint.getSyncPoint().getVersion();
		resCtx.getHeaders().add(HEADER_VERSION, Long.toString(v));
		version.remove();
	}
}
