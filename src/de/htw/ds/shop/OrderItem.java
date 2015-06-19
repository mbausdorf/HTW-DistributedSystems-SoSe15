package de.htw.ds.shop;

import de.sb.java.TypeMetadata;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * This class models simplistic order items. Note that this entity is equivalent to the
 * "PurchaseItem" table, because "order" is a reserved word in SQL.
 */
@XmlRootElement
@TypeMetadata(copyright = "2010-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class OrderItem extends Entity {
	static private final long serialVersionUID = 1L;

	private volatile long articleIdentity;
	private volatile long articleGrossPrice;
	private volatile int count;


	/**
	 * Returns the article price (gross).
	 * @return the article gross price
	 */
	@XmlAttribute
	public long getArticleGrossPrice () {
		return this.articleGrossPrice;
	}


	/**
	 * Returns the identity of the related article.
	 * @return the article identity
	 */
	@XmlAttribute
	public long getArticleIdentity () {
		return this.articleIdentity;
	}


	/**
	 * Returns the number of units ordered.
	 * @return the unit count
	 */
	@XmlAttribute
	public int getCount () {
		return this.count;
	}


	/**
	 * Sets the article price (gross).
	 * @param articleGrossPrice the article gross price
	 */
	public void setArticleGrossPrice (final long articleGrossPrice) {
		this.articleGrossPrice = articleGrossPrice;
	}


	/**
	 * Sets the identity of the related article.
	 * @param articleIdentity the article identity
	 */
	public void setArticleIdentity (final long articleIdentity) {
		this.articleIdentity = articleIdentity;
	}


	/**
	 * Sets the number of units ordered.
	 * @param count unit count
	 */
	public void setCount (final int count) {
		this.count = count;
	}
}