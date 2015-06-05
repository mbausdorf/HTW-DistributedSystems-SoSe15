package de.htw.ds.edge;

import java.io.IOException;
import de.sb.java.TypeMetadata;
import de.sb.java.net.HttpModuleHandler;


/**
 * HTTP server implementation serving embedded content.
 */
@TypeMetadata(copyright = "2014-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class HttpEdgeServer {

	/**
	 * Application entry point. The given argument is expected to be a service port.
	 * @param args the runtime arguments
	 * @throws NumberFormatException if the given port is not a number
	 * @throws IOException if there is an I/O related problem
	 */
	static public void main (final String[] args) throws IOException {
		final int servicePort = Integer.parseInt(args[0]);
		HttpModuleHandler.service(servicePort, "/");
	}
}