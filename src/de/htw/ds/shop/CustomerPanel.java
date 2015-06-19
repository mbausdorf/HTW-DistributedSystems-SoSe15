package de.htw.ds.shop;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;
import de.sb.java.TypeMetadata;


/**
 * This class models a Swing based customer panel. Note that this panel maintains the login data
 * required for shop order interactions.
 */
@TypeMetadata(copyright = "2012-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class CustomerPanel extends JPanel {
	static private final long serialVersionUID = 1L;


	/**
	 * Creates a new instance.
	 * @param shopPanel the shop panel
	 * @throws NullPointerException if the given shop panel is {@code null}
	 */
	public CustomerPanel (final ShopPanel shopPanel) {
		if (shopPanel == null) throw new NullPointerException();

		final Container westPane = new JPanel(), eastPane = new JPanel();
		final JTextComponent identityField, aliasField, passwordField, givenNameField, familyNameField, streetField, postcodeField, cityField, emailField, phoneField;
		final AbstractButton logonButton, createButton, deleteButton;
		{
			final SpringLayout layoutManager = new SpringLayout();
			this.setLayout(layoutManager);

			final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, westPane, eastPane);
			splitPane.setResizeWeight(0.5);
			splitPane.setBorder(new LineBorder(Color.LIGHT_GRAY));
			((JComponent) splitPane.getTopComponent()).setBorder(new EmptyBorder(0, 2, 0, 0));
			((JComponent) splitPane.getBottomComponent()).setBorder(new EmptyBorder(0, 2, 0, 0));

			this.add(splitPane);
			this.add(logonButton = new JButton("logon"));
			this.add(createButton = new JButton("create"));
			this.add(deleteButton = new JButton("delete"));

			// same-faced alignment against parent
			for (final Component component : new Component[] { splitPane, logonButton }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, this);
			}
			layoutManager.putConstraint(SpringLayout.NORTH, splitPane, 0, SpringLayout.NORTH, this);
			layoutManager.putConstraint(SpringLayout.EAST, splitPane, 0, SpringLayout.EAST, this);

			// opposite-faced alignment against sibling
			for (final Component component : new Component[] { logonButton, createButton, deleteButton }) {
				layoutManager.putConstraint(SpringLayout.NORTH, component, 2, SpringLayout.SOUTH, splitPane);
			}
			for (final Component component : new Component[] { logonButton, createButton, deleteButton }) {
				layoutManager.putConstraint(SpringLayout.EAST, component, 80, SpringLayout.WEST, component);
			}
			layoutManager.putConstraint(SpringLayout.WEST, createButton, 5, SpringLayout.EAST, logonButton);
			layoutManager.putConstraint(SpringLayout.WEST, deleteButton, 5, SpringLayout.EAST, createButton);
		}

		{ // fill left pane area
			final Component identityLabel, aliasLabel, passwordLabel, firstNameLabel, lastNameLabel;
			final SpringLayout layoutManager = new SpringLayout();
			westPane.setLayout(layoutManager);

			westPane.add(identityLabel = new JLabel("Identity"));
			westPane.add(identityField = new JTextField());
			westPane.add(aliasLabel = new JLabel("Alias"));
			westPane.add(aliasField = new JTextField());
			westPane.add(passwordLabel = new JLabel("Password"));
			westPane.add(passwordField = new JPasswordField());
			westPane.add(firstNameLabel = new JLabel("Given Name"));
			westPane.add(givenNameField = new JTextField());
			westPane.add(lastNameLabel = new JLabel("Family Name"));
			westPane.add(familyNameField = new JTextField());
			identityField.setEditable(false);

			// same-faced alignment against parent
			for (final Component component : new Component[] { identityLabel, aliasLabel, passwordLabel, firstNameLabel, lastNameLabel }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, westPane);
			}
			for (final Component component : new Component[] { identityField, aliasField, passwordField, givenNameField, familyNameField }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 80, SpringLayout.WEST, westPane);
			}
			for (final Component component : new Component[] { identityField, aliasField, passwordField, givenNameField, familyNameField }) {
				layoutManager.putConstraint(SpringLayout.EAST, component, 0, SpringLayout.EAST, westPane);
			}
			layoutManager.putConstraint(SpringLayout.NORTH, identityField, 0, SpringLayout.NORTH, westPane);
			layoutManager.putConstraint(SpringLayout.SOUTH, westPane, 0, SpringLayout.SOUTH, familyNameField);

			// same-faced alignment against sibling
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, identityLabel, 0, SpringLayout.VERTICAL_CENTER, identityField);
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, aliasLabel, 0, SpringLayout.VERTICAL_CENTER, aliasField);
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, passwordLabel, 0, SpringLayout.VERTICAL_CENTER, passwordField);
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, firstNameLabel, 0, SpringLayout.VERTICAL_CENTER, givenNameField);
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, lastNameLabel, 0, SpringLayout.VERTICAL_CENTER, familyNameField);

			// opposite-faced alignment against sibling
			layoutManager.putConstraint(SpringLayout.NORTH, aliasField, 0, SpringLayout.SOUTH, identityField);
			layoutManager.putConstraint(SpringLayout.NORTH, passwordField, 0, SpringLayout.SOUTH, aliasField);
			layoutManager.putConstraint(SpringLayout.NORTH, givenNameField, 0, SpringLayout.SOUTH, passwordField);
			layoutManager.putConstraint(SpringLayout.NORTH, familyNameField, 0, SpringLayout.SOUTH, givenNameField);
		}

		{ // fill right pane area
			final Component streetLabel, postcodeLabel, cityLabel, emailLabel, phoneLabel;
			final SpringLayout layoutManager = new SpringLayout();
			eastPane.setLayout(layoutManager);

			eastPane.add(streetLabel = new JLabel("Street"));
			eastPane.add(streetField = new JTextField());
			eastPane.add(postcodeLabel = new JLabel("Postcode"));
			eastPane.add(postcodeField = new JTextField());
			eastPane.add(cityLabel = new JLabel("City"));
			eastPane.add(cityField = new JTextField());
			eastPane.add(emailLabel = new JLabel("Email"));
			eastPane.add(emailField = new JTextField());
			eastPane.add(phoneLabel = new JLabel("Phone"));
			eastPane.add(phoneField = new JTextField());

			// same-faced alignment against parent
			for (final Component component : new Component[] { streetLabel, postcodeLabel, emailLabel, phoneLabel }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, eastPane);
			}
			for (final Component component : new Component[] { streetField, postcodeField, emailField, phoneField }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 80, SpringLayout.WEST, eastPane);
			}
			for (final Component component : new Component[] { streetField, cityField, emailField, phoneField }) {
				layoutManager.putConstraint(SpringLayout.EAST, component, 0, SpringLayout.EAST, eastPane);
			}
			layoutManager.putConstraint(SpringLayout.NORTH, streetField, 0, SpringLayout.NORTH, eastPane);

			// same-faced alignment against sibling
			for (final Component component : new Component[] { postcodeLabel, cityLabel }) {
				layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, component, 0, SpringLayout.VERTICAL_CENTER, postcodeField);
			}
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, phoneLabel, 0, SpringLayout.VERTICAL_CENTER, phoneField);
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, emailLabel, 0, SpringLayout.VERTICAL_CENTER, emailField);
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, streetLabel, 0, SpringLayout.VERTICAL_CENTER, streetField);

			// opposite-faced alignment against sibling
			for (final Component component : new Component[] { postcodeField, cityField }) {
				layoutManager.putConstraint(SpringLayout.NORTH, component, 0, SpringLayout.SOUTH, streetField);
			}
			layoutManager.putConstraint(SpringLayout.NORTH, emailField, 0, SpringLayout.SOUTH, postcodeField);
			layoutManager.putConstraint(SpringLayout.NORTH, phoneField, 0, SpringLayout.SOUTH, emailField);
			layoutManager.putConstraint(SpringLayout.WEST, cityLabel, 5, SpringLayout.EAST, postcodeField);
			layoutManager.putConstraint(SpringLayout.WEST, cityField, 5, SpringLayout.EAST, cityLabel);
			layoutManager.putConstraint(SpringLayout.EAST, postcodeField, 80, SpringLayout.WEST, postcodeField);
		}

		final ActionListener logonAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				try {
					shopPanel.setMessage(null);

					final String password = passwordField.getText();
					final Customer customer = shopPanel.getServiceProxy().queryCustomer(aliasField.getText(), password);
					identityField.setText(Long.toString(customer.getIdentity()));
					givenNameField.setText(customer.getGivenName());
					familyNameField.setText(customer.getFamilyName());
					streetField.setText(customer.getStreet());
					postcodeField.setText(customer.getPostcode());
					cityField.setText(customer.getCity());
					emailField.setText(customer.getEmail());
					phoneField.setText(customer.getPhone());

					shopPanel.getSessionMap().put("alias", customer.getAlias());
					shopPanel.getSessionMap().put("password", password);
				} catch (final Exception exception) {
					shopPanel.setMessage(exception);
					shopPanel.getSessionMap().clear();
				}
			}
		};
		logonButton.addActionListener(logonAction);

		final ActionListener createAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				try {
					shopPanel.setMessage(null);

					final String password = passwordField.getText();
					final Customer customer = new Customer();
					customer.setAlias(aliasField.getText());
					customer.setGivenName(givenNameField.getText());
					customer.setFamilyName(familyNameField.getText());
					customer.setStreet(streetField.getText());
					customer.setPostcode(postcodeField.getText());
					customer.setCity(cityField.getText());
					customer.setEmail(emailField.getText());
					customer.setPhone(phoneField.getText());

					final long customerIdentity = shopPanel.getServiceProxy().registerCustomer(customer, password);
					identityField.setText(Long.toString(customerIdentity));
					shopPanel.getSessionMap().put("alias", customer.getAlias());
					shopPanel.getSessionMap().put("password", password);
				} catch (final Exception exception) {
					shopPanel.setMessage(exception);
					shopPanel.getSessionMap().clear();
				}
			}
		};
		createButton.addActionListener(createAction);

		final ActionListener deleteAction = new ActionListener() {
			public void actionPerformed (final ActionEvent event) {
				try {
					shopPanel.setMessage(null);

					shopPanel.getServiceProxy().unregisterCustomer(aliasField.getText(), passwordField.getText());
					shopPanel.getSessionMap().clear();
					for (final JTextComponent field : new JTextComponent[] { identityField, aliasField, passwordField, givenNameField, familyNameField, streetField, postcodeField, cityField, emailField, phoneField }) {
						field.setText("");
					}
				} catch (final Exception exception) {
					shopPanel.setMessage(exception);
				}
			}
		};
		deleteButton.addActionListener(deleteAction);
	}
}