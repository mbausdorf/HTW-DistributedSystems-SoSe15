package de.htw.ds.chat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.Format;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;
import de.sb.java.Threads;
import de.sb.java.TypeMetadata;
import de.sb.java.swing.BeanTableModel;
import de.sb.java.swing.TableCellFormater;


/**
 * Swing based chat pane. Note the use of a SpringLayout for professional resizing behavior.
 * @see <a
 *      href="http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Freference%2Fref-icons.htm">The
 *      original source of the Eclipse icons, available under EPL license</a>
 */
@TypeMetadata(copyright = "2010-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class ChatPanel extends JPanel {
	static private final long serialVersionUID = 1L;
	static private final String[] CHAT_ENTRY_COLUMN_NAMES = { "alias", "content", "timestamp" };
	static private final String[] CHAT_ENTRY_HEADER_NAMES = { "Sender", "Message", "Created" };
	static private final Format[] CHAT_ENTRY_COLUMN_FORMATS = { null, null, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM), null };

	static private final Image IMAGE_APP, IMAGE_SEND, IMAGE_TRASH, IMAGE_REFRESH;
	static {
		final Map<String,Image> images = new HashMap<>();
		try (InputStream fileSource = ChatPanel.class.getResourceAsStream("images.zip")) {
			try (ZipInputStream zipSource = new ZipInputStream(fileSource)) {
				for (ZipEntry zipEntry = zipSource.getNextEntry(); zipEntry != null; zipEntry = zipSource.getNextEntry()) {
					images.put(zipEntry.getName(), ImageIO.read(zipSource));
				}
			}
		} catch (final IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}

		IMAGE_APP = images.get("chat-client.png");
		IMAGE_SEND = images.get("send-icon.gif");
		IMAGE_TRASH = images.get("trash-icon.gif");
		IMAGE_REFRESH = images.get("refresh-icon.gif");
	}

	/**
	 * Returns the application icon image.
	 * @return the application icon image
	 */
	static public Image applicationIconImage () {
		return IMAGE_APP;
	}
	/**
	 * Creates a new column model for the chat table.
	 * @param tableModel the chat table model
	 * @return the corresponding column model
	 */
	static private TableColumnModel createChatColumnModel (final BeanTableModel<ChatEntry> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < CHAT_ENTRY_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(CHAT_ENTRY_COLUMN_NAMES[headerIndex]);

			final TableCellFormater formater = new TableCellFormater(CHAT_ENTRY_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex);
			column.setHeaderValue(CHAT_ENTRY_HEADER_NAMES[headerIndex]);
			column.setCellRenderer(formater);
			column.setCellEditor(formater);

			result.addColumn(column);
		}
		return result;
	}
	private final BeanTableModel<ChatEntry> chatTableModel;
	private final JTable chatEntryTable;
	private final JTextComponent aliasField;


	private final JTextField contentField;


	private final JTextComponent messageField;


	/**
	 * Public constructor.
	 * @param sendActionListener the listener adding new chat entries
	 * @param deleteActionListener the listener removing selected chat entries
	 * @param refreshActionListener the listener refreshing the chat area
	 */
	public ChatPanel (final ActionListener sendActionListener, final ActionListener deleteActionListener, final ActionListener refreshActionListener) {
		final SpringLayout layoutManager = new SpringLayout();
		this.setLayout(layoutManager);

		final Component aliasLabel, contentLabel, messageLabel, chatEntryScrollPane;
		final AbstractButton sendButton, trashButton, refreshButton;

		this.chatTableModel = new BeanTableModel<>(ChatEntry.class);
		this.chatEntryTable = new JTable(this.chatTableModel, createChatColumnModel(this.chatTableModel));
		this.chatEntryTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.chatEntryTable.setShowGrid(false);

		this.add(aliasLabel = new JLabel("Alias"));
		this.add(this.aliasField = new JTextField(20));
		this.add(contentLabel = new JLabel("Text"));
		this.add(this.contentField = new JTextField(20));
		this.add(sendButton = new JButton(new ImageIcon(IMAGE_SEND)));
		this.add(trashButton = new JButton(new ImageIcon(IMAGE_TRASH)));
		this.add(refreshButton = new JButton(new ImageIcon(IMAGE_REFRESH)));
		this.add(messageLabel = new JLabel("Message"));
		this.add(this.messageField = new JTextField());
		this.add(chatEntryScrollPane = new JScrollPane(this.chatEntryTable));

		this.aliasField.setText(ManagementFactory.getRuntimeMXBean().getName());
		this.messageField.setEditable(false);
		sendButton.setToolTipText("send");
		trashButton.setToolTipText("delete");
		refreshButton.setToolTipText("refresh");

		messageLabel.setForeground(Color.RED);
		sendButton.addActionListener(sendActionListener);
		trashButton.addActionListener(deleteActionListener);
		refreshButton.addActionListener(refreshActionListener);

		// same-faced alignment against parent
		for (final Component component : new Component[] { this.aliasField }) {
			layoutManager.putConstraint(SpringLayout.NORTH, component, 0, SpringLayout.NORTH, this);
		}
		for (final Component component : new Component[] { aliasLabel, chatEntryScrollPane, messageLabel }) {
			layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, this);
		}
		for (final Component component : new Component[] { this.messageField }) {
			layoutManager.putConstraint(SpringLayout.SOUTH, component, 0, SpringLayout.SOUTH, this);
		}
		for (final Component component : new Component[] { chatEntryScrollPane, this.messageField }) {
			layoutManager.putConstraint(SpringLayout.EAST, component, 0, SpringLayout.EAST, this);
		}
		layoutManager.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, refreshButton);

		// same-faced alignment against sibling
		for (final Component component : new Component[] { aliasLabel, contentLabel, this.contentField, refreshButton, trashButton, sendButton }) {
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, component, 0, SpringLayout.VERTICAL_CENTER, this.aliasField);
		}
		layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, messageLabel, 0, SpringLayout.VERTICAL_CENTER, this.messageField);

		// opposite-faced alignment against sibling
		layoutManager.putConstraint(SpringLayout.NORTH, chatEntryScrollPane, 5, SpringLayout.SOUTH, this.aliasField);
		layoutManager.putConstraint(SpringLayout.SOUTH, chatEntryScrollPane, -5, SpringLayout.NORTH, this.messageField);
		layoutManager.putConstraint(SpringLayout.WEST, this.aliasField, 5, SpringLayout.EAST, aliasLabel);
		layoutManager.putConstraint(SpringLayout.WEST, contentLabel, 5, SpringLayout.EAST, this.aliasField);
		layoutManager.putConstraint(SpringLayout.WEST, this.contentField, 5, SpringLayout.EAST, contentLabel);
		layoutManager.putConstraint(SpringLayout.WEST, sendButton, 15, SpringLayout.EAST, this.contentField);
		layoutManager.putConstraint(SpringLayout.WEST, trashButton, 5, SpringLayout.EAST, sendButton);
		layoutManager.putConstraint(SpringLayout.WEST, refreshButton, 5, SpringLayout.EAST, trashButton);
		layoutManager.putConstraint(SpringLayout.WEST, this.messageField, 5, SpringLayout.EAST, messageLabel);

		final ActionListener contentListener = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				sendButton.doClick();
			}
		};
		this.contentField.addActionListener(contentListener);
	}


	/**
	 * Returns the alias.
	 * @return the alias
	 */
	public String getAlias () {
		return this.aliasField.getText();
	}


	/**
	 * Returns the content.
	 * @return the content
	 */
	public String getContent () {
		return this.contentField.getText();
	}


	/**
	 * Returns the index of the selected chat entry, or {@code -1} if none is selected.
	 * @return the index of the selected chat entry, or {@code -1}
	 */
	public int getSelectionIndex() {
		return this.chatEntryTable.getSelectionModel().getMinSelectionIndex();
	}


	/**
	 * Sets the chat entries
	 * @param chatEntries the chat entries
	 */
	public void setChatEntries (final ChatEntry[] chatEntries) {
		this.chatTableModel.removeRows();
		this.chatTableModel.addRows(chatEntries);
	}


	/**
	 * Sets the message field text based on the given exception.
	 * @param exception the exception
	 */
	public void setMessage (final Throwable exception) {
		if (exception == null) {
			this.messageField.setText("");
		} else {
			final Throwable rootCause = Threads.rootCause(exception);
			this.messageField.setText(String.format("%s: %s", rootCause.getClass().getSimpleName(), rootCause.getMessage()));
		}
	}
}