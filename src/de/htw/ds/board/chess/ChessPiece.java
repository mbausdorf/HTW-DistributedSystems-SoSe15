package de.htw.ds.board.chess;

import static de.htw.ds.board.chess.ChessPieceType.PAWN;
import static de.htw.ds.board.chess.ChessPieceType.KING;
import java.util.ArrayList;
import java.util.List;
import de.htw.ds.board.AbstractPiece;
import de.htw.ds.board.Board;
import de.htw.ds.board.RelativeMotion;
import de.htw.ds.board.AbsoluteMotion;
import de.htw.ds.board.Piece;
import de.htw.ds.board.PieceType;
import de.sb.java.TypeMetadata;
import de.sb.java.util.BitArrays;


/**
 * Instances of this class model positional chess pieces. Note that this class is declared final
 * because it's conception assumes a limited amount of well known instances, similarly to an enum;
 * this condition would be violated if subclasses create additional ones.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class ChessPiece extends AbstractPiece<ChessPieceType> {
	static private final int COLOR_COUNT = 2;
	static private final int TYPE_ORDINAL_BIT_LENGTH = Byte.SIZE >> 1;
	static private final int COORDINATE_BIT_LENGTH = Byte.SIZE - 1;
	@SuppressWarnings("unchecked")
	static private final Piece<ChessPieceType>[][][][] PIECE_CACHE = new Piece[1 << (COORDINATE_BIT_LENGTH << 1)][][][];


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
	private ChessPiece (final ChessPieceType type, final boolean white, final byte rank, final byte file, final byte rankCount, final byte fileCount) {
		super(type, white, rank, file, rankCount, fileCount);
	}


	/**
	 * {@inheritDoc}
	 */
	public int ordinal () {
		final int typeBitShift = COORDINATE_BIT_LENGTH << 1;
		final int colorBitShift = typeBitShift + TYPE_ORDINAL_BIT_LENGTH;
		return ((this.isWhite() ? 0 : 1) << colorBitShift) | (this.getType().ordinal() << typeBitShift) | this.getPosition();
	}


	/**
	 * {@inheritDoc}
	 */
	protected AbsoluteMotion[][] calculateMoves () {
		final List<AbsoluteMotion[]> result = new ArrayList<>();

		for (final RelativeMotion motion : this.getType().getMotions()) {
			if (motion.getRankDelta() == 0 & motion.getFileDelta() == 0) continue;
			if (motion.isTouchForbidden()) {
				if (this.getType() == PAWN) {
					if (this.getRankCount() <= 6 || (this.isWhite() ? this.getRank() : this.getReverseRank()) != 1) continue;
				} else if (this.getType() == KING) {
					if (this.getFileCount() <= 6 || (this.isWhite() ? this.getRank() : this.getReverseRank()) != 0 || this.getFile() != (this.getFileCount() >> 1)) continue;
				}
			}

			final int direction = this.isWhite() ? +1 : -1;
			final int rankDelta = motion.getRankDelta() * direction;
			final int fileDelta = motion.getFileDelta();
			final AbsoluteMotion[] directedMoves;

			if (motion.isContinuous()) {
				final int targetRank = rankDelta == 0 ? Byte.MAX_VALUE : (rankDelta < 0 ? this.getRank() : this.getReverseRank());
				final int targetFile = fileDelta == 0 ? Byte.MAX_VALUE : (fileDelta < 0 ? this.getFile() : this.getReverseFile());
				directedMoves = new AbsoluteMotion[targetFile < targetRank ? targetFile : targetRank];

				for (int index = 0; index < directedMoves.length; ++index) {
					final byte sinkRank = (byte) (this.getRank() + (index + 1) * rankDelta);
					final byte sinkFile = (byte) (this.getFile() + (index + 1) * fileDelta);
					directedMoves[index] = new AbsoluteMotion(this.getRank(), this.getFile(), (byte) sinkRank, (byte) sinkFile, motion.getFlags());
				}
			} else {
				final int sinkRank = this.getRank() + rankDelta;
				final int sinkFile = this.getFile() + fileDelta;
				directedMoves = (sinkRank >= 0 & sinkRank < this.getRankCount() & sinkFile >= 0 & sinkFile < this.getFileCount())
					? new AbsoluteMotion[] { new AbsoluteMotion(this.getRank(), this.getFile(), (byte) sinkRank, (byte) sinkFile, motion.getFlags()) }
					: new AbsoluteMotion[] {};
			}
			if (directedMoves.length > 0) result.add(directedMoves);
		}

		return result.toArray(new AbsoluteMotion[0][]);
	}


	/**
	 * {@inheritDoc}
	 */
	protected int calculateRating () {
		int rating = this.getType().getRating();

		switch (this.getType()) {
		// prefer centralized pawn advancement by increasing their rating up to 100%
			case PAWN: {
				final byte halfFileCount = (byte) (this.getFileCount() >> 1);
				final int advancement = (this.isWhite() ? this.getRank() : this.getReverseRank()) - 1;
				final int centralization = this.getFile() < halfFileCount ? this.getFile() : this.getReverseFile();
				final int normalizer = this.getRankCount() + halfFileCount - 4;
				rating = rating * (normalizer + advancement + centralization) / normalizer;
				break;
			}

			case KING: { // prefer king on base row
				if ((this.isWhite() ? this.getRank() : this.getReverseRank()) == 0) {
					rating += 2;
				}
				break;
			}

			// adjust rating for number of reachable squares
			default: {
				final int sinkPositionCount = (int) BitArrays.cardinality(this.getMotionMap(), 0, this.getFieldCount());
				rating = rating + 50 * sinkPositionCount / (this.getRankCount() + this.getFileCount() - 2);
				break;
			}
		}

		return this.isWhite() ? +rating : -rating;
	}


	/**
	 * Returns the board ordinal for the given rank and file count.
	 * @param rankCount the number of ranks on a board
	 * @param fileCount the number of files on a board
	 * @return the board ordinal
	 */
	static private int boardOrdinal (final byte rankCount, final byte fileCount) {
		assert rankCount >= 0 & fileCount >= 0;
		return (rankCount << COORDINATE_BIT_LENGTH) | fileCount;
	}


	/**
	 * Initializes the piece cache for the given rank and file count, if necessary.
	 * @param rankCount the number of ranks on the piece's boards
	 * @param fileCount the number of files on the piece's boards
	 * @throws IllegalArgumentException if the given rank or file count is negative
	 */
	@SuppressWarnings("unchecked")
	static private void initializePieceCache (final byte rankCount, final byte fileCount) {
		if (rankCount <= 0 | fileCount <= 0) throw new IllegalArgumentException();

		final int boardOrdinal = boardOrdinal(rankCount, fileCount);
		synchronized (PIECE_CACHE) {
			if (PIECE_CACHE[boardOrdinal] == null) {
				final int fieldCount = Board.fieldCount(rankCount, fileCount);
				PIECE_CACHE[boardOrdinal] = new Piece[COLOR_COUNT][ChessPieceType.values().length][fieldCount];

				for (final ChessPieceType type : ChessPieceType.values()) {
					for (byte rank = 0; rank < rankCount; ++rank) {
						for (byte file = 0; file < fileCount; ++file) {
							final int position = Board.coordinatesToPosition(rank, file, fileCount);
							PIECE_CACHE[boardOrdinal][0][type.ordinal()][position] = new ChessPiece(type, true, rank, file, rankCount, fileCount);
							PIECE_CACHE[boardOrdinal][1][type.ordinal()][position] = new ChessPiece(type, false, rank, file, rankCount, fileCount);
						}
					}
				}
			}
		}
	}


	/**
	 * Returns the piece cache for the given rank and file count, organized by:
	 * <ul>
	 * <li>position</li>
	 * <li>piece color (white 0, black 1)</li>
	 * <li>piece type ordinal</li>
	 * <ul>
	 * Note that the piece cache of the given dimensions is initialized if necessary.
	 * @param rankCount the number of ranks on the piece's boards
	 * @param fileCount the number of files on the piece's boards
	 * @return the piece cache
	 * @throws IllegalArgumentException if the given rankCount or fileCount is negative
	 */
	static public Piece<ChessPieceType>[][][] values (final byte rankCount, final byte fileCount) {
		if (rankCount <= 0 | fileCount <= 0) throw new IllegalArgumentException();

		final int boardOrdinal = boardOrdinal(rankCount, fileCount);
		if (PIECE_CACHE[boardOrdinal] == null) initializePieceCache(rankCount, fileCount);
		return PIECE_CACHE[boardOrdinal];
	}


	/**
	 * Returns a piece instance from the piece cache suitable for the given color, type and
	 * position.
	 * @param type the piece type
	 * @param white {@code true} for white, {@code false} for black
	 * @param rank the rank
	 * @param file the file
	 * @param rankCount the number of ranks on the piece's boards
	 * @param fileCount the number of files on the piece's boards
	 * @return the cached piece
	 * @throws NullPointerException if the given color or type is {@code null}
	 * @throws IllegalArgumentException if the given rank or file are strictly negative, or if they
	 *         equal or exceed their corresponding rank count or file count
	 */
	static public Piece<ChessPieceType> valueOf (final boolean white, final ChessPieceType type, final byte rank, final byte file, final byte rankCount, final byte fileCount) {
		if (rank < 0 | file < 0 | rank >= rankCount | file >= fileCount) throw new IllegalArgumentException();

		final int position = Board.coordinatesToPosition(rank, file, fileCount);
		return values(rankCount, fileCount)[white ? 0 : 1][type.ordinal()][position];
	}


	/**
	 * Returns a piece instance from the piece cache suitable for the given alias and position.
	 * @param alias the character alias
	 * @param rank the rank
	 * @param file the file
	 * @param rankCount the number of ranks on the piece's boards
	 * @param fileCount the number of files on the piece's boards
	 * @return the cached piece
	 * @throws IllegalArgumentException if the given rank or file are strictly negative, or if they
	 *         equal or exceed their corresponding rank count or file count
	 */
	static public Piece<ChessPieceType> valueOf (final char alias, final byte rank, final byte file, final byte rankCount, final byte fileCount) {
		if (rank < 0 | file < 0 | rank >= rankCount | file >= fileCount) throw new IllegalArgumentException();

		final int position = Board.coordinatesToPosition(rank, file, fileCount);
		final int colorOrdinal = Character.isUpperCase(alias) ? 0 : 1;
		final PieceType type = ChessPieceType.valueOf(Character.toUpperCase(alias));
		return values(rankCount, fileCount)[colorOrdinal][type.ordinal()][position];
	}
}