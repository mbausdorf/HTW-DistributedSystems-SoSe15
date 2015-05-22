package de.htw.ds.tcp;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.Format;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;
import de.sb.java.Threads;
import de.sb.java.TypeMetadata;
import de.sb.java.swing.BeanTableModel;
import de.sb.java.swing.TableCellFormater;


/**
 * TCP monitor pane for use within the TcpMonitor2 class.
 * @see <a
 *      href="http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Freference%2Fref-icons.htm">The
 *      original source of the Eclipse icons, available under EPL license</a>
 */
@TypeMetadata(copyright = "2012-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class TcpMonitorPane extends JPanel implements TcpMonitorWatcher {
	static private final long serialVersionUID = 1L;
	static private final Image IMAGE_APP;
	static private final ImageIcon ICON_START, ICON_SUSPEND, ICON_RESUME, ICON_STOP, ICON_TRASH;
	static private final String[] RECORD_COLUMN_NAMES = { "identity", "openTimestamp", "closeTimestamp", "requestLength", "responseLength" };
	static private final String[] RECORD_HEADER_NAMES = { "ID", "Opened", "Closed", "Request Bytes", "Response Bytes" };
	static private final int[] RECORD_COLUMN_WIDTH = { 60, 40, 40, 30, 30 };
	static private final Format[] RECORD_COLUMN_FORMATS = {
			null,
			DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM),
			DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM),
			null,
			null
	};

	static {
		final Map<String,Image> images = new HashMap<>();
		try (InputStream fileSource = TcpMonitorPane.class.getResourceAsStream("images.zip")) {
			try (ZipInputStream zipSource = new ZipInputStream(fileSource)) {
				for (ZipEntry zipEntry = zipSource.getNextEntry(); zipEntry != null; zipEntry = zipSource.getNextEntry()) {
					images.put(zipEntry.getName(), ImageIO.read(zipSource));
				}
			}
		} catch (final IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}

		IMAGE_APP = images.get("tcp-monitor.png");
		ICON_START = new ImageIcon(images.get("start-icon.gif"), "start");
		ICON_SUSPEND = new ImageIcon(images.get("suspend-icon.gif"), "suspend");
		ICON_RESUME = new ImageIcon(images.get("resume-icon.gif"), "resume");
		ICON_STOP = new ImageIcon(images.get("stop-icon.gif"), "stop");
		ICON_TRASH = new ImageIcon(images.get("trash-icon.gif"), "clear");
	}

	/**
	 * Sets the given button's icon to the given one, and it's tool-tip text to the comment
	 * contained within the given image icon.
	 * @param button the button
	 * @param icon the image icon
	 * @throws NullPointerException if the given button or icon is {@code null}
	 */
	static private void changeIconAndTooltip (final AbstractButton button, final ImageIcon icon) {
		button.setIcon(icon);
		button.setToolTipText(icon.getDescription());
	}
	/**
	 * Application entry point.
	 * @param args the given runtime arguments
	 * @throws UnsupportedLookAndFeelException if the VM does not support Nimbus look-and-feel
	 */
	static public void main (final String[] args) throws UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(new NimbusLookAndFeel());
		final TcpMonitorPane contentPane = new TcpMonitorPane();
		contentPane.setBorder(new EmptyBorder(2, 2, 2, 2));

		final JFrame frame = new JFrame("TCP Monitor");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(contentPane);
		frame.setIconImage(IMAGE_APP);
		frame.pack();
		frame.setSize(800, 600);
		frame.setVisible(true);

		final WindowListener windowListener = new WindowAdapter() {
			public void windowClosed (final WindowEvent event) {
				contentPane.disconnect();
			}
		};
		frame.addWindowListener(windowListener);
	}
	/**
	 * Returns a new verifier for port values stored in text fields verifier.
	 * @return the port verifier
	 */
	static private InputVerifier newPortVerifier () {
		return new InputVerifier () {
			public boolean verify (final JComponent component) {
				if (!(component instanceof JTextComponent)) return false;
				final String text = ((JTextComponent) component).getText();
				try {
					final int port = Integer.parseInt(text);
					return port >= 0 & port <= 0xFFFF;
				} catch (final NumberFormatException exception) {
					return false;
				}
			}
		};
	}
	/**
	 * Creates a new column model for the record table.
	 * @param tableModel the table model
	 * @return the corresponding column model
	 */
	static private TableColumnModel newRecordColumnModel (final BeanTableModel<TcpMonitorRecord> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < RECORD_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(RECORD_COLUMN_NAMES[headerIndex]);

			final TableCellFormater formater = new TableCellFormater(RECORD_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex, RECORD_COLUMN_WIDTH[headerIndex]);
			column.setHeaderValue(RECORD_HEADER_NAMES[headerIndex]);
			column.setCellRenderer(formater);
			column.setCellEditor(formater);
			result.addColumn(column);
		}

		return result;
	}


	private final JTextComponent messageField, servicePortField, forwardHostField, forwardPortField, requestArea, responseArea;


	private final AbstractButton startButton, clearButton, stopButton;


	private final BeanTableModel<TcpMonitorRecord> recordTableModel;


	private volatile AutoCloseable monitor;


	/**
	 * Creates a new instance.
	 */
	public TcpMonitorPane () {
		this.messageField = new JTextField();
		this.startButton = new JButton();
		this.clearButton = new JButton();
		this.stopButton = new JButton();
		this.recordTableModel = new BeanTableModel<>(TcpMonitorRecord.class);
		this.servicePortField = new JTextField("8010");
		this.forwardHostField = new JTextField("localhost");
		this.forwardPortField = new JTextField("80");
		this.requestArea = new JTextArea();
		this.responseArea = new JTextArea();
		this.monitor = null;

		final InputVerifier portVerifier = newPortVerifier();
		this.servicePortField.setInputVerifier(portVerifier);
		this.forwardPortField.setInputVerifier(portVerifier);
		this.messageField.setEditable(false);
		this.clearButton.setEnabled(false);
		this.stopButton.setEnabled(false);
		this.requestArea.setEditable(false);
		this.responseArea.setEditable(false);
		changeIconAndTooltip(this.startButton, ICON_START);
		changeIconAndTooltip(this.clearButton, ICON_TRASH);
		changeIconAndTooltip(this.stopButton, ICON_STOP);

		final Container statusPane, messagePane;
		final JTable recordTable;
		{
			final SpringLayout layoutManager = new SpringLayout();
			this.setLayout(layoutManager);

			recordTable = new JTable(this.recordTableModel, newRecordColumnModel(this.recordTableModel));
			recordTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			recordTable.setShowGrid(false);

			final JSplitPane textAreaSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(this.requestArea), new JScrollPane(this.responseArea));
			final JSplitPane recordSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, new JScrollPane(recordTable), textAreaSplitPane);
			this.add(statusPane = new JPanel());
			this.add(recordSplitPane);
			this.add(messagePane = new JPanel());
			textAreaSplitPane.setResizeWeight(0.5);
			recordSplitPane.setDividerLocation(120);
			recordSplitPane.setResizeWeight(0.25);

			// align component edges against this panel and vice versa (usually on same side)
			for (final Component component : new Component[] { statusPane, recordSplitPane, messagePane }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, this);
			}
			for (final Component component : new Component[] { statusPane, recordSplitPane, messagePane }) {
				layoutManager.putConstraint(SpringLayout.EAST, component, 0, SpringLayout.EAST, this);
			}
			layoutManager.putConstraint(SpringLayout.NORTH, statusPane, 0, SpringLayout.NORTH, this);
			layoutManager.putConstraint(SpringLayout.SOUTH, messagePane, 0, SpringLayout.SOUTH, this);

			// align component edges against this panel and vice versa (usually on opposite side)
			layoutManager.putConstraint(SpringLayout.NORTH, recordSplitPane, 2, SpringLayout.SOUTH, statusPane);
			layoutManager.putConstraint(SpringLayout.SOUTH, recordSplitPane, -2, SpringLayout.NORTH, messagePane);
		}

		{
			final SpringLayout layoutManager = new SpringLayout();
			statusPane.setLayout(layoutManager);

			final Component servicePortLabel, forwardHostLabel, forwardPortLabel;
			statusPane.add(servicePortLabel = new JLabel("service port"));
			statusPane.add(this.servicePortField);
			statusPane.add(forwardHostLabel = new JLabel("forward host"));
			statusPane.add(this.forwardHostField);
			statusPane.add(forwardPortLabel = new JLabel("port"));
			statusPane.add(this.forwardPortField);
			statusPane.add(this.startButton);
			statusPane.add(this.stopButton);
			statusPane.add(this.clearButton);

			// same-faced alignment against parent
			for (final Component component : new Component[] { this.servicePortField, this.forwardHostField, this.forwardPortField }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, statusPane);
			}
			layoutManager.putConstraint(SpringLayout.WEST, servicePortLabel, 0, SpringLayout.WEST, statusPane);
			layoutManager.putConstraint(SpringLayout.SOUTH, statusPane, 0, SpringLayout.SOUTH, this.servicePortField);
			layoutManager.putConstraint(SpringLayout.EAST, statusPane, 0, SpringLayout.EAST, this.clearButton);

			// same-faced alignment against sibling
			for (final Component component : new Component[] { servicePortLabel, forwardHostLabel, forwardPortLabel, this.startButton, this.stopButton, this.clearButton }) {
				layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, component, 0, SpringLayout.VERTICAL_CENTER, this.servicePortField);
			}

			// opposite-faced alignment against sibling
			layoutManager.putConstraint(SpringLayout.WEST, this.servicePortField, 5, SpringLayout.EAST, servicePortLabel);
			layoutManager.putConstraint(SpringLayout.WEST, forwardHostLabel, 15, SpringLayout.EAST, this.servicePortField);
			layoutManager.putConstraint(SpringLayout.WEST, this.forwardHostField, 5, SpringLayout.EAST, forwardHostLabel);
			layoutManager.putConstraint(SpringLayout.WEST, forwardPortLabel, 5, SpringLayout.EAST, this.forwardHostField);
			layoutManager.putConstraint(SpringLayout.WEST, this.forwardPortField, 5, SpringLayout.EAST, forwardPortLabel);
			layoutManager.putConstraint(SpringLayout.WEST, this.startButton, 15, SpringLayout.EAST, this.forwardPortField);
			layoutManager.putConstraint(SpringLayout.WEST, this.stopButton, 5, SpringLayout.EAST, this.startButton);
			layoutManager.putConstraint(SpringLayout.WEST, this.clearButton, 5, SpringLayout.EAST, this.stopButton);
			layoutManager.putConstraint(SpringLayout.EAST, this.servicePortField, 60, SpringLayout.WEST, this.servicePortField);
			layoutManager.putConstraint(SpringLayout.EAST, this.forwardPortField, 60, SpringLayout.WEST, this.forwardPortField);
		}

		{
			final SpringLayout layoutManager = new SpringLayout();
			messagePane.setLayout(layoutManager);

			final Component messageLabel = new JLabel("Message");
			messageLabel.setForeground(Color.RED);
			messagePane.add(messageLabel);
			messagePane.add(this.messageField);

			// align component edges against this panel and vice versa (usually on same side)
			layoutManager.putConstraint(SpringLayout.NORTH, this.messageField, 0, SpringLayout.NORTH, messagePane);
			layoutManager.putConstraint(SpringLayout.SOUTH, messagePane, 0, SpringLayout.SOUTH, this.messageField);
			layoutManager.putConstraint(SpringLayout.WEST, messageLabel, 0, SpringLayout.WEST, messagePane);
			layoutManager.putConstraint(SpringLayout.EAST, this.messageField, 0, SpringLayout.EAST, messagePane);

			// align remaining component edges against each other (usually on opposite side)
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, messageLabel, 0, SpringLayout.VERTICAL_CENTER, this.messageField);
			layoutManager.putConstraint(SpringLayout.WEST, this.messageField, 5, SpringLayout.EAST, messageLabel);
		}

		recordTable.getSelectionModel().addListSelectionListener((ListSelectionEvent event) -> { if (!event.getValueIsAdjusting()) this.selectAction(recordTable.getSelectedRow()); });
		this.startButton.addActionListener((ActionEvent event) -> this.startAction());
		this.stopButton.addActionListener((ActionEvent event) -> this.stopAction());
		this.clearButton.addActionListener((ActionEvent event) -> this.clearAction());
	}


	/**
	 * Event handler for the clear button.
	 */
	private void clearAction () {
		this.setMessage(null);
		this.recordTableModel.removeRows();
		this.clearButton.setEnabled(false);
	}


	/**
	 * Closes and discards this pane's TCP monitor, and sets the activity state to inactive.
	 */
	public void disconnect () {
		try {
			this.monitor.close();
		} catch (final Exception exception) {}
		this.monitor = null;
		changeIconAndTooltip(this.startButton, ICON_START);
	}


	/**
	 * {@inheritDoc}
	 */
	public void exceptionCatched (final Exception exception) {
		if (this.startButton.getIcon() == ICON_SUSPEND) {
			this.setMessage(exception);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void recordCreated (final TcpMonitorRecord record) {
		if (this.startButton.getIcon() == ICON_SUSPEND) {
			this.recordTableModel.addRow(record);
			this.clearButton.setEnabled(true);
			this.setMessage(null);
		}
	}

	/**
	 * Event handler for the list selector.
	 * @param rowIndex the selected row index
	 */
	private void selectAction (final int rowIndex) {
		if (rowIndex == -1) {
			this.requestArea.setText("");
			this.responseArea.setText("");
		} else {
			final TcpMonitorRecord record = this.recordTableModel.getRow(rowIndex);
			final String requestText = new String(record.getRequestData(), Charset.forName("ASCII"));
			final String responseText = new String(record.getResponseData(), Charset.forName("ASCII"));

			this.requestArea.setText(requestText);
			this.requestArea.setCaretPosition(0);
			this.responseArea.setText(responseText);
			this.responseArea.setCaretPosition(0);
		}
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


	/**
	 * Event handler for the start button.
	 */
	private void startAction () {
		this.setMessage(null);
		if (this.monitor == null) {
			try {
				final int servicePort = Integer.parseInt(this.servicePortField.getText());
				final int forwardPort = Integer.parseInt(this.forwardPortField.getText());
				final String forwardHost = this.forwardHostField.getText();
				final InetSocketAddress forwardAddress = new InetSocketAddress(forwardHost, forwardPort);
				this.monitor = new TcpMonitor(servicePort, forwardAddress, this);
				this.stopButton.setEnabled(true);
			} catch (final Exception exception) {
				this.setMessage(exception);
				return;
			}
		}

		final boolean active = this.startButton.getIcon() == ICON_SUSPEND;
		changeIconAndTooltip(this.startButton, active ? ICON_RESUME : ICON_SUSPEND);
	}


	/**
	 * Event handler for the stop button.
	 */
	private void stopAction () {
		this.setMessage(null);
		this.disconnect();
		this.stopButton.setEnabled(false);
	}
}