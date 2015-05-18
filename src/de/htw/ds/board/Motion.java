package de.htw.ds.board;

import de.sb.java.TypeMetadata;


/**
 * Instances of this interface represent relative or absolute piece motions.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface Motion {

	/**
	 * Flag indicating that this motion is continuous.
	 */
	static public final byte CONTINUOUS = 0x01;

	/**
	 * Flag indicating that this motion is repetitive.
	 */
	static public final byte REPETITIVE = 0x02;

	/**
	 * Flag indicating that this motion must capture an opposing piece.
	 */
	static public final byte CAPTURE_REQUIRED = 0x04;

	/**
	 * Flag indicating that this motion must not capture an opposing piece.
	 */
	static public final byte CAPTURE_FORBIDDEN = 0x08;

	/**
	 * Flag indicating that the piece must have moved before.
	 */
	static public final byte TOUCH_REQUIRED = 0x10;

	/**
	 * Flag indicating that the piece must not have moved before.
	 */
	static public final byte TOUCH_FORBIDDEN = 0x20;


	/**
	 * Returns the property flags.
	 * @return the property flags
	 */
	byte getFlags ();


	/**
	 * Returns {@code true} if this motion is continuous, {@code false} otherwise.
	 * @return whether or not this motion is continuous
	 */
	boolean isContinuous ();


	/**
	 * Returns {@code true} if this motion is repetitive, {@code false} otherwise.
	 * @return whether or not this motion is repetitive
	 */
	boolean isRepetitive ();


	/**
	 * Returns {@code true} if this move must capture an opposing piece, {@code false} otherwise.
	 * @return whether or not this move must capture an opposing piece
	 */
	boolean isCaptureRequired ();


	/**
	 * Returns {@code true} if this move must not capture an opposing piece, {@code false}
	 * otherwise.
	 * @return whether or not this move must not capture an opposing piece
	 */
	boolean isCaptureForbidden ();


	/**
	 * Returns {@code true} if this must be a piece's subsequent move, {@code false} otherwise.
	 * @return whether or not this must be a piece's subsequent move
	 */
	boolean isTouchRequired ();


	/**
	 * Returns {@code true} if this must be a piece's first move, {@code false} otherwise.
	 * @return whether or not this must be a piece's first move
	 */
	boolean isTouchForbidden ();
}