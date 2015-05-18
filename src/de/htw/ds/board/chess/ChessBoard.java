package de.htw.ds.board.chess;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import de.htw.ds.board.Board;
import de.htw.ds.board.Piece;
import de.htw.ds.board.Prediction;
import de.sb.java.TypeMetadata;


/**
 * Instances of this interface model chess boards.
 * @see <a href="http://en.wikipedia.org/wiki/Chess">Wikipedia article about Chess</a>
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface ChessBoard extends Board<ChessPieceType> {

	/**
	 * Chess boards must have at least 3 ranks to allow for one rank distance between two
	 * single-rank armies.
	 */
	static final byte MIN_RANK_COUNT = 3;

	/**
	 * Chess boards must have at least 3 files to avoid negative castling distances. Note that kings
	 * castle on the spot on boards with 3 and 4 files.
	 */
	static final byte MIN_FILE_COUNT = 3;

	/**
	 * The bit-mask for the right to castle to the left of the white king.
	 */
	static final byte MASK_CASTLE_WHITE_LEFT = 0b00000001;

	/**
	 * The bit-mask for the right to castle to the right of the white king.
	 */
	static final byte MASK_CASTLE_WHITE_RIGHT = 0b00000010;

	/**
	 * The bit-mask for the right to castle to the left of the black king.
	 */
	static final byte MASK_CASTLE_BLACK_LEFT = 0b00000100;

	/**
	 * The bit-mask for the right to castle to the right of the black king.
	 */
	static final byte MASK_CASTLE_BLACK_RIGHT = 0b00001000;

	/**
	 * The default board setups for chess boards with eight ranks three to ten files.
	 */
	static final String[] DEFAULT_EIGHT_RANK_SETUPS = {
		"rkr/ppp/3/3/3/3/PPP/RKR w KQkq - 0 1",
		"rekr/pppp/4/4/4/4/PPPP/REKR w KQkq - 0 1",
		"rckcr/ppppp/5/5/5/5/PPPPP/RCKCR w KQkq - 0 1",
		"raqkar/pppppp/6/6/6/6/PPPPPP/RAQKAR w KQkq - 0 1",
		"rnqkanr/ppppppp/7/7/7/7/PPPPPPP/RNQKANR w KQkq - 0 1",
		"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
		"rnbqkbncr/ppppppppp/9/9/9/9/PPPPPPPPP/RNBQKBNCR w KQkq - 0 1",
		"rnabqkbanr/pppppppppp/10/10/10/10/PPPPPPPPPP/RNABQKBANR w KQkq - 0 1"
	};

	/**
	 * Compares two prediction alternatives from the perspective of the white opponent.
	 */
	static final Comparator<Prediction> WHITE_PREDICTION_COMPARATOR = (Prediction left, Prediction right) -> {
		int compare = +Integer.compare(left.getRating(), right.getRating());
		if (compare != 0) return compare;
		compare = Integer.compare(left.getMoveSequence().size(), right.getMoveSequence().size());
		return left.getRating() > 0 ? -compare : +compare;
	};

	/**
	 * Compares two prediction alternatives from the perspective of the black opponent.
	 */
	static final Comparator<Prediction> BLACK_PREDICTION_COMPARATOR = (Prediction left, Prediction right) -> {
		int compare = -Integer.compare(left.getRating(), right.getRating());
		if (compare != 0) return compare;
		compare = Integer.compare(left.getMoveSequence().size(), right.getMoveSequence().size());
		return left.getRating() < 0 ? -compare : +compare;
	};


	/**
	 * Returns the castling abilities.
	 * @return the four castling abilities as a bit field
	 */
	byte getCastlingAbilities ();


	/**
	 * Returns the given side's king.
	 * @param white {@code true} for the white king, {@code false} for the black king
	 * @return the king, or {@code null} for none
	 */
	Piece<ChessPieceType> getKing (boolean white);


	/**
	 * Returns the passive pawn that may be captured en passant by an active pawn.
	 * @return the passing pawn, or {@code null} for none
	 */
	Piece<ChessPieceType> getPassingPawn ();


	/**
	 * Creates an empty chess board with the given dimension.
	 * @param rankCount the number of ranks on the board
	 * @param fileCount the number of files on the board
	 * @param moveClock the move clock, see {@linkplain #getMoveClock}
	 * @param reversibleMoveClock the reversible move clock, see
	 *        {@linkplain #getReversibleMoveClock}
	 * @throws IllegalArgumentException if the given rank or file count is smaller than {@code 3},
	 *    or if any of the move clocks is negative
	 */
	static <T extends ChessBoard> T newInstance (final Class<T> boardClass, final byte rankCount, final byte fileCount, final short moveClock, final short reversibleMoveClock) {
		try {
			final Constructor<T> constructor = boardClass.getConstructor(byte.class, byte.class, short.class, short.class);
			return constructor.newInstance(rankCount, fileCount, moveClock, reversibleMoveClock);
		} catch (final InvocationTargetException exception) {
			final Throwable cause = exception.getCause();
			if (cause instanceof Error) throw (Error) cause;
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			throw new AssertionError();
		} catch (final Exception exception) {
			throw new IllegalArgumentException();
		}
	}


	/**
	 * Creates a new chess board with the given properties.
	 * @param boardClass the chess board class
	 * @param pieceMatrix the game neutral piece matrix
	 * @param moveClock the move clock, see {@linkplain #getMoveClock}
	 * @param reversibleMoveClock the reversible move clock, see
	 *        {@linkplain #getReversibleMoveClock}
	 * @param castlingAbilities the castling abilities as an array of four boolean values, i.e. the
	 *        ability of white to castle to the left or right, and the ability of black to castle to
	 *        the left or right
	 * @param passingPawnCoordinates the passing pawn coordinates as a rank&file pair, or
	 *        {@code null} for none
	 * @return the chess board created
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if any of the given characters is not a legal chess piece
	 *         representation, or if the given piece matrix, or any of it's elements, has more than
	 *         {@code 127} slots, or if any of the move clocks is negative, of if the resulting
	 *         droughts board would be invalid
	 */
	static <T extends ChessBoard> T newInstance (final Class<T> boardClass, final char[][] pieceMatrix, final short moveClock, final short reversibleMoveClock, final boolean[] castlingAbilities, final byte[] passingPawnCoordinates) {
		try {
			final Constructor<T> constructor = boardClass.getConstructor(char[][].class, short.class, short.class, boolean[].class, byte[].class);
			return constructor.newInstance(pieceMatrix, moveClock, reversibleMoveClock, castlingAbilities, passingPawnCoordinates);
		} catch (final InvocationTargetException exception) {
			final Throwable cause = exception.getCause();
			if (cause instanceof Error) throw (Error) cause;
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			throw new AssertionError();
		} catch (final Exception exception) {
			throw new IllegalArgumentException();
		}
	}
}