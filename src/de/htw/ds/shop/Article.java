package de.htw.ds.shop;

import de.sb.java.TypeMetadata;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * This class models simplistic articles.
 */
@XmlRootElement
@TypeMetadata(copyright = "2010-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class Article extends Entity {
	static private final long serialVersionUID = 1L;

	private volatile String description;
	private volatile long price;
	private volatile int count;


	/**
	 * Returns the number of units on stock.
	 * @return the unit count
	 */
	@XmlAttribute
	public int getCount () {
		return this.count;
	}


	/**
	 * Returns the description.
	 * @return the description
	 */
	@XmlAttribute
	public String getDescription () {
		return this.description;
	}


	/**
	 * Returns the unit price (gross).
	 * @return the unit price
	 */
	@XmlAttribute
	public long getPrice () {
		return this.price;
	}


	/**
	 * Sets the number of units on stock.
	 * @param count the unit count
	 */
	public void setCount (final int count) {
		this.count = count;
	}


	/**
	 * Sets the description.
	 * @param description the description
	 */
	public void setDescription (final String description) {
		this.description = description;
	}


	/**
	 * Sets the unit price (gross).
	 * @param price the unit price
	 */
	public void setPrice (final long price) {
		this.price = price;
	}
}