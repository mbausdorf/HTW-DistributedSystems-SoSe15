package de.htw.ds.board;

import java.util.Arrays;
import java.util.stream.Stream;
import de.sb.java.TypeMetadata;


/**
 * Instances of this class model abstract boards based on a single-dimensional table of pre-cached
 * positional pieces.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public abstract class AbstractTableBoard<T extends PieceType> extends AbstractBoard<T> {

	protected volatile Piece<T>[] pieces;


	@SuppressWarnings("unchecked")
	public AbstractTableBoard (final byte rankCount, final byte fileCount, final short moveClock, final short reversibleMoveClock) {
		super(rankCount, fileCount, moveClock, reversibleMoveClock);
		this.pieces = new Piece[Board.fieldCount(rankCount, fileCount)];
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractTableBoard<T> clone () {
		final AbstractTableBoard<T> clone = (AbstractTableBoard<T>) super.clone();
		clone.pieces = this.pieces.clone();
		return clone;
	}


	/**
	 * {@inheritDoc}
	 */
	public final Stream<Piece<T>> pieceStream() {
		return Arrays.stream(this.pieces).filter((Piece<T> piece) -> piece != null);
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	public final Piece<T> getPiece (final byte rank, final byte file) {
		try {
			return this.pieces[Board.coordinatesToPosition(rank, file, this.fileCount)];
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalArgumentException();
		}
	}
}