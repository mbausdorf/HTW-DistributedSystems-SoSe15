package de.htw.ds.shop;

import java.util.Collection;
import java.util.SortedSet;
import de.sb.java.TypeMetadata;


/**
 * Shop SOAP service interface.
 */
@TypeMetadata(copyright = "2010-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public interface ShopService {

	/**
	 * Cancels an order if the given orderIdentity identifies an existing order of the user with the
	 * given alias, the password is correct for said user, and the order is no older than one hour.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @param orderIdentity the order identity
	 */
	void cancelOrder (String alias, String password, long orderIdentity);


	/**
	 * Creates an order from the given items. Note that the suggested price for each item must be
	 * equal to or exceed the current article price. Also, note that orders which exhaust the
	 * available article capacity are rejected.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @param items the order items
	 * @return the order identity
	 */
	long createOrder (String alias, String password, Collection<OrderItem> items);


	/**
	 * Returns the article data for the given identity.
	 * @param articleIdentity the article identity
	 */
	Article queryArticle (long articleIdentity);


	/**
	 * Returns all article data.
	 */
	SortedSet<Article> queryArticles ();


	/**
	 * Returns the customer data.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @return the customer matching the given alias and password
	 */
	Customer queryCustomer (String alias, String password);


	/**
	 * Queries the given order.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @param orderIdentity the order identity
	 * @return the customer's order
	 */
	Order queryOrder (String alias, String password, long orderIdentity);


	/**
	 * Queries the given customer's orders.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @return the customer's orders
	 * @throws NullPointerException if one of the given values is {@code null}
	 * @throws IllegalStateException if the login data is invalid
	 */
	SortedSet<Order> queryOrders (String alias, String password);


	/**
	 * Registers a new customer and returns it's identity. The given customer's identity is ignored
	 * during processing.
	 * @param customer the customer
	 * @param String password
	 * @return the customer identity
	 */
	long registerCustomer (Customer customer, String password);


	/**
	 * Unregisters a customer that has no orders.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @return the customer identity
	 */
	long unregisterCustomer (String alias, String password);
}