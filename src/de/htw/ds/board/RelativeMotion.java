package de.htw.ds.board;

import de.sb.java.TypeMetadata;


/**
 * Instances of this class represent relative piece motions.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class RelativeMotion extends AbstractMotion {

	private final byte rankDelta;
	private final byte fileDelta;


	/**
	 * Creates a new instance.
	 * @param rankDelta the rank delta
	 * @param fileDelta the file delta
	 * @param flags the property flags
	 */
	public RelativeMotion (final byte rankDelta, final byte fileDelta, final byte flags) {
		super(flags);

		this.rankDelta = rankDelta;
		this.fileDelta = fileDelta;
	}


	/**
	 * Returns the rank delta.
	 * @return the rank delta
	 */
	public byte getRankDelta () {
		return this.rankDelta;
	}


	/**
	 * Returns the file delta.
	 * @return the file delta
	 */
	public byte getFileDelta () {
		return this.fileDelta;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode () {
		return Byte.hashCode(this.rankDelta) ^ Byte.hashCode(this.fileDelta);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals (final Object object) {
		if (!(object instanceof RelativeMotion)) return false;
		final RelativeMotion motion = (RelativeMotion) object;

		return this.rankDelta == motion.rankDelta & this.fileDelta == motion.fileDelta;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return String.format("[%d,%d]", this.rankDelta, this.fileDelta);
	}
}