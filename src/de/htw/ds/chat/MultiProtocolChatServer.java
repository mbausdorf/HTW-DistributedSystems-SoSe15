package de.htw.ds.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.ws.WebServiceException;
import de.sb.java.TypeMetadata;
import de.sb.java.net.SocketAddress;


/**
 * Chat server wrapping a POJO chat service with everything needed for CCP, Java-RMI and JAX-WS
 * based communication. Note that every method invocation causes an individual thread to be spawned.
 * Note that this class is declared final because it provides an application entry point, and is
 * therefore not supposed to be extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class MultiProtocolChatServer {
	static private final String SERVICE_MESSAGE = "%s chat service URI is %s.\n";


	/**
	 * Application entry point. The given runtime parameters must be the service port for JAX-WS,
	 * the service port for RMI, and the service port for CCP.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given service name cannot be used to construct a valid
	 *         service URI
	 * @throws IOException if the RMI or CCP service port is already in use, or if there is a
	 *         problem waiting for the quit signal
	 * @throws WebServiceException if the given SOAP port is already in use
	 */
	static public void main (final String[] args) throws URISyntaxException, IOException, WebServiceException {
		final long timestamp = System.currentTimeMillis();
		final int servicePortRPC = Integer.parseInt(args[0]);
		final int servicePortRMI = Integer.parseInt(args[1]);
		final int servicePortCCP = Integer.parseInt(args[2]);
		final String serviceName = args[3];

		final URI ccpServiceURI = new URI("ccp", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePortCCP, "/", null, null);

		final ChatService chatService = new ChatService(50);
		try (SoapChatServer rpcChatServer = new SoapChatServer(servicePortRPC, serviceName, chatService)) {
			try (RmiChatServer rmiChatServer = new RmiChatServer(servicePortRMI, serviceName, chatService)) {
				try (CcpChatServer ccpChatServer = new CcpChatServer(ccpServiceURI.getPort(), chatService)) {
					System.out.format("Multi-protocol chat server running, enter \"quit\" to stop.\n");
					System.out.format(SERVICE_MESSAGE, "JAX-WS", rpcChatServer.getServiceURI());
					System.out.format(SERVICE_MESSAGE, "Java-RMI", rmiChatServer.getServiceURI());
					System.out.format(SERVICE_MESSAGE, "CCP", ccpChatServer.getServiceURI());
					System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

					final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
					while (!"quit".equals(charSource.readLine()));
				}
			}
		}
	}
}