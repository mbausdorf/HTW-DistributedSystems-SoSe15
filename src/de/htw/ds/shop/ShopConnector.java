package de.htw.ds.shop;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import de.sb.java.TypeMetadata;
import de.sb.java.sql.JdbcConnectionMonitor;


/**
 * Shop connector class abstracting the use of JDBC for a set of given operations. Note that a
 * connector always works on the same JDBC connection, therefore allowing multiple method calls to
 * operate within a single local transaction, if desired.
 */
@TypeMetadata(copyright = "2010-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class ShopConnector implements AutoCloseable {
	static private final String SQL_SELECT_CUSTOMER = "SELECT * FROM soap_shop.Customer WHERE alias=? AND passwordHash=?";
	static private final String SQL_SELECT_ARTICLE = "SELECT * FROM soap_shop.Article WHERE identity=?";
	static private final String SQL_SELECT_ARTICLES = "SELECT * FROM soap_shop.Article";
	static private final String SQL_SELECT_PURCHASES = "SELECT * FROM soap_shop.Purchase WHERE customerIdentity=?";
	static private final String SQL_SELECT_PURCHASE = "SELECT * FROM soap_shop.Purchase WHERE identity=?";
	static private final String SQL_SELECT_PURCHASE_ITEMS = "SELECT * FROM soap_shop.PurchaseItem WHERE purchaseIdentity=?";
	static private final String SQL_INSERT_CUSTOMER = "INSERT INTO soap_shop.Customer VALUES (0, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	static private final String SQL_INSERT_PURCHASE = "INSERT INTO soap_shop.Purchase VALUES (0, ?, ?, ?)";
	static private final String SQL_INSERT_PURCHASE_ITEM = "INSERT INTO soap_shop.PurchaseItem VALUES (0, ?, ?, ?, ?)";
	static private final String SQL_RESERVE_ARTICLE_UNITS = "UPDATE soap_shop.Article set count=count-? WHERE identity=? AND count>=?";
	static private final String SQL_RELEASE_ARTICLE_UNITS = "UPDATE soap_shop.Article set count=count+? WHERE identity=?";
	static private final String SQL_DELETE_CUSTOMER = "DELETE FROM soap_shop.Customer WHERE identity=?";
	static private final String SQL_DELETE_PURCHASE = "DELETE FROM soap_shop.Purchase WHERE identity=?";
	static private final DataSource DATA_SOURCE;

	static {
		try {
			final Properties properties = new Properties();
			try (InputStream byteSource = ShopConnector.class.getResourceAsStream("shop-mysql.properties")) {
				properties.load(byteSource);
			}

			final Class<?> dataSourceClass = Class.forName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource", true, Thread.currentThread().getContextClassLoader());
			final DataSource dataSource = (DataSource) dataSourceClass.newInstance();
			dataSourceClass.getMethod("setURL", String.class).invoke(dataSource, properties.get("connectionUrl"));
			dataSourceClass.getMethod("setCharacterEncoding", String.class).invoke(dataSource, properties.get("characterEncoding"));
			dataSourceClass.getMethod("setUser", String.class).invoke(dataSource, properties.get("alias"));
			dataSourceClass.getMethod("setPassword", String.class).invoke(dataSource, properties.get("password"));
			DATA_SOURCE = dataSource;
		} catch (final Exception exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private final Connection connection;


	/**
	 * Creates a new instance.
	 * @throws SQLException if there is an underlying JDBC problem
	 */
	public ShopConnector () throws SQLException {
		this.connection = DATA_SOURCE.getConnection();

		final Runnable connectionMonitor = new JdbcConnectionMonitor(this.connection, "select null", 60000);
		final Thread thread = new Thread(connectionMonitor, "jdbc-connection-monitor");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Closes the underlying JDBC connection.
	 */
	public void close () throws SQLException {
		this.connection.close();
	}


	/**
	 * Unregisters a customer that has no orders.
	 * @param alias the customer alias
	 * @param passwordHash the customer password-hash
	 * @return the customer identity
	 * @throws IllegalStateException if the login data is invalid, or the customer has any orders
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	public long deleteCustomer (final String alias, final byte[] passwordHash) throws SQLException {
		final Customer customer = this.queryCustomer(alias, passwordHash);
		final Set<Order> purchases = this.queryOrders(alias, passwordHash);
		if (!purchases.isEmpty()) throw new IllegalStateException("customer has orders.");

		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_DELETE_CUSTOMER)) {
				statement.setLong(1, customer.getIdentity());
				if (statement.executeUpdate() != 1) throw new IllegalStateException("customer removal failed.");
			}
		}

		return customer.getIdentity();
	}


	/**
	 * Cancels an order. Note that cancel requests for orders will be rejected if they are older
	 * than one hour, or don't target the given customer.
	 * @param alias the customer alias
	 * @param passwordHash the customer password-hash
	 * @param orderIdentity the order identity
	 * @throws IllegalStateException if the login data is invalid, if the order is too old, or if it
	 *         is not targeting the given customer
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	public void deleteOrder (final String alias, final byte[] passwordHash, final long orderIdentity) throws SQLException {
		final Customer customer = this.queryCustomer(alias, passwordHash);
		final Order order = this.queryOrder(alias, passwordHash, orderIdentity);
		if (order.getCustomerIdentity() != customer.getIdentity()) throw new IllegalStateException("purchase not created by given customer.");
		if (System.currentTimeMillis() > order.getCreationTimestamp() + TimeUnit.HOURS.toMillis(1)) throw new IllegalStateException("purchase too old.");

		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_DELETE_PURCHASE)) {
				statement.setLong(1, orderIdentity);
				statement.executeUpdate();
			}
		}

		for (final OrderItem item : order.getItems()) {
			synchronized (this.connection) {
				try (PreparedStatement statement = this.connection.prepareStatement(SQL_RELEASE_ARTICLE_UNITS)) {
					statement.setLong(1, item.getCount());
					statement.setLong(2, item.getArticleIdentity());
					statement.executeUpdate();
				}
			}
		}
	}


	/**
	 * Returns the JDBC connection to allow external synchronization and transaction handling.
	 * @return the JDBC connection
	 */
	public Connection getConnection () {
		return this.connection;
	}


	/**
	 * Registers a new customer and returns it's identity. The given customer's identity is ignored
	 * during processing.
	 * @param alias the customer alias
	 * @param passwordHash the customer password-hash
	 * @param firstName the first name
	 * @param lastName the last name
	 * @param street the address street
	 * @param postcode the address post code
	 * @param city the city
	 * @param email the eMail address
	 * @param phone the phone number
	 * @return the customer identity
	 * @throws NullPointerException if one of the given values is {@code null}
	 * @throws IllegalArgumentException if the given alias or password are shorter than four digits
	 * @throws IllegalStateException if the insert is unsuccessful
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	public long insertCustomer (final String alias, final byte[] passwordHash, final String firstName, final String lastName, final String street, final String postcode, final String city, final String email, final String phone) throws SQLException {
		if (alias.length() < 4 | passwordHash.length != 32) throw new IllegalArgumentException("alias or password too short.");

		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_INSERT_CUSTOMER, Statement.RETURN_GENERATED_KEYS)) {
				statement.setString(1, alias);
				statement.setBytes(2, passwordHash);
				statement.setString(3, firstName);
				statement.setString(4, lastName);
				statement.setString(5, street);
				statement.setString(6, postcode);
				statement.setString(7, city);
				statement.setString(8, email);
				statement.setString(9, phone);
				if (statement.executeUpdate() != 1) throw new IllegalStateException("customer creation failed.");

				try (ResultSet resultSet = statement.getGeneratedKeys()) {
					if (!resultSet.next()) throw new IllegalStateException("customer key generation failed.");
					final long customerIdentity = resultSet.getLong(1);
					return customerIdentity;
				}
			}
		}
	}


	/**
	 * Creates an order from the given items. Note that the suggested price for each item must be
	 * equal to or exceed the current article price. Also, note that orders which exhaust the
	 * available article capacity are rejected.
	 * @param alias the customer alias
	 * @param passwordHash the customer password-hash
	 * @param items the items
	 * @return the order identity
	 * @throws NullPointerException if one of the given values is {@code null}
	 * @throws IllegalArgumentException if items is empty, or if any of the given items is priced
	 *         too low
	 * @throws IllegalStateException if the login data is invalid, or the order creation fails
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	public long insertOrder (final String alias, final byte[] passwordHash, final double taxRate, final Collection<OrderItem> items) throws SQLException {
		final long purchaseIdentity;
		final Customer customer = this.queryCustomer(alias, passwordHash);

		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_INSERT_PURCHASE, Statement.RETURN_GENERATED_KEYS)) {
				statement.setLong(1, customer.getIdentity());
				statement.setLong(2, System.currentTimeMillis());
				statement.setDouble(3, taxRate);
				if (statement.executeUpdate() != 1) throw new IllegalStateException("purchase creation failed.");

				try (ResultSet resultSet = statement.getGeneratedKeys()) {
					if (!resultSet.next()) throw new IllegalStateException("purchase key generation failed.");
					purchaseIdentity = resultSet.getLong(1);
				}
			}
		}

		if (items.isEmpty()) throw new IllegalArgumentException("missing items.");
		for (final OrderItem item : items) {
			if (item.getCount() <= 0) throw new IllegalArgumentException("item count too low.");
			final Article article = this.queryArticle(item.getArticleIdentity());
			if (article.getPrice() > item.getArticleGrossPrice()) throw new IllegalArgumentException("price offer too low.");

			synchronized (this.connection) {
				try (PreparedStatement statement = this.connection.prepareStatement(SQL_RESERVE_ARTICLE_UNITS)) {
					statement.setLong(1, item.getCount());
					statement.setLong(2, article.getIdentity());
					statement.setLong(3, item.getCount());
					if (statement.executeUpdate() != 1) throw new IllegalStateException("too few article units on stock.");
				}
			}

			synchronized (this.connection) {
				try (PreparedStatement statement = this.connection.prepareStatement(SQL_INSERT_PURCHASE_ITEM)) {
					statement.setLong(1, purchaseIdentity);
					statement.setLong(2, item.getArticleIdentity());
					statement.setLong(3, item.getArticleGrossPrice());
					statement.setInt(4, item.getCount());
					if (statement.executeUpdate() != 1) throw new IllegalStateException("purchase item creation failed.");
				}
			}
		}

		return purchaseIdentity;
	}


	/**
	 * Queries the items for a given order.
	 * @param order the order to be filled with it's items
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	private void populateOrderItems (final Order order) throws SQLException {
		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_PURCHASE_ITEMS)) {
				statement.setLong(1, order.getIdentity());

				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						final OrderItem item = new OrderItem();
						item.setIdentity(resultSet.getLong("identity"));
						item.setArticleIdentity(resultSet.getLong("articleIdentity"));
						item.setArticleGrossPrice(resultSet.getLong("articlePrice"));
						item.setCount(resultSet.getInt("count"));
						order.getItems().add(item);
					}
				}
			}
		}
	}


	/**
	 * Returns the article data for the given identity.
	 * @param articleIdentity the article identity
	 * @throws IllegalStateException if there is no article with the given identity
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	public Article queryArticle (final long articleIdentity) throws SQLException {
		final Article article = new Article();

		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_ARTICLE)) {
				statement.setLong(1, articleIdentity);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (!resultSet.next()) throw new IllegalStateException("article doesn't exist.");
					article.setIdentity(resultSet.getLong("identity"));
					article.setDescription(resultSet.getString("description"));
					article.setCount(resultSet.getInt("count"));
					article.setPrice(resultSet.getLong("price"));
				}
			}
		}

		return article;
	}


	/**
	 * Returns all article data.
	 * @throws IllegalStateException if the login data is invalid
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	public SortedSet<Article> queryArticles () throws SQLException {
		final SortedSet<Article> articles = new TreeSet<Article>();

		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_ARTICLES)) {
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						final Article article = new Article();
						article.setIdentity(resultSet.getLong("identity"));
						article.setDescription(resultSet.getString("description"));
						article.setCount(resultSet.getInt("count"));
						article.setPrice(resultSet.getLong("price"));
						articles.add(article);
					}
				}
			}
		}

		return articles;
	}


	/**
	 * Returns the customer data.
	 * @param alias the customer alias
	 * @param passwordHash the customer password-hash
	 * @return the customer
	 * @throws IllegalStateException if the login data is invalid
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	public Customer queryCustomer (final String alias, final byte[] passwordHash) throws SQLException {
		final Customer customer = new Customer();

		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_CUSTOMER)) {
				statement.setString(1, alias);
				statement.setBytes(2, passwordHash);

				try (ResultSet resultSet = statement.executeQuery()) {
					if (!resultSet.next()) throw new IllegalStateException("customer doesn't exist.");
					customer.setIdentity(resultSet.getLong("identity"));
					customer.setAlias(resultSet.getString("alias"));
					customer.setGivenName(resultSet.getString("givenName"));
					customer.setFamilyName(resultSet.getString("familyName"));
					customer.setStreet(resultSet.getString("street"));
					customer.setPostcode(resultSet.getString("postcode"));
					customer.setCity(resultSet.getString("city"));
					customer.setEmail(resultSet.getString("email"));
					customer.setPhone(resultSet.getString("phone"));
				}
			}
		}

		return customer;
	}


	/**
	 * Queries the given customer's order with the given identity.
	 * @param alias the customer alias
	 * @param passwordHash the customer password-hash
	 * @param orderIdentity the order identity
	 * @return the customer's order
	 * @throws IllegalStateException if the login data is invalid
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	public Order queryOrder (final String alias, final byte[] passwordHash, final long orderIdentity) throws SQLException {
		final Order order = new Order();
		final Customer customer = this.queryCustomer(alias, passwordHash);

		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_PURCHASE)) {
				statement.setLong(1, orderIdentity);

				try (ResultSet resultSet = statement.executeQuery()) {
					if (!resultSet.next()) throw new IllegalStateException("purchase doesn't exist.");
					if (customer.getIdentity() != resultSet.getLong("customerIdentity")) throw new IllegalStateException("purchase not created by given customer.");

					order.setIdentity(resultSet.getLong("identity"));
					order.setCustomerIdentity(resultSet.getLong("customerIdentity"));
					order.setCreationTimestamp(resultSet.getLong("creationTimestamp"));
					order.setTaxRate(resultSet.getDouble("taxRate"));
				}
			}
		}

		this.populateOrderItems(order);
		return order;
	}


	/**
	 * Queries the given customer's orders.
	 * @param alias the customer alias
	 * @param passwordHash the customer password-hash
	 * @return the customer's orders
	 * @throws IllegalStateException if the login data is invalid
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	public SortedSet<Order> queryOrders (final String alias, final byte[] passwordHash) throws SQLException {
		final SortedSet<Order> orders = new TreeSet<Order>();
		final Customer customer = this.queryCustomer(alias, passwordHash);

		synchronized (this.connection) {
			try (PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_PURCHASES)) {
				statement.setLong(1, customer.getIdentity());

				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						final Order purchase = new Order();
						purchase.setIdentity(resultSet.getLong("identity"));
						purchase.setCustomerIdentity(resultSet.getLong("customerIdentity"));
						purchase.setCreationTimestamp(resultSet.getLong("creationTimestamp"));
						purchase.setTaxRate(resultSet.getDouble("taxRate"));
						orders.add(purchase);
					}
				}
			}
		}

		for (final Order purchase : orders) {
			this.populateOrderItems(purchase);
		}

		return orders;
	}
}