package de.htw.ds.chat;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import de.sb.java.TypeMetadata;


/**
 * Chat service interprocess proxy for the custom "ccp" protocol. The protocol is defined as
 * follows. Note that there is a handshake between client and server regarding the exchange of the
 * ccpID protocol identifier:
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
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class CcpChatStub {
	static private final String PROTOCOL_IDENTIFIER = "ccp";

	private final InetSocketAddress serviceAddress;


	/**
	 * Public constructor
	 * @param serviceAddress the service address
	 * @throws NullPointerException if the given serviceAddress is {@code null}
	 */
	public CcpChatStub (final InetSocketAddress serviceAddress) {
		if (serviceAddress == null) throw new java.lang.NullPointerException();
		this.serviceAddress = serviceAddress;
	}


	/**
	 * Delegates the addEntry method to the server.
	 * @param entry the chat entry
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IOException if an I/O related problem occurs
	 */
	public void addEntry (final ChatEntry entry) throws IOException {
		try (Socket connection = this.getConnection()) {
			final DataOutputStream dataSink = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));

			dataSink.writeUTF("addEntry");
			dataSink.writeUTF(entry.getAlias());
			dataSink.writeUTF(entry.getContent());
			dataSink.writeLong(entry.getTimestamp());
			dataSink.flush();
		}
	}


	/**
	 * Returns a new TCP connection to the server after verifying the protocol identifier.
	 * @return the TCP connection
	 * @throws IOException if an I/O related problem occurs
	 */
	private Socket getConnection () throws IOException {
		final Socket connection = new Socket(this.serviceAddress.getHostName(), this.serviceAddress.getPort());
		try {
			final DataInputStream dataSource = new DataInputStream(connection.getInputStream());
			final DataOutputStream dataSink = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));

			dataSink.writeChars(PROTOCOL_IDENTIFIER);
			dataSink.flush();
			for (final char character : PROTOCOL_IDENTIFIER.toCharArray()) {
				if (dataSource.readChar() != character) throw new ProtocolException();
			}
			return connection;
		} catch (final Exception exception) {
			try {
				connection.close();
			} catch (final Exception nestedException) {
				exception.addSuppressed(nestedException);
			}
			throw exception;
		}
	}


	/**
	 * Delegates the getEntries method to the server.
	 * @return the chat entries
	 * @throws IOException if an I/O related problem occurs
	 */
	public ChatEntry[] getEntries () throws IOException {
		try (Socket connection = this.getConnection()) {
			final DataInputStream dataSource = new DataInputStream(connection.getInputStream());
			final DataOutputStream dataSink = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));

			dataSink.writeUTF("getEntries");
			dataSink.flush();

			final ChatEntry[] result = new ChatEntry[dataSource.readInt()];
			for (int index = 0; index < result.length; ++index) {
				final String alias = dataSource.readUTF();
				final String content = dataSource.readUTF();
				final long creationTimestamp = dataSource.readLong();
				result[index] = new ChatEntry(alias, content, creationTimestamp);
			}
			return result;
		}
	}


	/**
	 * Delegates the removeEntry method to the server.
	 * @param index the chat entry index
	 * @throws IllegalArgumentException if the given index is out of bounds
	 * @throws IOException if an I/O related problem occurs
	 */
	public void removeEntry (final int index) throws IOException {
		try (Socket connection = this.getConnection()) {
			final DataOutputStream dataSink = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));

			dataSink.writeUTF("removeEntry");
			dataSink.writeInt(index);
			dataSink.flush();
		}
	}
}