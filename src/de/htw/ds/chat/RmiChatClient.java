package de.htw.ds.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import de.sb.java.TypeMetadata;


/**
 * ChatPanel based client class using a Java-RMI service. Note that this class is declared final
 * because it provides an application entry point, and therefore not supposed to be extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class RmiChatClient {

	/**
	 * Application entry point. The given runtime parameter must be an RMI service URI.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given URI is malformed
	 * @throws RemoteException if the service registry cannot be contacted
	 * @throws UnsupportedLookAndFeelException if the VM does not support Nimbus look-and-feel
	 */
	static public void main (final String[] args) throws URISyntaxException, RemoteException, UnsupportedLookAndFeelException {
		final URI serviceURI = new URI(args[0]);
		final Registry serviceRegistry = LocateRegistry.getRegistry(serviceURI.getHost(), serviceURI.getPort());
		final String serviceName = serviceURI.getPath().substring(1);

		UIManager.setLookAndFeel(new NimbusLookAndFeel());
		final RmiChatClient client = new RmiChatClient(serviceRegistry, serviceName);

		final JFrame frame = new JFrame("RMI Chat Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(client.getContentPane());
		frame.setIconImage(ChatPanel.applicationIconImage());
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}
	private final ChatPanel chatPanel;
	private final Registry serviceRegistry;
	private final String serviceName;


	private volatile transient RmiChatService serviceProxy;


	/**
	 * Public constructor.
	 * @param serviceRegistry the remove service registry
	 * @param serviceName the name of a service within the service registry
	 * @throws NullPointerException if the given registry or name is {@code null}
	 */
	public RmiChatClient (final Registry serviceRegistry, final String serviceName) {
		if (serviceRegistry == null | serviceName == null) throw new java.lang.NullPointerException();

		this.serviceRegistry = serviceRegistry;
		this.serviceName = serviceName;

		final ActionListener sendButtonActionListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				RmiChatClient.this.addChatEntry();
				RmiChatClient.this.refreshChatEntries();
			}
		};
		final ActionListener deleteButtonActionListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				final int selectionIndex = ((ChatPanel) ((JButton) event.getSource()).getParent()).getSelectionIndex();
				if (selectionIndex != -1) {
					RmiChatClient.this.removeChatEntry(selectionIndex);
					RmiChatClient.this.refreshChatEntries();
				}
			}
		};
		final ActionListener refreshButtonActionListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				RmiChatClient.this.refreshChatEntries();
			}
		};

		this.chatPanel = new ChatPanel(sendButtonActionListener, deleteButtonActionListener, refreshButtonActionListener);
		this.chatPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

		this.refreshChatEntries();
	}


	/**
	 * Adds a chat entry to the chat service.
	 */
	public void addChatEntry () {
		try {
			final ChatEntry entry = new ChatEntry(this.chatPanel.getAlias(), this.chatPanel.getContent(), System.currentTimeMillis());
			try {
				if (this.serviceProxy == null) throw new RemoteException();
				this.serviceProxy.addEntry(entry);
			} catch (final RemoteException exception) {
				this.serviceProxy = (RmiChatService) this.serviceRegistry.lookup(this.serviceName);
				this.serviceProxy.addEntry(entry);
			}
		} catch (final Exception exception) {
			this.chatPanel.setMessage(exception);
		}
	}


	/**
	 * Returns the content pane.
	 * @return the content pane
	 */
	public JComponent getContentPane () {
		return this.chatPanel;
	}


	/**
	 * Refreshes the chat entries from the chat service.
	 */
	public void refreshChatEntries () {
		try {
			ChatEntry[] chatEntries;
			try {
				if (this.serviceProxy == null) throw new RemoteException();
				chatEntries = this.serviceProxy.getEntries();
			} catch (final RemoteException exception) {
				this.serviceProxy = (RmiChatService) this.serviceRegistry.lookup(this.serviceName);
				chatEntries = this.serviceProxy.getEntries();
			}

			this.chatPanel.setChatEntries(chatEntries);
			this.chatPanel.setMessage(null);
		} catch (final Exception exception) {
			this.chatPanel.setMessage(exception);
		}
	}


	/**
	 * Removes the chat entry from the chat service.
	 * @param index the chat entry index
	 */
	public void removeChatEntry (final int index) {
		try {
			try {
				if (this.serviceProxy == null) throw new RemoteException();
				this.serviceProxy.removeEntry(index);
			} catch (final RemoteException exception) {
				this.serviceProxy = (RmiChatService) this.serviceRegistry.lookup(this.serviceName);
				this.serviceProxy.removeEntry(index);
			}
			
			this.chatPanel.setMessage(null);
		} catch (final Exception exception) {
			this.chatPanel.setMessage(exception);
		}
	}
}