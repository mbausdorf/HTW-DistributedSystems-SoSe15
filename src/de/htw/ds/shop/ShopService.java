package de.htw.ds.shop;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.SortedSet;
import de.sb.java.TypeMetadata;

import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebService;


/**
 * Shop SOAP service interface.
 */
@WebService
@TypeMetadata(copyright = "2010-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public interface ShopService {

	/**
	 * Cancels an order if the given orderIdentity identifies an existing order of the user with the
	 * given alias, the password is correct for said user, and the order is no older than one hour.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @param orderIdentity the order identity
	 */
	void cancelOrder (
			@WebParam(name = "alias") String alias,
			@WebParam(name = "password") String password,
			@WebParam(name = "orderIdentity") long orderIdentity) throws NoSuchAlgorithmException, UnsupportedEncodingException, SQLException, IllegalStateException;


	/**
	 * Creates an order from the given items. Note that the suggested price for each item must be
	 * equal to or exceed the current article price. Also, note that orders which exhaust the
	 * available article capacity are rejected.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @param items the order items
	 * @return the order identity
	 */
	long createOrder (
			@WebParam(name = "alias") String alias,
			@WebParam(name = "password") String password,
			@WebParam(name = "items") Collection<OrderItem> items)  throws NoSuchAlgorithmException, UnsupportedEncodingException, SQLException;


	/**
	 * Returns the article data for the given identity.
	 * @param articleIdentity the article identity
	 */
	Article queryArticle (@WebParam(name = "articleIdentity") long articleIdentity) throws SQLException;


	/**
	 * Returns all article data.
	 */
	SortedSet<Article> queryArticles ()throws SQLException;


	/**
	 * Returns the customer data.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @return the customer matching the given alias and password
	 */
	Customer queryCustomer (
			@WebParam(name = "alias") String alias,
			@WebParam(name = "password") String password) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException;


	/**
	 * Queries the given order.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @param orderIdentity the order identity
	 * @return the customer's order
	 */
	Order queryOrder (
			@WebParam(name = "alias") String alias,
			@WebParam(name = "password") String password,
			@WebParam(name = "orderIdentity") long orderIdentity) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException;


	/**
	 * Queries the given customer's orders.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @return the customer's orders
	 * @throws NullPointerException if one of the given values is {@code null}
	 * @throws IllegalStateException if the login data is invalid
	 */
	SortedSet<Order> queryOrders (
			@WebParam(name = "alias") String alias,
			@WebParam(name = "password") String password) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException;


	/**
	 * Registers a new customer and returns it's identity. The given customer's identity is ignored
	 * during processing.
	 * @param customer the customer
	 * @param String password
	 * @return the customer identity
	 */
	long registerCustomer (
			@WebParam(name = "customer") Customer customer,
			@WebParam(name = "password") String password) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException;


	/**
	 * Unregisters a customer that has no orders.
	 * @param alias the customer alias
	 * @param password the customer password
	 * @return the customer identity
	 */
	long unregisterCustomer (
			@WebParam(name = "alias") String alias,
			@WebParam(name = "password") String password) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException;
}