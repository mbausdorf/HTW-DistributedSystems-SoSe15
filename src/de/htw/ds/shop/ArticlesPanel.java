package de.htw.ds.shop;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import de.sb.java.TypeMetadata;
import de.sb.java.swing.BeanTableModel;
import de.sb.java.swing.ScaleFormat;
import de.sb.java.swing.TableCellFormater;


/**
 * This class models a Swing based articles panel.
*/
@TypeMetadata(copyright = "2012-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class ArticlesPanel extends JPanel {
	static private final long serialVersionUID = 1L;
	static private final String[] ARTICLE_COLUMN_NAMES = { "identity", "description", "price", "count" };
	static private final String[] ARTICLE_HEADER_NAMES = { "ID", "Description", "Gross", "Available" };
	static private final Format[] ARTICLE_COLUMN_FORMATS = {
			NumberFormat.getIntegerInstance(Locale.ROOT),
			null,
			new ScaleFormat(NumberFormat.getCurrencyInstance(), -2),
			NumberFormat.getIntegerInstance(Locale.ROOT)
	};


	/**
	 * Creates a new column model for the article table.
	 * @param tableModel the table model
	 * @return the corresponding column model
	 */
	static private TableColumnModel createArticleColumnModel (final BeanTableModel<Article> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < ARTICLE_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(ARTICLE_COLUMN_NAMES[headerIndex]);
			final int width = headerIndex == 1 ? 300 : 40;

			final TableCellFormater formater = new TableCellFormater(ARTICLE_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex, width);
			column.setHeaderValue(ARTICLE_HEADER_NAMES[headerIndex]);
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
	public ArticlesPanel (final ShopPanel shopPanel) {
		if (shopPanel == null) throw new NullPointerException();

		final SpringLayout layoutManager = new SpringLayout();
		this.setLayout(layoutManager);

		final Component articleScrollPane;
		final JTable articleTable;
		final AbstractButton refreshButton;
		{
			final BeanTableModel<Article> articleTableModel = new BeanTableModel<>(Article.class);
			articleTable = new JTable(articleTableModel, createArticleColumnModel(articleTableModel));
			articleTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			articleTable.setShowGrid(false);

			this.add(articleScrollPane = new JScrollPane(articleTable));
			this.add(refreshButton = new JButton("refresh"));

			// same-faced alignment against parent
			for (final Component component : new Component[] { articleScrollPane, refreshButton }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, this);
			}
			layoutManager.putConstraint(SpringLayout.NORTH, articleScrollPane, 0, SpringLayout.NORTH, this);
			layoutManager.putConstraint(SpringLayout.EAST, articleScrollPane, 0, SpringLayout.EAST, this);
			layoutManager.putConstraint(SpringLayout.SOUTH, refreshButton, 0, SpringLayout.SOUTH, this);

			// opposite-faced alignment against sibling
			layoutManager.putConstraint(SpringLayout.SOUTH, articleScrollPane, -2, SpringLayout.NORTH, refreshButton);
		}

		final ActionListener refreshAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				try {
					shopPanel.setMessage(null);

					@SuppressWarnings("unchecked")
					final BeanTableModel<Article> articleTableModel = (BeanTableModel<Article>) articleTable.getModel();
					final Article[] articles = shopPanel.getServiceProxy().queryArticles().toArray(new Article[0]);
					articleTableModel.removeRows();
					articleTableModel.addRows(articles);
				} catch (final Exception exception) {
					shopPanel.setMessage(exception);
				}
			}
		};
		refreshButton.addActionListener(refreshAction);
	}
}