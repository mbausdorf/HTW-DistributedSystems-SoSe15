package de.htw.ds.board;

import de.sb.java.TypeMetadata;


/**
 * Instances of this interface model boards based on a fixed number of ranks (rows) & files
 * (columns), and clocks measuring time in half moves. Note that the reversible move clock counts
 * the half moves since the last reversible (half) move, while the normal clock counts any (half)
 * move.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public abstract class AbstractBoard<T extends PieceType> implements Board<T> {

	protected final byte rankCount;
	protected final byte fileCount;
	protected volatile short moveClock;
	protected volatile short reversibleMoveClock;


	/**
	 * Creates an empty board with the given dimensions and clocks.
	 * @param rankCount the number of ranks on the board
	 * @param fileCount the number of files on the board
	 * @param moveClock the move clock, see {@linkplain #getMoveClock}
	 * @param reversibleMoveClock the reversible move clock, see
	 *        {@linkplain #getReversibleMoveClock}
	 * @throws IllegalArgumentException if the given rank of file count is smaller than {@code 3},
	 *         or if any of the given clocks is negative, or if the reversible move clock exceeds
	 *         the standard move clock
	 */
	public AbstractBoard (final byte rankCount, final byte fileCount, final short moveClock, final short reversibleMoveClock) {
		if (rankCount < 0 | fileCount < 0 | reversibleMoveClock < 0 | reversibleMoveClock > moveClock) throw new IllegalArgumentException();

		this.rankCount = rankCount;
		this.fileCount = fileCount;
		this.moveClock = moveClock;
		this.reversibleMoveClock = reversibleMoveClock;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public AbstractBoard<T> clone () {
		try {
			return (AbstractBoard<T>) super.clone();
		} catch (final CloneNotSupportedException exception) {
			throw new AssertionError();
		}
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
	public final short getMoveClock () {
		return this.moveClock;
	}


	/**
	 * {@inheritDoc}
	 */
	public final short getReversibleMoveClock () {
		return this.reversibleMoveClock;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getRating () {
		return this.pieceStream().mapToInt(Piece::getRating).sum();
	}


	/**
	 * {@inheritDoc}
	 */
	public final boolean isWhiteActive () {
		return (this.moveClock & 1) == 0;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return this.getXfenState();
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws InterruptedException {@inheritDoc}
	 */
	public Prediction analyze (final int depth) throws InterruptedException {
		return this.analyzeRecursively(depth);
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	public final Prediction analyzeUninterruptibly (final int depth) {
		while (true) {
			try {
				return this.analyzeRecursively(depth);
			} catch (final InterruptedException exception) {
				// repeat until analysis is no longer interrupted
			}
		}
	}


	/**
	 * Recursively analyzes this board for candidate moves and counter moves up until the given
	 * search depth, beginning with this board's active color, and implementing the minimax game
	 * theory principle. The result contains the next depth (half) moves predicted given optimum
	 * play from both sides, and the board rating after performing said moves. If the game is
	 * expected to end shortly (or has ended), the result will contain fewer (no) moves, and a
	 * rating that indicates if the game is a win for a particular side, or a draw.
	 * @param depth the search depth in half moves
	 * @return the prediction for the next {@code depth} (half) moves, and the board rating after
	 *         performing said moves
	 * @throws IllegalArgumentException if the given depth is negative
	 * @throws InterruptedException if this operation is interrupted by another thread
	 */
	protected abstract Prediction analyzeRecursively (final int depth) throws InterruptedException;


	/**
	 * Return the lower left board position.
	 * @return the lower left position
	 */
	protected final int getLowerLeftPosition () {
		return 0;
	}


	/**
	 * Return the lower right board position.
	 * @return the lower right position
	 */
	protected final int getLowerRightPosition () {
		return this.fileCount - 1;
	}


	/**
	 * Return the upper left board position.
	 * @return the upper left position
	 */
	protected final int getUpperLeftPosition () {
		return (this.rankCount - 1) * fileCount;
	}


	/**
	 * Return the upper right board position.
	 * @return the upper right position
	 */
	protected final int getUpperRightPosition () {
		return this.rankCount * this.fileCount - 1;
	}
}