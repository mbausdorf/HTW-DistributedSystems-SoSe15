package de.htw.ds.chat;

import java.util.LinkedList;
import java.util.List;
import de.sb.java.TypeMetadata;


/**
 * ServiceProtocol independent POJO chat service class. Note that this implementation is
 * thread-safe.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class ChatService {

	private final List<ChatEntry> chatEntries;
	private final int maxSize;


	/**
	 * Public constructor.
	 * @param maxSize the maximum number of chat entries
	 * @throws IllegalArgumentException if the maximum number of chat entries is negative
	 */
	public ChatService (final int maxSize) {
		if (maxSize <= 0) throw new IllegalArgumentException(Integer.toString(maxSize));

		this.chatEntries = new LinkedList<ChatEntry>();
		this.maxSize = maxSize;
	}


	/**
	 * Adds a chat entry.
	 * @param entry the chat entry
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	public void addEntry (final ChatEntry entry) {
		if (entry == null) throw new java.lang.NullPointerException();

		synchronized (this.chatEntries) {
			if (this.chatEntries.size() == this.maxSize) {
				this.chatEntries.remove(this.chatEntries.size() - 1);
			}
			this.chatEntries.add(0, entry);
		}
	}


	/**
	 * Returns the chat entries.
	 * @return the chat entries
	 */
	public ChatEntry[] getEntries () {
		synchronized (this.chatEntries) {
			return this.chatEntries.toArray(new ChatEntry[this.chatEntries.size()]);
		}
	}


	/**
	 * Removes a chat entry.
	 * @param index the chat entry index
	 * @throws IllegalArgumentException if the given index is out of bounds
	 */
	public void removeEntry (final int index) {
		try {
			synchronized (this.chatEntries) {
				this.chatEntries.remove(index);
			}
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalArgumentException(exception);
		}
	}
}