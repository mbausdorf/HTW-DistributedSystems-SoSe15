package de.htw.ds.chat;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.xml.ws.WebServiceException;
import de.sb.java.TypeMetadata;


/**
 * Chat service interface for Java-RMI.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public interface RmiChatService extends Remote {

	/**
	 * Adds a chat entry.
	 * @param chatEntry the chat entry
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws RemoteException if there is an RMI-related problem
	 */
	void addEntry (ChatEntry entry) throws RemoteException;


	/**
	 * Returns the chat entries.
	 * @return the chat entries
	 */
	ChatEntry[] getEntries () throws RemoteException;


	/**
	 * Removes a chat entry.
	 * @param index the chat entry index
	 * @throws IllegalArgumentException if the given index is out of bounds
	 * @throws WebServiceException if there's an RMI related problem
	 */
	void removeEntry (int index) throws RemoteException;
}