package de.htw.ds.chat;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.WebServiceException;
import de.sb.java.TypeMetadata;


/**
 * Chat service interface for JAX-WS subject-oriented web-service
 * serving as the base for dynamic WSDL generation at runtime.
 */
@WebService
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public interface SoapChatService {

	/**
	 * Adds a chat entry.
	 * @param chatEntry the chat entry
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	void addEntry (
		@WebParam(name = "entry") ChatEntry entry
	);


	/**
	 * Returns the chat entries.
	 * @return the chat entries
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	ChatEntry[] getEntries ();


	/**
	 * Removes the chat entry at the given index.
	 * @param index the chat entry index
	 * @throws IllegalArgumentException if the given index is out of bounds
	 * @throws WebServiceException if there's a JAX-WS related problem
	 */
	void removeEntry (
		@WebParam(name = "index") int index
	);
}