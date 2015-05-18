package de.htw.ds.board;

import de.sb.java.TypeMetadata;


/**
 * Instances of this class represent absolute piece motions.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class AbsoluteMotion extends AbstractMotion {

	private final byte sourceRank;
	private final byte sourceFile;
	private final byte sinkRank;
	private final byte sinkFile;


	/**
	 * Creates a new instance.
	 * @param sourceRank the source rank
	 * @param sourceFile the source file
	 * @param sinkRank the sink rank
	 * @param sinkFile the sink file
	 * @param flags the property flags
	 * @throws IllegalArgumentException if any of the given values is strictly negative
	 */
	public AbsoluteMotion (final byte sourceRank, final byte sourceFile, final byte sinkRank, final byte sinkFile, final byte flags) {
		super(flags);
		if (sourceRank < 0 | sourceFile < 0 | sinkRank < 0 | sinkFile < 0) throw new IllegalArgumentException();

		this.sourceRank = sourceRank;
		this.sourceFile = sourceFile;
		this.sinkRank = sinkRank;
		this.sinkFile = sinkFile;
	}


	/**
	 * Returns the source rank.
	 * @return the source rank
	 */
	public byte getSourceRank () {
		return this.sourceRank;
	}


	/**
	 * Returns the source file.
	 * @return the source file
	 */
	public byte getSourceFile () {
		return this.sourceFile;
	}


	/**
	 * Returns the sink rank.
	 * @return the sink rank
	 */
	public byte getSinkRank () {
		return this.sinkRank;
	}


	/**
	 * Returns the sink file.
	 * @return the sink file
	 */
	public byte getSinkFile () {
		return this.sinkFile;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode () {
		return Byte.hashCode(this.sourceRank) ^ Byte.hashCode(this.sourceFile) ^ Byte.hashCode(this.sinkRank) ^ Byte.hashCode(this.sinkFile);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals (final Object object) {
		if (!(object instanceof AbsoluteMotion)) return false;
		final AbsoluteMotion motion = (AbsoluteMotion) object;

		return this.sourceRank == motion.sourceRank & this.sourceFile == motion.sourceFile & this.sinkRank == motion.sinkRank & this.sinkFile == motion.sinkFile;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return String.format("[%d,%d]->[%d,%d]", this.sourceRank, this.sourceFile, this.sinkRank, this.sinkFile);
	}
}