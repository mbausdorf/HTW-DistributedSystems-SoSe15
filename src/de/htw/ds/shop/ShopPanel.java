package de.htw.ds.shop;

import java.awt.Color;
import java.awt.Component;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import de.sb.java.Threads;
import de.sb.java.TypeMetadata;


/**
 * This class models a Swing based multi-lab GUI shop panel.
 */
@TypeMetadata(copyright = "2012-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class ShopPanel extends JPanel {
	static private final long serialVersionUID = 1L;

	private final ShopService serviceProxy;
	private final Map<String,Object> sessionMap;
	private final JTextComponent messageField;


	/**
	 * Creates a new instance.
	 * @param serviceProxy the shop service proxy
	 * @throws NullPointerException if the given proxy is {@code null}
	 */
	public ShopPanel (final ShopService serviceProxy) {
		if (serviceProxy == null) throw new NullPointerException();

		this.serviceProxy = serviceProxy;
		this.sessionMap = Collections.synchronizedMap(new HashMap<String,Object>());
		final SpringLayout layoutManager = new SpringLayout();
		this.setLayout(layoutManager);

		final Component messageLabel;
		final JTabbedPane tabbedPane;
		{
			this.add(tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT));
			this.add(messageLabel = new JLabel("Message"));
			this.add(this.messageField = new JTextField());

			tabbedPane.addTab("Articles", this.createTabPane(0));
			tabbedPane.addTab("Customer", null);
			tabbedPane.addTab("Orders", null);
			messageLabel.setForeground(Color.RED);
			this.messageField.setEditable(false);
		}

		// same-faced alignment against parent
		for (final Component component : new Component[] { tabbedPane }) {
			layoutManager.putConstraint(SpringLayout.NORTH, component, 0, SpringLayout.NORTH, this);
		}
		for (final Component component : new Component[] { tabbedPane, messageLabel }) {
			layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, this);
		}
		for (final Component component : new Component[] { tabbedPane, this.messageField }) {
			layoutManager.putConstraint(SpringLayout.EAST, component, 0, SpringLayout.EAST, this);
		}
		layoutManager.putConstraint(SpringLayout.SOUTH, this.messageField, 0, SpringLayout.SOUTH, this);

		// same-faced alignment against sibling
		layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, messageLabel, 0, SpringLayout.VERTICAL_CENTER, this.messageField);

		// opposite-faced alignment against sibling
		layoutManager.putConstraint(SpringLayout.WEST, this.messageField, 0, SpringLayout.EAST, messageLabel);
		layoutManager.putConstraint(SpringLayout.SOUTH, tabbedPane, 0, SpringLayout.NORTH, this.messageField);

		// lazy initialize pane tabs 1-n in order to spread panel creation cost
		final ChangeListener selectionAction = new ChangeListener() {
			public void stateChanged (final ChangeEvent event) {
				ShopPanel.this.setMessage(null);

				final int selectionIndex = tabbedPane.getSelectedIndex();
				if (selectionIndex >= 0 && tabbedPane.getComponentAt(selectionIndex) == null) {
					tabbedPane.setComponentAt(selectionIndex, ShopPanel.this.createTabPane(selectionIndex));
				}
			}
		};
		tabbedPane.addChangeListener(selectionAction);
	}


	/**
	 * Creates a pane for the given tab index. Note that this allows lazy initialization of the pane
	 * tabs in order to spread the creation cost.
	 * @param tabIndex the tab index
	 * @return the tab pane
	 */
	private JPanel createTabPane (final int tabIndex) {
		switch (tabIndex) {
			case 0:
				return new ArticlesPanel(this);
			case 1:
				return new CustomerPanel(this);
			case 2:
				return new OrdersPanel(this);
			default:
				throw new AssertionError();
		}
	}


	/**
	 * Returns the shop service proxy.
	 * @return the service proxy
	 */
	public ShopService getServiceProxy () {
		return this.serviceProxy;
	}


	/**
	 * Returns the session map.
	 * @return the session map
	 */
	public Map<String,Object> getSessionMap () {
		return this.sessionMap;
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