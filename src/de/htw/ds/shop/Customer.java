package de.htw.ds.shop;

import de.sb.java.TypeMetadata;


/**
 * This class models simplistic customers.
 */
@TypeMetadata(copyright = "2010-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class Customer extends Entity {
	static private final long serialVersionUID = 1L;

	private volatile String alias;
	private volatile String givenName;
	private volatile String familyName;
	private volatile String street;
	private volatile String postcode;
	private volatile String city;
	private volatile String email;
	private volatile String phone;


	/**
	 * Returns the unique alias.
	 * @return the alias
	 */
	public String getAlias () {
		return this.alias;
	}


	/**
	 * Returns the city.
	 * @return the city
	 */
	public String getCity () {
		return this.city;
	}


	/**
	 * Returns the e-mail address.
	 * @return the e-mail address
	 */
	public String getEmail () {
		return this.email;
	}


	/**
	 * Returns the family name.
	 * @return the family name
	 */
	public String getFamilyName () {
		return this.familyName;
	}


	/**
	 * Returns the given name.
	 * @return the given name
	 */
	public String getGivenName () {
		return this.givenName;
	}


	/**
	 * Returns the phone number.
	 * @return the phone number
	 */
	public String getPhone () {
		return this.phone;
	}


	/**
	 * Returns the post code.
	 * @return the post code
	 */
	public String getPostcode () {
		return this.postcode;
	}


	/**
	 * Returns the street and street number.
	 * @return the street
	 */
	public String getStreet () {
		return this.street;
	}


	/**
	 * Sets the unique alias.
	 * @param alias the alias
	 */
	public void setAlias (final String alias) {
		this.alias = alias;
	}


	/**
	 * Sets the city.
	 * @param city the city
	 */
	public void setCity (final String city) {
		this.city = city;
	}


	/**
	 * Sets the e-mail address.
	 * @param email the e-mail address
	 */
	public void setEmail (final String email) {
		this.email = email;
	}


	/**
	 * Sets the family name.
	 * @param familyName the family name
	 */
	public void setFamilyName (final String familyName) {
		this.familyName = familyName;
	}


	/**
	 * Sets the given name.
	 * @param givenName the given name
	 */
	public void setGivenName (final String givenName) {
		this.givenName = givenName;
	}


	/**
	 * Sets the phone number.
	 * @param phone the phone number
	 */
	public void setPhone (final String phone) {
		this.phone = phone;
	}


	/**
	 * Sets the post code.
	 * @param postcode the post code
	 */
	public void setPostcode (final String postcode) {
		this.postcode = postcode;
	}


	/**
	 * Sets the street and street number.
	 * @param street the street
	 */
	public void setStreet (final String street) {
		this.street = street;
	}
}