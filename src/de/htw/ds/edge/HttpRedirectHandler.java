package de.htw.ds.edge;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Instances of this HTTP handler class redirect any request to an appropriate
 * edge server, based on timezone information provided as a query parameter.
 */
public class HttpRedirectHandler implements HttpHandler {
	static private final short HTTP_REDIRECT = 307;
	static private final short HTTP_BAD_REQUEST = 400;
	static private final short HTTP_METHOD_NOT_ALLOWED = 405;

	private final String contextPath;


	public HttpRedirectHandler(String contextPath) {
		if (contextPath == null) throw new NullPointerException();

		if (!contextPath.startsWith("/")) contextPath = "/" + contextPath;
		if (!contextPath.endsWith("/")) contextPath = contextPath + "/";
		this.contextPath = contextPath;
	}


	/**
	 * Returns the (normalized) context path.
	 * @return the context path
	 */
	public String getContextPath () {
		return this.contextPath.length() == 1
			? this.contextPath
			: this.contextPath.substring(0, this.contextPath.length() - 1);
	}


	/**
	 * Handles the given HTTP exchange by redirecting the request.
	 * @param exchange the HTTP exchange
	 * @throws NullPointerException if the given exchange is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	public void handle (final HttpExchange exchange) throws IOException {
		try {
			if (!"GET".equals(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, -1);
				return;
			}

			final URI requestURI = exchange.getRequestURI();
			final String requestPath = requestURI.getPath();
			final String requestQuery = requestURI.getQuery();
			final Map<String,String> requestParameters = HttpRedirectHandlers.parseQueryParameters(requestQuery);
			if (!requestPath.startsWith(this.contextPath)) {
				exchange.sendResponseHeaders(HTTP_BAD_REQUEST, -1);
				return;
			}

			if(requestParameters.containsKey("timezoneOffset")){
				// get timezone offset
				float timezoneOffset = Float.parseFloat(requestParameters.get("timezoneOffset"));
				// construct redirect URL
				InetSocketAddress redirectAddress = HttpRedirectHandlers.nodeAddress(timezoneOffset);
				String redirectUrl = "http://" + redirectAddress.getHostName() + ":" + redirectAddress.getPort() + requestPath.replace("redirect/","");
				// send response header
				exchange.getResponseHeaders().add("Location",redirectUrl);
				exchange.sendResponseHeaders(HTTP_REDIRECT, 0);
			}
			else{
				//time offset not available
				exchange.sendResponseHeaders(HTTP_BAD_REQUEST,-1);
			}

		} finally {
			exchange.close();
		}
	}
}