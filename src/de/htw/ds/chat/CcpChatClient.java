package de.htw.ds.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import de.sb.java.TypeMetadata;
import de.sb.java.net.SocketAddress;


/**
 * ChatPanel based client class using a custom protocol based service.Note that this class is
 * declared final because it provides an application entry point, and therefore not supposed to be
 * extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class CcpChatClient {

	/**
	 * Application entry point. The given runtime parameters must be a socket-address.
	 * @param args the given runtime arguments
	 * @throws IOException if there is an I/O related problem
	 * @throws UnsupportedLookAndFeelException if the VM does not support Nimbus look-and-feel
	 */
	static public void main (final String[] args) throws IOException, UnsupportedLookAndFeelException {
		final InetSocketAddress serviceAddress = new SocketAddress(args[0]).toInetSocketAddress();
		final CcpChatStub chatServiceProxy = new CcpChatStub(serviceAddress);

		UIManager.setLookAndFeel(new NimbusLookAndFeel());
		final CcpChatClient client = new CcpChatClient(chatServiceProxy);

		final JFrame frame = new JFrame("CCP Chat Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(client.getContentPane());
		frame.setIconImage(ChatPanel.applicationIconImage());
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}
	private final ChatPanel chatPanel;


	private final CcpChatStub serviceStub;


	/**
	 * Public constructor.
	 * @param serviceStub the chat service stub
	 * @throws NullPointerException if the given stub is {@code null}
	 */
	public CcpChatClient (final CcpChatStub serviceStub) {

		if (serviceStub == null) throw new java.lang.NullPointerException();

		this.serviceStub = serviceStub;

		final ActionListener sendButtonActionListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				CcpChatClient.this.addChatEntry();
				CcpChatClient.this.refreshChatEntries();
			}
		};
		final ActionListener deleteButtonActionListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				final int selectionIndex = ((ChatPanel) ((JButton) event.getSource()).getParent()).getSelectionIndex();
				if (selectionIndex != -1) {
					CcpChatClient.this.removeChatEntry(selectionIndex);
					CcpChatClient.this.refreshChatEntries();
				}
			}
		};
		final ActionListener refreshButtonActionListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				CcpChatClient.this.refreshChatEntries();
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
			this.serviceStub.addEntry(entry);
		} catch (final Exception exception) {
			this.chatPanel.setMessage(exception);
		}
	}


	/**
	 * Returns the content pane.
	 * @return the content pane.
	 */
	public JComponent getContentPane () {
		return this.chatPanel;
	}


	/**
	 * Refreshes the chat entries from the chat service.
	 */
	public void refreshChatEntries () {
		try {
			final ChatEntry[] chatEntries = this.serviceStub.getEntries();
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
			this.serviceStub.removeEntry(index);
			this.chatPanel.setMessage(null);
		} catch (final Exception exception) {
			this.chatPanel.setMessage(exception);
		}
	}
}