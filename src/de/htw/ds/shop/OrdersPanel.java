package de.htw.ds.shop;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import de.sb.java.TypeMetadata;
import de.sb.java.swing.BeanTableModel;
import de.sb.java.swing.ScaleFormat;
import de.sb.java.swing.TableCellFormater;


/**
 * This class models a Swing based orders panel.
 */
@TypeMetadata(copyright = "2012-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class OrdersPanel extends JPanel {
	static private final long serialVersionUID = 1L;
	static private final String[] ORDER_COLUMN_NAMES = { "identity", "creationTimestamp", "taxRate", "itemCount", "grossPrice", "tax", "netPrice" };
	static private final String[] ORDER_HEADER_NAMES = { "ID", "Created", "Rate", "Items", "Gross", "Tax", "Net" };
	static private final String[] ITEM_COLUMN_NAMES = { "identity", "articleIdentity", "articleGrossPrice", "count" };
	static private final String[] ITEM_HEADER_NAMES = { "ID", "Article", "Gross", "Units" };
	static private final Format[] ORDER_COLUMN_FORMATS = {
			NumberFormat.getIntegerInstance(Locale.ROOT),
			DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM),
			NumberFormat.getPercentInstance(),
			NumberFormat.getIntegerInstance(Locale.ROOT),
			new ScaleFormat(NumberFormat.getCurrencyInstance(), -2),
			new ScaleFormat(NumberFormat.getCurrencyInstance(), -2),
			new ScaleFormat(NumberFormat.getCurrencyInstance(), -2)
	};
	static private final Format[] ITEM_COLUMN_FORMATS = {
			NumberFormat.getIntegerInstance(Locale.ROOT),
			NumberFormat.getIntegerInstance(Locale.ROOT),
			new ScaleFormat(NumberFormat.getCurrencyInstance(), -2),
			NumberFormat.getIntegerInstance(Locale.ROOT)
	};


	/**
	 * Creates a new column model for the item table.
	 * @param tableModel the table model
	 * @return the corresponding column model
	 */
	static private TableColumnModel createItemColumnModel (final BeanTableModel<OrderItem> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < ITEM_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(ITEM_COLUMN_NAMES[headerIndex]);
			if (headerIndex > 0) tableModel.setColumnEditable(modelIndex, true);

			final TableCellFormater formater = new TableCellFormater(ITEM_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex, 40);
			column.setHeaderValue(ITEM_HEADER_NAMES[headerIndex]);
			column.setCellRenderer(formater);
			column.setCellEditor(formater);
			result.addColumn(column);
		}

		return result;
	}


	/**
	 * Creates a new column model for the order table.
	 * @param tableModel the table model
	 * @return the corresponding column model
	 */
	static private TableColumnModel createOrderColumnModel (final BeanTableModel<Order> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < ORDER_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(ORDER_COLUMN_NAMES[headerIndex]);
			final int width = headerIndex == 1 ? 140 : 40;

			final TableCellFormater formater = new TableCellFormater(ORDER_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex, width);
			column.setHeaderValue(ORDER_HEADER_NAMES[headerIndex]);
			column.setCellRenderer(formater);
			column.setCellEditor(formater);
			result.addColumn(column);
		}

		return result;
	}


	/**
	 * Creates a new instance.
	 * @param shopPanel the shop panel
	 * @throws NullPointerException if the given shop panel is {@code null}
	 */
	public OrdersPanel (final ShopPanel shopPanel) {
		final Container northPane, southPane, orderScrollPane, itemScrollPane;
		final JTable orderTable, itemTable;
		final AbstractButton refreshButton, cancelButton, newButton, addButton, removeButton, orderButton;
		{
			this.setLayout(new GridLayout(1, 1));

			final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, northPane = new JPanel(), southPane = new JPanel());
			splitPane.setResizeWeight(0.5);
			this.add(splitPane);
		}

		{
			final SpringLayout layoutManager = new SpringLayout();
			northPane.setLayout(layoutManager);

			final BeanTableModel<Order> orderTableModel = new BeanTableModel<>(Order.class);
			orderTable = new JTable(orderTableModel, createOrderColumnModel(orderTableModel));
			orderTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			orderTable.setShowGrid(false);

			northPane.add(orderScrollPane = new JScrollPane(orderTable));
			northPane.add(refreshButton = new JButton("refresh"));
			northPane.add(cancelButton = new JButton("cancel"));
			northPane.add(newButton = new JButton("new..."));
			cancelButton.setEnabled(false);

			// same-faced alignment against parent
			for (final Component component : new Component[] { orderScrollPane, refreshButton }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, northPane);
			}
			for (final Component component : new Component[] { refreshButton, cancelButton, newButton }) {
				layoutManager.putConstraint(SpringLayout.SOUTH, component, 0, SpringLayout.SOUTH, northPane);
			}
			layoutManager.putConstraint(SpringLayout.EAST, orderScrollPane, 0, SpringLayout.EAST, northPane);
			layoutManager.putConstraint(SpringLayout.NORTH, orderScrollPane, 0, SpringLayout.NORTH, northPane);

			// opposite-faced alignment against sibling
			for (final Component component : new Component[] { refreshButton, cancelButton, newButton }) {
				layoutManager.putConstraint(SpringLayout.EAST, component, 80, SpringLayout.WEST, component);
			}
			layoutManager.putConstraint(SpringLayout.WEST, cancelButton, 5, SpringLayout.EAST, refreshButton);
			layoutManager.putConstraint(SpringLayout.WEST, newButton, 5, SpringLayout.EAST, cancelButton);
			layoutManager.putConstraint(SpringLayout.SOUTH, orderScrollPane, 0, SpringLayout.NORTH, refreshButton);
		}

		{
			final SpringLayout layoutManager = new SpringLayout();
			southPane.setLayout(layoutManager);

			final BeanTableModel<OrderItem> itemTableModel = new BeanTableModel<>(OrderItem.class);
			itemTable = new JTable(itemTableModel, createItemColumnModel(itemTableModel));
			itemTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			itemTable.setShowGrid(false);

			southPane.add(itemScrollPane = new JScrollPane(itemTable));
			southPane.add(addButton = new JButton("add"));
			southPane.add(removeButton = new JButton("remove"));
			southPane.add(orderButton = new JButton("order"));
			removeButton.setEnabled(false);

			// same-faced alignment against parent
			for (final Component component : new Component[] { itemScrollPane, addButton }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, southPane);
			}
			for (final Component component : new Component[] { addButton, removeButton, orderButton }) {
				layoutManager.putConstraint(SpringLayout.SOUTH, component, 0, SpringLayout.SOUTH, southPane);
			}
			layoutManager.putConstraint(SpringLayout.EAST, itemScrollPane, 0, SpringLayout.EAST, southPane);
			layoutManager.putConstraint(SpringLayout.NORTH, itemScrollPane, 0, SpringLayout.NORTH, southPane);

			// opposite-faced alignment against sibling
			for (final Component component : new Component[] { addButton, removeButton, orderButton }) {
				layoutManager.putConstraint(SpringLayout.EAST, component, 80, SpringLayout.WEST, component);
			}
			layoutManager.putConstraint(SpringLayout.WEST, removeButton, 5, SpringLayout.EAST, refreshButton);
			layoutManager.putConstraint(SpringLayout.WEST, orderButton, 5, SpringLayout.EAST, removeButton);
			layoutManager.putConstraint(SpringLayout.SOUTH, itemScrollPane, 0, SpringLayout.NORTH, addButton);

			//new SpringSpread(southPane, 0, 0).alignWest(itemScrollPane);
			//new SpringSpread(addButton, 0, -2).alignSouth(itemScrollPane);
			//new SpringSpread(southPane, 2, null).alignSouth(addButton, removeButton, orderButton);
			//new SpringSpread(southPane, 0, 80).alignWest(addButton);
			//new SpringSpread(addButton, 5, 80).alignWest(removeButton);
			//new SpringSpread(removeButton, 5, 80).alignWest(orderButton);
		}

		final ListSelectionListener selectionAction1 = new ListSelectionListener() {
			public void valueChanged (final ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				shopPanel.setMessage(null);

				final int rowIndex = orderTable.getSelectedRow();
				cancelButton.setEnabled(rowIndex != -1);

				@SuppressWarnings("unchecked")
				final BeanTableModel<OrderItem> itemTableModel = (BeanTableModel<OrderItem>) itemTable.getModel();
				if (rowIndex == -1) {
					itemTableModel.removeRows();
				} else {
					@SuppressWarnings("unchecked")
					final BeanTableModel<Order> orderTableModel = (BeanTableModel<Order>) orderTable.getModel();
					final Order order = orderTableModel.getRow(rowIndex);
					itemTableModel.removeRows();
					itemTableModel.addRows(order.getItems().toArray(new OrderItem[0]));
				}
			}
		};
		orderTable.getSelectionModel().addListSelectionListener(selectionAction1);

		final ListSelectionListener selectionAction2 = new ListSelectionListener() {
			public void valueChanged (final ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				shopPanel.setMessage(null);

				removeButton.setEnabled(itemTable.getSelectedRow() != -1);
			}
		};
		itemTable.getSelectionModel().addListSelectionListener(selectionAction2);

		final ActionListener refreshAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				try {
					shopPanel.setMessage(null);
					final String alias = (String) shopPanel.getSessionMap().get("alias");
					final String password = (String) shopPanel.getSessionMap().get("password");
					final Order[] orders = shopPanel.getServiceProxy().queryOrders(alias, password).toArray(new Order[0]);

					@SuppressWarnings("unchecked")
					final BeanTableModel<Order> orderTableModel = (BeanTableModel<Order>) orderTable.getModel();
					orderTableModel.removeRows();
					orderTableModel.addRows(orders);
				} catch (final Exception exception) {
					shopPanel.setMessage(exception);
				}
			}
		};
		refreshButton.addActionListener(refreshAction);

		final ActionListener cancelAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				try {
					shopPanel.setMessage(null);

					final int rowIndex = orderTable.getSelectedRow();
					if (rowIndex != -1) {
						@SuppressWarnings("unchecked")
						final BeanTableModel<Order> orderTableModel = (BeanTableModel<Order>) orderTable.getModel();
						final Order order = orderTableModel.getRow(rowIndex);
						final String alias = (String) shopPanel.getSessionMap().get("alias");
						final String password = (String) shopPanel.getSessionMap().get("password");
						shopPanel.getServiceProxy().cancelOrder(alias, password, order.getIdentity());

						orderTableModel.removeRow(rowIndex);
					}
				} catch (final Exception exception) {
					shopPanel.setMessage(exception);
				}
			}
		};
		cancelButton.addActionListener(cancelAction);

		final ActionListener newAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				shopPanel.setMessage(null);

				orderTable.getSelectionModel().clearSelection();
				((BeanTableModel<?>) itemTable.getModel()).removeRows();
				addButton.doClick();
			}
		};
		newButton.addActionListener(newAction);

		final ActionListener addAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				shopPanel.setMessage(null);

				@SuppressWarnings("unchecked")
				final BeanTableModel<OrderItem> itemTableModel = (BeanTableModel<OrderItem>) itemTable.getModel();
				itemTableModel.addRow(new OrderItem());
			}
		};
		addButton.addActionListener(addAction);

		final ActionListener removeAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				shopPanel.setMessage(null);

				@SuppressWarnings("unchecked")
				final BeanTableModel<OrderItem> itemTableModel = (BeanTableModel<OrderItem>) itemTable.getModel();
				itemTableModel.removeRow(itemTable.getSelectedRow());
			}
		};
		removeButton.addActionListener(removeAction);

		final ActionListener orderAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				try {
					shopPanel.setMessage(null);

					@SuppressWarnings("unchecked")
					final BeanTableModel<OrderItem> itemTableModel = (BeanTableModel<OrderItem>) itemTable.getModel();
					final OrderItem[] items = itemTableModel.getRows();
					final String alias = (String) shopPanel.getSessionMap().get("alias");
					final String password = (String) shopPanel.getSessionMap().get("password");
					shopPanel.getServiceProxy().createOrder(alias, password, Arrays.asList(items));

					itemTableModel.removeRows();
					refreshButton.doClick();
				} catch (final Exception exception) {
					shopPanel.setMessage(exception);
				}
			}
		};
		orderButton.addActionListener(orderAction);
	}
}