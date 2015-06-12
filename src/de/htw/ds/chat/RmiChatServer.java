package de.htw.ds.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import de.sb.java.TypeMetadata;
import de.sb.java.net.SocketAddress;


/**
 * Chat server wrapping a POJO chat service with everything needed for RMI communication,
 * implemented using the standard Java-RMI API. Note that every RMI method invocation causes an
 * individual thread to be spawned. Note that this class is declared final because it provides an
 * application entry point, and therefore not supposed to be extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class RmiChatServer implements RmiChatService, AutoCloseable {

	/**
	 * Application entry point. The given runtime parameters must be a service port, and a service
	 * name.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside it's allowed range, if
	 *         the service name is illegal, or if the given maximum number of chat entries is
	 *         negative
	 * @throws IOException if the service port is already in use, if the server class does not
	 *         implement a valid remote interfaces, or if there is a problem waiting for the quit
	 *         signal
	 */
	static public void main (final String[] args) throws IOException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final String serviceName = args[1];

		try (RmiChatServer server = new RmiChatServer(servicePort, serviceName, 50)) {
			System.out.format("Java-RMI chat server running, enter \"quit\" to stop.\n");
			System.out.format("Service URI is \"%s\".\n", server.getServiceURI());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		}
	}
	private final URI serviceURI;


	private final ChatService delegate;


	/**
	 * Creates a new instance and exports it to an RMI registry.
	 * @param delegate the protocol independent chat service delegate
	 * @throws NullPointerException if the given delegate is {@code null}
	 * @throws IllegalArgumentException if the given service port is outside it's allowed range, or
	 *         if the service name is illegal
	 * @throws RemoteException if export fails
	 */
	public RmiChatServer (final int servicePort, final String serviceName, final ChatService delegate) throws RemoteException {
		if (delegate == null) throw new NullPointerException();
		if (servicePort <= 0 | servicePort > 0xFFFF) throw new IllegalArgumentException();

		try {
			this.serviceURI = new URI("rmi", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException();
		}

		this.delegate = delegate;

		// opens service socket, starts RMI acceptor-thread,
		// creates and associates anonymous proxy class+instance
		UnicastRemoteObject.exportObject(this, servicePort);

		// allows remote distribution of proxy clones by naming lookup
		final Registry registry = LocateRegistry.createRegistry(servicePort);
		registry.rebind(this.serviceURI.getPath().substring(1), this);
	}


	/**
	 * Creates a new instance and exports it to a JAX-WS endpoint.
	 * @param servicePort the service port
	 * @param serviceName the service name
	 * @param maxEntries the maximum number of chat entries
	 * @throws IllegalArgumentException if the given service port is outside it's allowed range, if
	 *         the service name is illegal, or if the given maximum number of chat entries is
	 *         negative
	 * @throws RemoteException if export fails
	 */
	public RmiChatServer (final int servicePort, final String serviceName, final int maxEntries) throws RemoteException {
		this(servicePort, serviceName, new ChatService(maxEntries));
	}


	/**
	 * {@inheritDoc} Delegates the addEntry method to the service implementation.
	 * @throws NullPointerException {@inheritDoc}
	 */
	public void addEntry (final ChatEntry entry) {
		this.delegate.addEntry(entry);
	}


	/**
	 * Closes this server.
	 */
	public void close () {
		try {
			// prevents remote distribution of more proxy clones by naming lookup
			final Registry registry = LocateRegistry.getRegistry(this.serviceURI.getPort());
			registry.unbind(this.serviceURI.getPath().substring(1));
		} catch (final RemoteException | NotBoundException exception) {
			// do nothing
		} finally {
			// removes association with proxy instance,
			// stops RMI acceptor-thread, closes service socket
			try {
				UnicastRemoteObject.unexportObject(this, true);
			} catch (final NoSuchObjectException exception) {}
		}
	}


	/**
	 * {@inheritDoc} Delegates the getEntries method to the service implementation.
	 */
	public ChatEntry[] getEntries () {
		return this.delegate.getEntries();
	}


	/**
	 * Returns the service URI.
	 * @return the service URI
	 */
	public URI getServiceURI () {
		return this.serviceURI;
	}


	/**
	 * {@inheritDoc} Delegates the removeEntry method to the service implementation.
	 * @throws IllegalArgumentException if the given index is out of bounds
	 */
	public void removeEntry (final int index) {
		this.delegate.removeEntry(index);
	}
}