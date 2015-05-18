package de.htw.ds.board.chess;

import de.htw.ds.board.AbsoluteMotion;
import de.htw.ds.board.Prediction;
import de.sb.java.TypeMetadata;


/**
 * This is a template for ChessTableBoard subclasses.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class ChessTableBoard1 extends ChessTableBoard {

	/**
	 * Creates an empty chess board with the given dimensions and clocks.
	 * @param rankCount the number of ranks on the board
	 * @param fileCount the number of files on the board
	 * @param moveClock the move clock, see {@linkplain #getMoveClock}
	 * @param reversibleMoveClock the reversible move clock, see
	 *        {@linkplain #getReversibleMoveClock}
	 * @throws IllegalArgumentException if the given rank of file count is smaller than {@code 3},
	 *         or if any of the given clocks is negative, or if the reversible move clock exceeds
	 *         the standard move clock or 99
	 */
	public ChessTableBoard1(final byte rankCount, final byte fileCount, final short moveClock, final short reversibleMoveClock) {
		super(rankCount, fileCount, moveClock, reversibleMoveClock);
	}


	/**
	 * Creates a chess board with the given properties.
	 * @param pieceMatrix the game neutral piece matrix
	 * @param moveClock the move clock, see {@linkplain #getMoveClock}
	 * @param reversibleMoveClock the reversible move clock, see
	 *        {@linkplain #getReversibleMoveClock}
	 * @param castlingAbilities the castling abilities as an array of four boolean values, i.e. the
	 *        ability of white to castle to the left or right, and the ability of black to castle to
	 *        the left or right
	 * @param passingPawnCoordinates the passing pawn coordinates as a rank&file pair, or
	 *        {@code null} for none
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if any if the given characters is not a legal chess piece
	 *         representation, or if the given piece matrix, or any of it's elements, has more than
	 *         {@code 127} slots, or if any of the move clocks is negative, or if the castling
	 *         abilities do not have four elements, of if the resulting chess board would be invalid
	 */
	public ChessTableBoard1(final char[][] pieceMatrix, final short moveClock, final short reversibleMoveClock, final boolean[] castlingAbilities, byte[] passingPawnCoordinates) {
		super(pieceMatrix, moveClock, reversibleMoveClock, castlingAbilities, passingPawnCoordinates);
	}

	@Override
	protected Prediction analyzeRecursively(AbsoluteMotion[] move, int depth) throws InterruptedException {
		Thread.interrupted();
		return super.analyzeRecursively(move, depth);
	}
}