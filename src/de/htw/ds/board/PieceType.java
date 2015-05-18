package de.htw.ds.board;

import de.sb.java.TypeMetadata;


/**
 * Declares interface common to any kind of board based piece type, addressing base valuation and
 * text representation.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface PieceType {

	/**
	 * Returns this type's identifying ordinal, a positive number.
	 * @return the ordinal
	 * @see Enum#ordinal()
	 */
	int ordinal ();


	/**
	 * Returns this type's identifying name, an uppercase text.
	 * @return the ordinal
	 * @see Enum#name()
	 */
	String name ();


	/**
	 * Returns this type's unique character alias.
	 * @return the character alias
	 */
	char getAlias ();


	/**
	 * Returns this type's base rating in cents. Note that the rating is expected to be positive.
	 * @return the rating
	 */
	int getRating ();


	/**
	 * Returns this piece's motions. Each motion consists of a rank and file delta (relative to this
	 * piece's location), and associated movement details.
	 * @return the motions
	 */
	RelativeMotion[] getMotions ();
}