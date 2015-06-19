package de.htw.ds.tcp;

import de.sb.java.TypeMetadata;
import de.sb.java.io.Streams;
import de.sb.java.net.SocketAddress;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * This class models a TCP switch, i.e. a "spray router" for all kinds of TCP oriented protocol
 * connections. It routes incoming client requests to it's given set of protocol servers, either
 * randomly selected, or determined by known session association. Note that while this
 * implementation routes all kinds of TCP protocols, a single instance is only able to route one
 * protocol type unless it's child servers support multi-protocol requests.<br />
 * Session association is determined by receiving subsequent requests from the same client, which
 * may or may not be interpreted as being part of the same session by the protocol server selected.
 * However, two requests cannot be part of the same session if they do not share the same request
 * client address! Note that this algorithm allows for protocol independence, but does not work with
 * clients that dynamically change their IP-address during a session's lifetime. Also note that this
 * class is declared final because it provides an application entry point, and therefore not
 * supposed to be extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class TcpSwitch implements Runnable, AutoCloseable {
	static private final int MAX_PACKET_SIZE = 0xFFFF;

	// TODO: solution is possible without further instance variables. However, here such an
	// alteration is allowed if required, for example to cache clientAddress->serverSocketAddress
	// combinations within a map ...
	private final ServerSocket serviceSocket;
	private final ExecutorService threadPool;
	private final InetSocketAddress[] nodeAddresses;
	private final boolean sessionAware;


	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param nodeAddresses the node addresses
	 * @param sessionAware true if the server is aware of sessions, false otherwise
	 * @throws NullPointerException if the given socket-addresses array is {@code null}
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF], or
	 *         the given socket-addresses array is empty
	 * @throws IOException if the given port is already in use, or cannot be bound
	 */
	public TcpSwitch(final int servicePort, final boolean sessionAware, final InetSocketAddress... nodeAddresses) throws IOException {
		if (nodeAddresses.length == 0) throw new IllegalArgumentException();

		this.serviceSocket = new ServerSocket(servicePort);
		this.threadPool = Executors.newCachedThreadPool();
		this.nodeAddresses = nodeAddresses;
		this.sessionAware = sessionAware;

		// start acceptor thread
		final Thread thread = new Thread(this, "tcp-acceptor");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Closes this server.
	 */
	public void close () {
		try {
			this.serviceSocket.close();
		} catch (final Throwable exception) {}
		this.threadPool.shutdown();
	}


	/**
	 * Returns the node addresses.
	 * @return the node addresses
	 */
	public InetSocketAddress[] getNodeAddresses () {
		return this.nodeAddresses;
	}


	/**
	 * Returns the service port.
	 * @return the service port
	 */
	public int getServicePort () {
		return this.serviceSocket.getLocalPort();
	}



	/**
	 * Returns the session awareness.
	 * @return the session awareness
	 */
	public boolean getSessionAware () {
		return this.sessionAware;
	}


	/**
	 * Periodically blocks until a request arrives, handles the latter subsequently.
	 */
	public void run () {
		while (true) {
			Socket clientConnection = null;
			try {
				clientConnection = this.serviceSocket.accept();

				final ConnectionHandler connectionHandler = new ConnectionHandler(clientConnection, this.threadPool, this.nodeAddresses, this.sessionAware);
				this.threadPool.execute(connectionHandler);
			} catch (final SocketException exception) {
				break;
			} catch (final Exception exception) {
				try { clientConnection.close(); } catch (final Exception nestedException) {}
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			}
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a service port, the session
	 * awareness, and the list of address:port combinations for the cluster nodes.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF], or
	 *         there are no cluster nodes
	 * @throws IOException if the given port is already in use or cannot be bound, or if there is a
	 *         problem waiting for the quit signal
	 */
	static public void main (final String[] args) throws IOException {
		LogManager.getLogManager();

		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final boolean sessionAware = Boolean.parseBoolean(args[1]);

		final Set<InetSocketAddress> nodeAddresses = new HashSet<>();
		for (int index = 2; index < args.length; ++index) {
			nodeAddresses.add(new SocketAddress(args[index]).toInetSocketAddress());
		}

		try (TcpSwitch server = new TcpSwitch(servicePort, sessionAware, nodeAddresses.toArray(new InetSocketAddress[0]))) {
			// print welcome message
			System.out.println("TCP switch running on one acceptor thread, enter \"quit\" to stop.");
			System.out.format("Service port is %s.\n", server.getServicePort());
			System.out.format("Session awareness is %s.\n", server.getSessionAware());
			System.out.println("The following node addresses have been registered:");
			for (final InetSocketAddress nodeAddress : server.getNodeAddresses()) {
				System.out.println(nodeAddress);
			}
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			// wait for stop signal on System.in
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		}
	}


	/**
	 * Instances of this inner class handle TCP client connections accepted by a TCP switch.
	 */
	static private class ConnectionHandler implements Runnable {
		private final Socket clientConnection;
		private final ExecutorService executorService;
		private final InetSocketAddress[] nodeAddresses;
		private final boolean sessionAware;


		/**
		 * Creates a new instance from a given client connection.
		 * @param clientConnection the connection
		 * @param threadPool the thread pool for transporter execution
		 * @param nodeAddresses the node addresses
		 * @param sessionAware the session awareness
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 */
		public ConnectionHandler (final Socket clientConnection, final ExecutorService threadPool, final InetSocketAddress[] nodeAddresses, final boolean sessionAware) {
	
			if (clientConnection == null | threadPool == null | nodeAddresses == null) throw new NullPointerException();

			this.clientConnection = clientConnection;
			this.executorService = threadPool;
			this.nodeAddresses = nodeAddresses;
			this.sessionAware = sessionAware;
		}


		/**
		 * Handles the client connection by transporting all data to a new server connection, and
		 * vice versa. Closes all connections upon completion.
		 */
		public void run () {
			try(Socket localClientConn = clientConnection){
				String clientIP = localClientConn.getInetAddress().getHostAddress();
				int serverIndex;

				/**
				 * not using ThreadLocalRandom because setting a seed is not supported
				 * //Random random = ThreadLocalRandom.current();
				 */

				if(this.sessionAware)
				{
					Random random = new Random(Long.parseLong(clientIP.replace(".","").replace(":","")));
					serverIndex = random.nextInt(this.nodeAddresses.length);
				}
				else
				{
					Random random = new Random();
					serverIndex = random.nextInt(this.nodeAddresses.length);
				}
				InetSocketAddress target = this.nodeAddresses[serverIndex];

				try(Socket targetConnection = new Socket(target.getHostName(),target.getPort())){
					forwardConnectionToSelectedTarget(localClientConn, targetConnection);
				}
			}
			catch (Exception e)
			{
				// blackjack and hookers
				System.out.println(e.toString());
			}
		}

		private void forwardConnectionToSelectedTarget(Socket clientConnection, Socket targetConnection) throws IOException{
			Semaphore sem = new Semaphore(-1);
			int BUFFER_SIZE = 1024;

				//long connectionOpenTime = System.currentTimeMillis();
				try (OutputStream monitoredUpstream = targetConnection.getOutputStream()) {
					executorService.submit(() -> {
						try {
							Streams.copy(clientConnection.getInputStream(), monitoredUpstream, BUFFER_SIZE);
						} catch (IOException e) {
							//
						} finally {
							sem.release();
						}
					});

					try (OutputStream monitoredDownstream = clientConnection.getOutputStream()) {
						executorService.submit(() -> {
							try {
								Streams.copy(targetConnection.getInputStream(), monitoredDownstream, BUFFER_SIZE);
							} catch (IOException e) {
								//
							} finally {
								sem.release();
							}
						});

						sem.acquireUninterruptibly();
					}
				}

		}
	}
}