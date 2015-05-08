package de.htw.ds.board;

import de.sb.java.math.ExtendedMath;
import de.sb.java.util.BitArrays;


/**
 * Instances of this abstract class model positional pieces. Positional pieces are concrete
 * definitions of pieces, i.e. they have a relation to a board dimension, and their position within
 * such boards. Such pieces can only be reused for a specific position on matching boards. However,
 * the advantage is that they can both be cached, and pre-calculate geometrically valid moves, which
 * is much faster compared to calculating moves whenever required.<br />
 * Note that this class has a natural ordering that is inconsistent with
 * {@linkplain #equals(Object)}, as pieces related to differing board dimensions may share the same
 * ordinal. Therefore, equality remains based on object identity, not ordinal; practically this
 * should never be a problem, as differing board dimensions indicate differing game variants as
 * well.
 * @param <T> the piece type
 */
public abstract class AbstractPiece<T extends PieceType> implements Piece<T> {
	static private final byte LOG2_WORD_SIZE = ExtendedMath.log2(Long.SIZE);

	private final T type;
	private final boolean white;
	private final byte rank;
	private final byte file;
	private final byte rankCount;
	private final byte fileCount;

	private final AbsoluteMotion[][] moves;
	private final long[] moveMap;
	private final int rating;


	/**
	 * Creates a new instance.
	 * @param type the type
	 * @param white {@code true} for white, {@code false} for black
	 * @param rank the rank
	 * @param file the file
	 * @param rankCount the number of ranks on a board
	 * @param fileCount the number of files on a board
	 * @throws NullPointerException if the given type is {@code null}
	 * @throws IllegalArgumentException if the given rank or file are strictly negative, or if they
	 *         equal or exceed their corresponding rank count or file count
	 */
	public AbstractPiece (final T type, final boolean white, final byte rank, final byte file, final byte rankCount, final byte fileCount) {
		if (type == null) throw new NullPointerException();
		if (rank < 0 | file < 0 | rank >= rankCount | file >= fileCount) throw new IllegalArgumentException();

		this.type = type;
		this.white = white;
		this.rank = rank;
		this.file = file;
		this.rankCount = rankCount;
		this.fileCount = fileCount;

		this.moves = this.calculateMoves();
		this.moveMap = this.calculateMoveMap();
		this.rating = this.calculateRating();
	}


	/**
	 * {@inheritDoc}
	 */
	public final T getType () {
		return this.type;
	}


	/**
	 * {@inheritDoc}
	 */
	public final boolean isWhite () {
		return this.white;
	}


	/**
	 * {@inheritDoc}
	 */
	public final byte getRank () {
		return this.rank;
	}


	/**
	 * {@inheritDoc}
	 */
	public final byte getFile () {
		return this.file;
	}


	/**
	 * {@inheritDoc}
	 */
	public byte getReverseRank () {
		return (byte) (this.rankCount + ~this.rank);		// == this.rankCount - this.rank - 1
	}


	/**
	 * {@inheritDoc}
	 */
	public byte getReverseFile () {
		return (byte) (this.fileCount + ~this.file);		// = this.fileCount - this.file - 1
	}


	/**
	 * {@inheritDoc}
	 */
	public final short getPosition () {
		return (short) Board.coordinatesToPosition(this.rank, this.file, this.fileCount);
	}


	/**
	 * {@inheritDoc}
	 */
	public final byte getRankCount () {
		return this.rankCount;
	}


	/**
	 * {@inheritDoc}
	 */
	public final byte getFileCount () {
		return this.fileCount;
	}


	/**
	 * {@inheritDoc}
	 */
	public final short getFieldCount () {
		return (short) Board.fieldCount(this.rankCount, this.fileCount);
	}


	/**
	 * {@inheritDoc}
	 */
	public AbsoluteMotion[][] getMotions () {
		return this.moves;
	}


	/**
	 * {@inheritDoc}
	 */
	public long[] getMotionMap () {
		return this.moveMap;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getRating () {
		return this.rating;
	}


	/**
	 * {@inheritDoc}
	 */
	public final String name () {
		final String alias = Board.coordinatesToAlias(this.rank, this.file, this.fileCount);
		return String.format("%s_%s@%s", this.white ? "WHITE" : "BLACK", this.type, alias);
	}


	/**
	 * {@inheritDoc}
	 */
	public final char getAlias () {
		final char typeAlias = this.type.getAlias();
		return this.white ? Character.toUpperCase(typeAlias) : Character.toLowerCase(typeAlias);
	}


	/**
	 * {@inheritDoc}
	 */
	public final int compareTo (final Piece<T> piece) {
		return this.ordinal() - piece.ordinal();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return this.name();
	}


	/**
	 * Calculates the move map bit-board indicating all positions reachable within a single move.
	 * Note that this algorithm assumes that this piece's moves have already been calculated.
	 * @return the move map as a bit-board
	 * @see #getMotionMap()
	 */
	protected final long[] calculateMoveMap () {
		final int fieldCount = Board.fieldCount(this.rankCount, this.fileCount);
		final long[] result = new long[((fieldCount - 1) >> LOG2_WORD_SIZE) + 1];
		for (final AbsoluteMotion[] directedMoves : this.moves) {
			for (final AbsoluteMotion move : directedMoves) {
				final int sinkPosition = Board.coordinatesToPosition(move.getSinkRank(), move.getSinkFile(), this.fileCount);
				BitArrays.on(result, sinkPosition);
			}
		}
		return result;
	}


	/**
	 * Calculates this piece's move directions, with each entry consisting of geometrically valid
	 * moves stemming from subsequent steps of continuous movement. This implies that non-continuous
	 * kinds of movement will contain exactly one move per move direction.
	 * @return the moves
	 * @see #getMotions()
	 */
	protected abstract AbsoluteMotion[][] calculateMoves ();


	/**
	 * Calculates the positional rating for this piece, in cents. The rating takes into account both
	 * piece color and piece position, but no other pieces.
	 * @return the piece rating
	 * @see #getRating()
	 */
	protected abstract int calculateRating ();
}