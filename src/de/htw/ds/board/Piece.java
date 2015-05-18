package de.htw.ds.board;

import de.sb.java.TypeMetadata;


/**
 * Interface for any kind of positional board piece, bundling information about color, type, rank,
 * file, etc. Positional pieces can only be used for a specific position on boards with matching
 * dimensions. However, the advantage of positional pieces is that they can pre-calculate absolute
 * move directions that are geometrically valid, which is a much faster approach compared to using
 * relative move directions. This is especially so since, even with the best available algorithms,
 * sorting out geometrically invalid moves during move analysis involves excessive amounts of badly
 * predictable if/else decisions, whose randomness causes excessive amounts of branch prediction
 * failures in modern processor pipelines.
 * @param <T> the piece type
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface Piece<T extends PieceType> extends Comparable<Piece<T>> {

	/**
	 * Returns the piece type.
	 * @return the type
	 */
	T getType ();


	/**
	 * Returns whether or not this piece is white.
	 * @return {@code true} if this piece is white, {@code false} otherwise
	 */
	boolean isWhite ();


	/**
	 * Returns the number of board ranks this piece is related to. Positional pieces must only be
	 * used with boards that feature a corresponding number of ranks.
	 * @return the rank count
	 */
	byte getRankCount ();


	/**
	 * Returns the number of board files this piece is related to. Positional pieces must only be
	 * used with boards that feature a corresponding number of files.
	 * @return the file count
	 */
	byte getFileCount ();


	/**
	 * Returns the rank of this piece.
	 * @return the rank
	 */
	byte getRank ();


	/**
	 * Returns the file of this piece.
	 * @return the file
	 */
	byte getFile ();


	/**
	 * Returns the reverse rank of this piece.
	 * @return the reverse rank
	 */
	byte getReverseRank ();


	/**
	 * Returns the reverse file of this piece.
	 * @return the reverse file
	 */
	byte getReverseFile ();


	/**
	 * Returns the position corresponding to this piece's rank and file.
	 * @return the position
	 */
	short getPosition ();


	/**
	 * Returns this piece's ordinal which uniquely identifies the piece within it's universe. It is
	 * used to address the piece within caches, for example in enums. Note that this method's name
	 * has been chosen to conform to {@linkplain java.lang.Enum}, as enums are possible implementors
	 * of this interface. The downside is that name does not conform to the JavaBeans specification,
	 * and is therefore hard to reflect as an accessor.
	 * @return the ordinal
	 */
	int ordinal ();


	/**
	 * Returns the name which uniquely identifies this piece within it's universe. Note that this
	 * method's name has been chosen to conform to {@linkplain java.lang.Enum}, as enums are
	 * possible implementors of this interface. The downside is that name does not conform to the
	 * JavaBeans specification, and is therefore hard to reflect as an accessor.
	 * @return the name
	 */
	String name ();


	/**
	 * Returns the character alias which uniquely identifies this piece's type and color.
	 * @return the character alias
	 */
	char getAlias ();


	/**
	 * Returns the piece rating in cents. With universal pieces, the rating will be adjusted for
	 * piece color only, i.e. positive for white pieces, and negative for black ones. With
	 * positional pieces, the rating will be adjusted for both piece color and piece position.
	 * @return the rating
	 */
	int getRating ();


	/**
	 * Returns this piece's candidate motions, with each entry consisting of geometrically valid
	 * motions for continuous movement in one direction. This implies that non-continuous kinds of
	 * movement will cause exactly one motion per direction.
	 * @return the motions
	 */
	AbsoluteMotion[][] getMotions ();


	/**
	 * Returns the motion map indicating all positions reachable within a single move. Basically, the
	 * resulting bit-board contains all the sink positions returned by {@linkplain #getMotions()} in
	 * the form {@code 1 << sinkPosition}, but without information about motion direction. This
	 * renders them useless for move generation, as pieces may be blocked in certain directions;
	 * however, it does allow for speedy tests if a piece can move to a given position at all, by
	 * simply testing for the presence or absence of the bit matching the position in question.
	 * @return the movement map as a bit-board
	 */
	long[] getMotionMap ();
}