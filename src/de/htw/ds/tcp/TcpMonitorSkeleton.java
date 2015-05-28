package de.htw.ds.tcp;

import de.sb.java.TypeMetadata;
import de.sb.java.io.MultiInputStream;
import de.sb.java.io.MultiOutputStream;
import de.sb.java.io.Streams;
import de.sb.java.net.SocketAddress;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * This class models a TCP monitor, i.e. a TCP router that mirrors all information between two
 * ports, while logging it at the same time. Note that this class is declared final because it
 * provides an application entry point, and therefore not supposed to be extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class TcpMonitorSkeleton implements Runnable, AutoCloseable {

	/**
	 * Instances of this static inner class handle TCP client connections accepted by a TCP monitor.
	 */
	// TODO: Remove
	@SuppressWarnings("unused")
	static private class ConnectionHandler implements Runnable {
		private final Socket clientConnection;
		private final ExecutorService executorService;
		private final InetSocketAddress forwardAddress;
		private final TcpMonitorWatcher watcher;

		/**
		 * Creates a new instance from a given client connection.
		 * @param clientConnection the connection
		 * @param executorService the executor service
		 * @param forwardAddress the forward address
		 * @param watcher the watcher
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 */
		public ConnectionHandler (final Socket clientConnection, final ExecutorService executorService, final InetSocketAddress forwardAddress, final TcpMonitorWatcher watcher) {

			if (clientConnection == null | executorService == null | forwardAddress == null | watcher == null) throw new NullPointerException();

			this.clientConnection = clientConnection;
			this.executorService = executorService;
			this.forwardAddress = forwardAddress;
			this.watcher = watcher;
		}


		/**
		 * Handles the client connection by transporting all data to a new server connection, and
		 * vice versa. Closes all connections upon completion.
		 */
		public void run () {
			try (Socket serverConnection = new Socket(this.forwardAddress.getHostName(), this.forwardAddress.getPort())) {
				// TODO: Transport all content from the client connection's input stream into
				// both the server connection's output stream and a byte output stream. In
				// parallel, transport all content from the server connection's input stream
				// into both the client connection's output stream and another byte output stream.
				// Note that the existing utility classes MultiInputStream, MultiOutputStream and
				// Streams#copy() might allow a highly elegant (and slim) solution, especially
				// in conjunction with Java 8 Lambda-Operators.

				// Start two transporter threads, and resynchronize them before closing all
				// resources; any exceptions acquired from the child threads should be logged
				// using "this.watcher.exceptionCatched()" - see code below. If all goes well,
				// use "ByteArrayOutputStream#toByteArray()" to get the respective request and
				// response data; use it to create a TcpMonitorRecord, and flush a log output
				// using "this.watcher.recordCreated()".

				// Note that you'll need 2 transporters in 1-2 separate threads to complete this
				// tasks, as you cannot foresee if the client or the server closes the connection,
				// or if the protocol communicated involves handshakes. Either case implies you'd
				// end up reading "too much" if you try to transport both communication directions
				// within this thread, creating a deadlock scenario! The easiest solution probably
				// involves the ConnectionHandler's executor service (see Method submit()), and
				// resynchronization using the futures returned by this method. Finally, beware that
				// HTTP usually implies delayed closing of connections after transmissions due to
				// connection caching.

				// Especially make sure that all connections and files are properly closed in
				// any circumstances! Note that closing one socket stream closes the underlying
				// socket connection (and therefore also the second socket stream) as well. Also
				// note that a SocketInputStream's read() method will throw a SocketException when
				// interrupted while blocking, which is "normal" behavior and should be handled as
				// if the read() Method returned -1!
			} catch (final Exception exception) {
				this.watcher.exceptionCatched(exception);
			} finally {
				try {
					this.clientConnection.close();
				} catch (final Exception exception) {}
			}
		}
	}
	/**
	 * Application entry point. The given runtime parameters must be a service port, a server
	 * context directory, and the forward socket-address as address:port combination.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use or cannot be bound, if the given
	 *         context path is not a directory, or if there is a problem waiting for the quit signal
	 */
	static public void main (final String[] args) throws IOException {
		LogManager.getLogManager();

		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final Path contextPath = Paths.get(args[1]).normalize();
		final InetSocketAddress forwardAddress = new SocketAddress(args[2]).toInetSocketAddress();

		final TcpMonitorWatcher watcher = new TcpMonitorWatcher() {
			public void exceptionCatched (final Exception exception) {
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			}


			public void recordCreated (final TcpMonitorRecord record) {
				final String fileName = String.format("%1$tF-%1$tH.%1$tM.%1$tS.%tL-%d.log", record.getOpenTimestamp(), record.getIdentity());
				final Path filePath = contextPath.resolve(fileName);
				try (OutputStream fileSink = Files.newOutputStream(filePath)) {
					fileSink.write(record.getRequestData());
					fileSink.write("\n\n*** RESPONSE DATA ***\n\n".getBytes("ASCII"));
					fileSink.write(record.getResponseData());
				} catch (final Exception exception) {
					this.exceptionCatched(exception);
				}
			}
		};

		try (TcpMonitorSkeleton server = new TcpMonitorSkeleton(servicePort, forwardAddress, watcher)) {
			// print welcome message
			System.out.println("TCP monitor running on one acceptor thread, enter \"quit\" to stop.");
			System.out.format("Service port is %s.\n", server.getServicePort());
			System.out.format("Forward socket address is %s:%s.\n", server.getForwardAddress().getHostName(), server.getForwardAddress().getPort());
			System.out.format("Context directory is %s.\n", contextPath);
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			// wait for stop signal on System.in
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		}
	}
	private final ServerSocket serviceSocket;
	private final ExecutorService executorService;


	private final InetSocketAddress forwardAddress;


	private final TcpMonitorWatcher watcher;


	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param forwardAddress the forward address
	 * @param watcher the monitor watcher that is notified of connection activity
	 * @throws NullPointerException if the given address or watcher is {@code null}
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound
	 */
	public TcpMonitorSkeleton(final int servicePort, final InetSocketAddress forwardAddress, final TcpMonitorWatcher watcher) throws IOException {
		if (forwardAddress == null | watcher == null) throw new NullPointerException();

		this.executorService = Executors.newCachedThreadPool();
		this.serviceSocket = new ServerSocket(servicePort);
		this.forwardAddress = forwardAddress;
		this.watcher = watcher;

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
		this.executorService.shutdown();
	}


	/**
	 * Returns the forward address.
	 * @return the forward address
	 */
	public InetSocketAddress getForwardAddress () {
		return forwardAddress;
	}



	/**
	 * Returns the service port.
	 * @return the service port
	 */
	public int getServicePort () {
		return this.serviceSocket.getLocalPort();
	}


	/**
	 * Periodically blocks until a request arrives, handles the latter subsequently.
	 */
	public void run () {
		while (true) {
			Socket clientConnection = null;
			try {
				clientConnection = this.serviceSocket.accept();

				final ConnectionHandler connectionHandler = new ConnectionHandler(clientConnection, this.executorService, this.forwardAddress, this.watcher);
				this.executorService.execute(connectionHandler);
			} catch (final SocketException exception) {
				break;
			} catch (final Exception exception) {
				try {
					clientConnection.close();
				} catch (final Exception nestedException) {}
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			}
		}
	}
}