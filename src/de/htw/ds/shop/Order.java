package de.htw.ds.shop;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import de.sb.java.TypeMetadata;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * This class models simplistic orders. Note that this entity is equivalent to the "purchase" table,
 * because "order" is a reserved word in SQL.
 */
@XmlRootElement
@TypeMetadata(copyright = "2010-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class Order extends Entity {
	static private final long serialVersionUID = 1L;

	private volatile long customerIdentity;
	private volatile long creationTimestamp;
	private volatile double taxRate;
	private final SortedSet<OrderItem> items;


	/**
	 * Creates a new instance.
	 */
	public Order() {
		this.items = Collections.synchronizedSortedSet(new TreeSet<OrderItem>());
	}


	/**
	 * Returns the creation time stamp in milliseconds since 1/1/1970.
	 * @return the creation time stamp
	 */
	@XmlAttribute
	public long getCreationTimestamp () {
		return this.creationTimestamp;
	}


	/**
	 * Returns the identity of the related customer.
	 * @return the customer identity
	 */
	@XmlAttribute
	public long getCustomerIdentity () {
		return this.customerIdentity;
	}


	/**
	 * Returns the gross sum.
	 * @return the gross sum
	 */
	@XmlAttribute
	public long getGrossPrice () {
		int grossPrice = 0;
		for (final OrderItem item : this.items) {
			grossPrice += item.getArticleGrossPrice() * item.getCount();
		}
		return grossPrice;
	}


	/**
	 * Returns the number of items.
	 * @return the item count
	 */
	@XmlAttribute
	public int getItemCount () {
		return this.items.size();
	}


	/**
	 * Returns the related order items.
	 * @return the order items
	 */
	@XmlElement
	public SortedSet<OrderItem> getItems () {
		return this.items;
	}


	/**
	 * Returns the net sum.
	 * @return the net sum
	 */
	@XmlAttribute
	public long getNetPrice () {
		return this.getGrossPrice() + this.getTax();
	}


	/**
	 * Returns the tax sum.
	 * @return the tax sum
	 */
	@XmlAttribute
	public long getTax () {
		return Math.round(this.getGrossPrice() * this.getTaxRate());
	}


	/**
	 * Returns the tax rate.
	 * @return the tax rate
	 */
	@XmlAttribute
	public double getTaxRate () {
		return this.taxRate;
	}


	/**
	 * Sets the creation time stamp in milliseconds since 1/1/1970.
	 * @param creationTimestamp the creation time stamp
	 */
	public void setCreationTimestamp (final long creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}


	/**
	 * Sets the identity of the related customer.
	 * @param customerIdentity the customer identity
	 */
	public void setCustomerIdentity (final long customerIdentity) {
		this.customerIdentity = customerIdentity;
	}


	/**
	 * Sets the tax rate.
	 * @param taxRate the tax rate
	 */
	public void setTaxRate (final double taxRate) {
		this.taxRate = taxRate;
	}
}