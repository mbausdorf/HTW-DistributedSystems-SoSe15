package de.htw.ds.shop;

import static javax.xml.bind.annotation.XmlAccessType.NONE;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import de.sb.java.TypeMetadata;


/**
 * This class models simplistic entities.
 */
@XmlType
@XmlAccessorType(NONE)
@XmlSeeAlso({ Article.class, Customer.class, Order.class, OrderItem.class })
@TypeMetadata(copyright = "2010-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public abstract class Entity implements Comparable<Entity>, Serializable {
	static private final long serialVersionUID = 1L;

	@XmlAttribute
	private volatile long identity;


	/**
	 * {@inheritDoc}
	 */
	public int compareTo (final Entity entity) {
		if (this.identity < entity.identity) return -1;
		if (this.identity > entity.identity) return 1;
		if (this.hashCode() < entity.hashCode()) return -1;
		if (this.hashCode() > entity.hashCode()) return 1;
		return 0;
	}


	/**
	 * Returns the identity.
	 * @return the identity
	 */
	public final long getIdentity () {
		return this.identity;
	}


	/**
	 * Sets the identity.
	 * @param identity the identity
	 */
	protected final void setIdentity (final long identity) {
		this.identity = identity;
	}
}