package de.htw.ds.edge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import de.sb.java.TypeMetadata;
import de.sb.java.net.HttpModuleHandler;


/**
 * HTTP server implementation capable of both serving embedded content for requests under
 * context-path "/resource/", and generating a redirect response for requests under context path
 * "/redirect/". Redirect responses point towards an edge server, selected using time zone
 * information.
 */
@TypeMetadata(copyright = "2014-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class HttpMasterServer {

	/**
	 * Application entry point. The given argument is expected to be a service port.
	 * @param args the runtime arguments
	 * @throws NumberFormatException if the given port is not a number
	 * @throws IOException if there is an I/O related problem
	 */
	static public void main (final String[] args) throws IOException {
		final InetSocketAddress		serviceAddress = new InetSocketAddress(Integer.parseInt(args[0]));
		final HttpModuleHandler		resourceHandler = new HttpModuleHandler("/resource");
		final HttpRedirectHandler	redirectHandler = new HttpRedirectHandler("/redirect");

		final HttpServer server = HttpServer.create(serviceAddress, 0);
		server.createContext(resourceHandler.getContextPath(), resourceHandler);
		server.createContext(redirectHandler.getContextPath(), redirectHandler);
		server.start();
		try {
			System.out.format("HTTP server running on service address %s:%s, enter \"quit\" to stop.\n", serviceAddress.getHostName(), serviceAddress.getPort());
			System.out.format("Service path \"%s\" is configured for resource access.\n", resourceHandler.getContextPath());
			System.out.format("Service path \"%s\" is configured for resource redirection.\n", redirectHandler.getContextPath());
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		} finally {
			server.stop(0);
		}
	}
}