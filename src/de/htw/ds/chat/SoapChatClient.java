package de.htw.ds.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import de.sb.java.TypeMetadata;


/**
 * ChatPanel based bottom-up client class using a JAX-WS web-service. Note that this class is
 * declared final because it provides an application entry point, and therefore not supposed to be
 * extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class SoapChatClient {

	/**
	 * Application entry point. The given runtime parameters must be a SOAP service URI.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given URI is malformed
	 * @throws MalformedURLException if the given URI cannot be converted into a URL
	 * @throws UnsupportedLookAndFeelException if the VM does not support Nimbus look-and-feel
	 */
	static public void main (final String[] args) throws URISyntaxException, MalformedURLException, UnsupportedLookAndFeelException {
		final URL wsdlLocator = new URL(args[0] + "?wsdl");
		final Service proxyFactory = Service.create(wsdlLocator, new QName("http://chat.ds.htw.de/", SoapChatService.class.getSimpleName()));
		final SoapChatService serviceProxy = proxyFactory.getPort(SoapChatService.class);

		UIManager.setLookAndFeel(new NimbusLookAndFeel());
		final SoapChatClient client = new SoapChatClient(serviceProxy);

		final JFrame frame = new JFrame("Dynamic (bottom-up) JAX-WS Chat Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(client.getContentPane());
		frame.setIconImage(ChatPanel.applicationIconImage());
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}
	private final ChatPanel chatPanel;


	private final SoapChatService serviceProxy;


	/**
	 * Public constructor.
	 * @param serviceProxy the chat service proxy
	 * @throws NullPointerException if the given proxy is {@code null}
	 */
	public SoapChatClient (final SoapChatService serviceProxy) {
		if (serviceProxy == null) throw new java.lang.NullPointerException();

		this.serviceProxy = serviceProxy;

		final ActionListener sendButtonActionListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				SoapChatClient.this.addChatEntry();
				SoapChatClient.this.refreshChatEntries();
			}
		};
		final ActionListener deleteButtonActionListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				final int selectionIndex = ((ChatPanel) ((JButton) event.getSource()).getParent()).getSelectionIndex();
				if (selectionIndex != -1) {
					SoapChatClient.this.removeChatEntry(selectionIndex);
					SoapChatClient.this.refreshChatEntries();
				}
			}
		};
		final ActionListener refreshButtonActionListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				SoapChatClient.this.refreshChatEntries();
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
			this.serviceProxy.addEntry(entry);
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
			final ChatEntry[] chatEntries = this.serviceProxy.getEntries();
			this.chatPanel.setChatEntries(chatEntries);
			this.chatPanel.setMessage(null);
		} catch (final Exception exception) {
			this.chatPanel.setMessage(exception);
		}
	}


	/**
	 * Removes the chat entry from the chat service.
	 * @param index the chat entry index
	 * @throws IllegalArgumentException if the given index is out of bounds
	 */
	public void removeChatEntry (final int index) {
		try {
			this.serviceProxy.removeEntry(index);
			this.chatPanel.setMessage(null);
		} catch (final Exception exception) {
			this.chatPanel.setMessage(exception);
		}
	}
}