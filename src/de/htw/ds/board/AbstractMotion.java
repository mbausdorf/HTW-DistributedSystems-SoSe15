package de.htw.ds.board;

import de.sb.java.TypeMetadata;


/**
 * Instances of this class represent relative or absolute piece motions.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public abstract class AbstractMotion implements Motion {

	private final byte flags;


	/**
	 * Creates a new instance.
	 * @param flags the property flags
	 */
	public AbstractMotion (final byte flags) {
		this.flags = flags;
	}


	/**
	 * Returns the property flags.
	 * @return the property flags
	 */
	public final byte getFlags () {
		return this.flags;
	}


	/**
	 * Returns {@code true} if this motion is continuous, {@code false} otherwise.
	 * @return whether or not this motion is continuous
	 */
	public final boolean isContinuous () {
		return (this.flags & CONTINUOUS) != 0;
	}


	/**
	 * Returns {@code true} if this motion is repetitive, {@code false} otherwise.
	 * @return whether or not this motion is repetitive
	 */
	public final boolean isRepetitive () {
		return (this.flags & REPETITIVE) != 0;
	}


	/**
	 * Returns {@code true} if this move must capture an opposing piece, {@code false} otherwise.
	 * @return whether or not this move must capture an opposing piece
	 */
	public final boolean isCaptureRequired () {
		return (this.flags & CAPTURE_REQUIRED) != 0;
	}


	/**
	 * Returns {@code true} if this move must not capture an opposing piece, {@code false}
	 * otherwise.
	 * @return whether or not this move must not capture an opposing piece
	 */
	public final boolean isCaptureForbidden () {
		return (this.flags & CAPTURE_FORBIDDEN) != 0;
	}


	/**
	 * Returns {@code true} if this must be a piece's subsequent move, {@code false} otherwise.
	 * @return whether or not this must be a piece's subsequent move
	 */
	public final boolean isTouchRequired () {
		return (this.flags & TOUCH_REQUIRED) != 0;
	}


	/**
	 * Returns {@code true} if this must be a piece's first move, {@code false} otherwise.
	 * @return whether or not this must be a piece's first move
	 */
	public final boolean isTouchForbidden () {
		return (this.flags & TOUCH_FORBIDDEN) != 0;
	}
}