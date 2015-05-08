package de.htw.ds.board;

import java.util.Collection;
import java.util.stream.Stream;
import de.sb.java.TypeMetadata;


/**
 * Abstract interface for checker-board games played by two opponents, on boards of varying size.
 * Note that this interface does not make any assumptions on the game type except that it has to be
 * a game played between two sides, and on a board whose rank and file count does not exceed
 * {@code 127}.
 * @param <T> the type of the board's pieces
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface Board<T extends PieceType> extends Cloneable {

	/**
	 * Returns a clone of this board.
	 * @return the clone
	 */
	Board<T> clone ();


	/**
	 * Returns a stream for the pieces on this board.
	 * @return the stream
	 */
	Stream<Piece<T>> pieceStream ();


	/**
	 * Returns the number of ranks on the board.
	 * @return the rank count
	 */
	byte getRankCount ();


	/**
	 * Returns the number of files on the board.
	 * @return the file count
	 */
	byte getFileCount ();


	/**
	 * Returns the total number of (half) moves performed.
	 * @return the move clock
	 */
	short getMoveClock ();


	/**
	 * Returns the number of (half) moves performed since the last irreversible move, i.e. the
	 * latest one where the former board state cannot be recovered by simply moving back.
	 * @return the reversible move clock
	 */
	short getReversibleMoveClock ();


	/**
	 * Returns whether or not white is active
	 * @return {@code true} if white is active, {@code false} otherwise
	 */
	boolean isWhiteActive ();


	/**
	 * Returns the board piece at the given rank and file.
	 * @param rank the board rank
	 * @param file the board file
	 * @return the board piece, or {@code null} for none
	 * @throws IllegalArgumentException if the given rank or file is out of range
	 */
	Piece<T> getPiece (byte rank, byte file);


	/**
	 * Returns the current board rating in cents, with matching sides rating zero by definition. The
	 * following values have a special meaning whenever there are no more valid moves:
	 * <ul>
	 * <li>{@code +}{@linkplain Integer#MAX_VALUE} indicates white has won</li>
	 * <li>{@code 0} indicates the game ended in a draw</li>
	 * <li>{@code -}{@linkplain Integer#MAX_VALUE} indicates black has won</li>
	 * </ul>
	 * @return the current board rating
	 */
	int getRating ();


	/**
	 * Returns all candidate moves of the active side.
	 * @return the moves, each consisting of possibly multiple motions
	 */
	Collection<AbsoluteMotion[]> getCandidateMoves ();


	/**
	 * Returns whether or not the given side can capture a (possibly temporal) piece on the given
	 * position if it were to move. Note that this operation can be based on reverse motion checks,
	 * which provides improved analysis performance compared to checking all possible board moves.
	 * @param rank the board rank
	 * @param file the board file
	 * @param white whether or not checking for white threats
	 * @return whether or not a (possibly temporal) opponent piece can be captured on the given
	 *         position by the given side if it were to move
	 * @throws IllegalArgumentException if the given rank or file is out of range
	 */
	boolean isThreatened (byte rank, byte file, boolean white);


	/**
	 * Moves a piece, modifying the board in the process. Note that this method does not check the
	 * validity of the given move, only that there is an active piece to be moved. Note that this
	 * operation is not thread safe.
	 * @return the move, consisting of possibly multiple motions
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if more or less motions are passed than the game permits, or
	 *         if any position is out of range, or if the piece to be moved is not active
	 */
	void move (AbsoluteMotion... move);


	/**
	 * Recursively analyzes this board for candidate moves and counter moves up until the given
	 * search depth, beginning with this board's active color, and implementing the minimax game
	 * theory principle. The result contains the next {@code depth} (half) moves predicted given
	 * optimum play from both sides, and the board rating after performing said moves. If the game
	 * is expected to end shortly (or has ended), the result will contain fewer (no) moves, and a
	 * rating that indicates if the game is a win for a particular side, or a draw. Note that this
	 * operation is designed to be interruptible.
	 * @param depth the search depth in half moves
	 * @return the prediction for the next {@code depth} (half) moves, and the board rating after
	 *         performing said moves
	 * @throws IllegalArgumentException if the given depth is negative
	 * @throws InterruptedException if this operation is interrupted by another thread
	 */
	Prediction analyze (int depth) throws InterruptedException;


	/**
	 * Recursively analyzes this board for candidate moves and counter moves up until the given
	 * search depth, beginning with this board's active color, and implementing the minimax game
	 * theory principle. The result contains the next {@code depth} (half) moves predicted given
	 * optimum play from both sides, and the board rating after performing said moves. If the game
	 * is expected to end shortly (or has ended), the result will contain fewer (no) moves, and a
	 * rating that indicates if the game is a win for a particular side, or a draw. Note that this
	 * operation is designed not to be interruptible.
	 * @param depth the search depth in half moves
	 * @return the prediction for the next {@code depth} (half) moves, and the board rating after
	 *         performing said moves
	 * @throws IllegalArgumentException if the given depth is negative
	 */
	Prediction analyzeUninterruptibly (int depth);


	/**
	 * Marshals this board's internal state into an implementation-neutral X-FEN representation.
	 * @return the X-FEN state
	 */
	String getXfenState ();


	/**
	 * Alters this board's internal state by unmarshaling the given implementation-neutral X-FEN
	 * representation. If the X-FEN state is {@code null}, the internal state is set to a fixed
	 * piece layout suitable for this board's dimensions. Note that this operation is not thread
	 * safe.
	 * @param xfenState the X-FEN state, or {@code null} for default
	 * @throws IllegalStateException if the given X-FEN state represents a board with incompatible
	 *         dimensions, or if no state is given and there is no typical piece layout for this
	 *         board's current dimensions
	 */
	void setXfenState (String xfenState);


	/**
	 * Returns the number of board fields corresponding to the given rank and file count.
	 * @param rankCount the number of ranks on a board
	 * @param fileCount the number of files on a board
	 * @return the field count
	 */
	static int fieldCount (final int rankCount, final int fileCount) {
		return rankCount * fileCount;
	}


	/**
	 * Returns {@code true} if the given coordinates target a white field, or {@code false} if they
	 * target a black field.
	 * @param rank the board rank
	 * @param file the board file
	 * @return whether or not the given coordinates target a white field
	 */
	static boolean whiteField (final int rank, final int file) {
		return ((rank ^ file) & 1) == 1;
	}


	/**
	 * Returns the position corresponding to the given coordinates.
	 * @param rank the rank
	 * @param file the file
	 * @param fileCount the number of files on a board
	 * @return the corresponding position
	 */
	static int coordinatesToPosition (final int rank, final int file, final int fileCount) {
		return rank * fileCount + file;
	}


	/**
	 * Returns the coordinates corresponding to the given position.
	 * @param position the position
	 * @param fileCount the number of files on a board
	 * @return the corresponding coordinates as a rank&file-array
	 */
	static int[] positionToCoordinates (final int position, final int fileCount) {
		final int rank = position / fileCount;
		final int file = position - rank * fileCount;
		return new int[] { rank, file };
	}


	/**
	 * Returns the alias corresponding to the given coordinates, consisting of 1-2 letters
	 * representing the file, followed by the one-based index of the rank.
	 * @param rank the rank
	 * @param file the file
	 * @param fileCount the file count
	 * @return the corresponding alias
	 * @throws IllegalArgumentException if any of the given arguments is out of range
	 */
	static String coordinatesToAlias (final byte rank, final byte file, final byte fileCount) {
		if (rank < 0 | file < 0 | file >= fileCount) throw new IllegalArgumentException();

		final char[] fileAlias;
		if (fileCount <= 26) {
			fileAlias = new char[] { (char) ('a' + file) };
		} else {
			fileAlias = new char[2];
			fileAlias[0] = (char) ('a' + (file / 26));
			fileAlias[1] = (char) ('a' + (file % 26));
		}
		return new String(fileAlias) + Integer.toString(rank + 1);
	}


	/**
	 * Returns the coordinates corresponding to the given alias, consisting of 1-2 letters
	 * representing the file, followed by the one-based index of the rank.
	 * @param alias the alias
	 * @param fileCount the file count
	 * @return the corresponding coordinates as a rank&file-array
	 * @throws NullPointerException if the given alias is {@code null}
	 * @throws IllegalArgumentException if the given alias is malformed or illegal
	 */
	static byte[] aliasToCoordinates (String alias, final byte fileCount) {
		alias = alias.toLowerCase();
		try {
			final int digitOffset = fileCount <= 26 ? 1 : 2;
			final byte rank = (byte) (Byte.parseByte(alias.substring(digitOffset)) - 1);
			final byte file = digitOffset == 1
				? (byte) (alias.charAt(0) - 'a')
				: (byte) (26 * (alias.charAt(0) - 'a') + (alias.charAt(1) - 'a'));

			if (rank < 0 | file < 0 | file >= fileCount) throw new IllegalArgumentException();
			return new byte[] { rank, file };
		} catch (final IndexOutOfBoundsException | NumberFormatException exception) {
			throw new IllegalArgumentException();
		}
	}
}