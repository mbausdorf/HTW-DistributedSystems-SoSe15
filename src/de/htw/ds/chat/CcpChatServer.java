package de.htw.ds.chat;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import de.sb.java.TypeMetadata;
import de.sb.java.net.SocketAddress;


/**
 * Chat service server providing service methods using the custom "CCP" protocol. The protocol is
 * defined as follows. Note that there is a handshake between client and server regarding the
 * exchange of the ccpID protocol identifier:
 * 
 * <pre>
 * ccpRequest			:= ccpID [ getEntriesCall | addEntryCall | removeEntryCall ]
 * ccpResponse			:= ccpID [ getEntriesResult | void ]
 * ccpID				:= "CCP"
 * getEntriesCall		:= "getEntries"
 * getEntriesResult		:= elementCount { chatEntry }
 * addEntryCall			:= "addEntry" chatEntry
 * removeEntryCall		:= "removeEntry" index
 * chatEntry			:= alias content timestamp
 * alias				:= UTF-8
 * content				:= UTF-8
 * timestamp			:= long
 * elementCount			:= int
 * index				:= int
 * </pre>
 * 
 * Note that this class is declared final because it provides an application entry point, and is
 * therefore not supposed to be extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class CcpChatServer implements Runnable, AutoCloseable {
	static private final String PROTOCOL_IDENTIFIER = "ccp";

	/**
	 * Application entry point. The given runtime parameters must be a Service port.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use or cannot be bound, or if there is a
	 *         problem waiting for the quit signal
	 */
	static public void main (final String[] args) throws IOException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);

		try (CcpChatServer server = new CcpChatServer(servicePort, 50)) {
			System.out.format("CCP chat server running on one acceptor thread, enter \"quit\" to stop.\n");
			System.out.format("Service URI is \"%s\".\n", server.getServiceURI());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		}
	}
	private final URI serviceURI;
	private final ServerSocket serviceSocket;


	private final ChatService delegate;


	/**
	 * Public constructor, also starting the acceptor thread.
	 * @param servicePort the service port
	 * @param delegate the protocol independent chat service delegate
	 * @throws NullPointerException if the given delegate is {@code null}
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound
	 */
	public CcpChatServer (final int servicePort, final ChatService delegate) throws IOException {
		if (delegate == null) throw new java.lang.NullPointerException();
		if (servicePort <= 0 | servicePort > 0xFFFF) throw new IllegalArgumentException();

		try {
			this.serviceURI = new URI("ccp", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/", null, null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException();
		}

		this.serviceSocket = new ServerSocket(servicePort);
		this.delegate = delegate;
		new Thread(this, "ccp-acceptor").start();
	}


	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param maxEntries the maximum number of chat entries
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF], or
	 *         if the maximum number of chat entries is negative
	 * @throws IOException if the given port is already in use, or cannot be bound
	 */
	public CcpChatServer (final int servicePort, final int maxEntries) throws IOException {
		this(servicePort, new ChatService(maxEntries));
	}


	/**
	 * Closes the server.
	 * @throws IOException if there is an I/O related problem
	 */
	public void close () throws IOException {
		this.serviceSocket.close();
	}


	/**
	 * Returns the service URI.
	 * @return the service URI
	 */
	public URI getServiceURI () {
		return this.serviceURI;
	}


	/**
	 * Periodically blocks until a request arrives, handles the latter subsequently.
	 */
	public void run () {
		while (true) {
			try {
				final Socket connection;
				try {
					connection = this.serviceSocket.accept();
				} catch (final SocketException exception) {
					break;
				}

				try {
					final Runnable requestHandler = new Runnable() {
						public void run () {
							try {
								final BufferedOutputStream bufferedByteSink = new BufferedOutputStream(connection.getOutputStream());
								final DataOutputStream dataSink = new DataOutputStream(bufferedByteSink);
								final DataInputStream dataSource = new DataInputStream(connection.getInputStream());

								// verify and acknowledge protocol
								for (final char character : PROTOCOL_IDENTIFIER.toCharArray()) {
									if (dataSource.readChar() != character) throw new ProtocolException();
								}
								dataSink.writeChars(PROTOCOL_IDENTIFIER);
								bufferedByteSink.flush();

								// read method
								final String method = dataSource.readUTF();
								switch (method) {
									case "getEntries": {
										final ChatEntry[] result = CcpChatServer.this.delegate.getEntries();
										dataSink.writeInt(result.length);
										for (final ChatEntry entry : result) {
											dataSink.writeUTF(entry.getAlias());
											dataSink.writeUTF(entry.getContent());
											dataSink.writeLong(entry.getTimestamp());
										}
										break;
									}
									case "addEntry": {
										final String alias = dataSource.readUTF();
										final String content = dataSource.readUTF();
										final long timestamp = dataSource.readLong();
										final ChatEntry entry = new ChatEntry(alias, content, timestamp);
										CcpChatServer.this.delegate.addEntry(entry);
										break;
									}
									case "removeEntry": {
										final int index = dataSource.readInt();
										CcpChatServer.this.delegate.removeEntry(index);
										break;
									}
									default: {
										throw new ProtocolException();
									}
								}
								bufferedByteSink.flush();
							} catch (final Exception exception) {
								try {
									exception.printStackTrace();
								} catch (final Exception nestedException) {}
							} finally {
								try {
									connection.close();
								} catch (final Exception exception) {}
							}
						}
					};

					final Thread thread = new Thread(requestHandler, "ccp-service");
					thread.setDaemon(false);
					thread.start();
				} catch (final Exception exception) {
					try {
						connection.close();
					} catch (final Exception nestedException) {
						exception.addSuppressed(nestedException);
					}
					throw exception;
				}
			} catch (final Exception exception) {
				exception.printStackTrace();
			}
		}
	}
}