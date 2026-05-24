package sd2526.trab.impl.rest.servers;

import java.io.IOException;
import java.net.URI;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import sd2526.trab.impl.utils.IP;

@Provider
public class PrimaryRedirectFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext ctx) throws IOException {
		if (RestRepMessagesServer.isPrimary())
			return;

		var method = ctx.getMethod();
		if (!HttpMethod.POST.equals(method))
			return;

		var path = ctx.getUriInfo().getPath();
		if (!path.startsWith("messages"))
			return;

		var domain = IP.domain();
		var target = new StringBuilder("https://messages0.%s:%d/rest/%s"
				.formatted(domain, RestRepMessagesServer.PORT, path));

		var query = ctx.getUriInfo().getRequestUri().getRawQuery();
		if (query != null && !query.isEmpty())
			target.append('?').append(query);

		throw new WebApplicationException(
				Response.temporaryRedirect(URI.create(target.toString())).build());
	}
}
