package de.htw.ds.board.chess;

import static de.htw.ds.board.chess.ChessPieceType.BISHOP;
import static de.htw.ds.board.chess.ChessPieceType.KING;
import static de.htw.ds.board.chess.ChessPieceType.KNIGHT;
import static de.htw.ds.board.chess.ChessPieceType.PAWN;
import static de.htw.ds.board.chess.ChessPieceType.QUEEN;
import static de.htw.ds.board.chess.ChessPieceType.ROOK;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import de.htw.ds.board.AbsoluteMotion;
import de.htw.ds.board.AbstractTableBoard;
import de.htw.ds.board.Board;
import de.htw.ds.board.Piece;
import de.htw.ds.board.Prediction;
import de.sb.java.TypeMetadata;


/**
 * Instances of this class model chess boards based on a single-dimensional table of pre-cached
 * positional pieces, with three extra fields for quick king and passing pawn lookup.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class ChessTableBoard extends AbstractTableBoard<ChessPieceType> implements ChessBoard {

	private volatile byte castlingAbilities;
	private volatile Piece<ChessPieceType> whiteKing;
	private volatile Piece<ChessPieceType> blackKing;
	private volatile Piece<ChessPieceType> passingPawn;


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
	public ChessTableBoard (final byte rankCount, final byte fileCount, final short moveClock, final short reversibleMoveClock) {
		super(rankCount, fileCount, moveClock, reversibleMoveClock);
		if (this.rankCount < MIN_RANK_COUNT | this.fileCount < MIN_FILE_COUNT | this.reversibleMoveClock >= 100) throw new IllegalArgumentException();
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
	public ChessTableBoard (final char[][] pieceMatrix, final short moveClock, final short reversibleMoveClock, final boolean[] castlingAbilities, final byte[] passingPawnCoordinates) {
		super(toByte(pieceMatrix.length), toByte(pieceMatrix.length == 0 ? 0 : pieceMatrix[0].length), moveClock, reversibleMoveClock);
		if (this.rankCount < MIN_RANK_COUNT | this.fileCount < MIN_FILE_COUNT | this.reversibleMoveClock >= 100) throw new IllegalArgumentException();

		for (byte rank = 0; rank < this.rankCount; ++rank) {
			if (pieceMatrix[rank].length != this.fileCount) throw new IllegalArgumentException();
			for (byte file = 0; file < this.fileCount; ++file) {
				final char alias = pieceMatrix[rank][file];
				if (alias != 0) {
					final Piece<ChessPieceType> piece = ChessPiece.valueOf(alias, rank, file, this.rankCount, this.fileCount);
					switch (piece.getType()) {
						case PAWN:
							if (rank == 0 | rank == (this.rankCount - 1)) throw new IllegalArgumentException();
							break;
						case KING:
							if (piece.isWhite()) {
								if (this.whiteKing != null) throw new IllegalArgumentException();
								this.whiteKing = piece;
							} else {
								if (this.blackKing != null) throw new IllegalArgumentException();
								this.blackKing = piece;
							}
							break;
						default:
							break;
					}
					this.pieces[Board.coordinatesToPosition(rank, file, this.fileCount)] = piece;
				}
			}
		}
		if (this.whiteKing == null | this.blackKing == null) throw new IllegalArgumentException();

		if (castlingAbilities.length != 4) throw new IllegalArgumentException();
		if (castlingAbilities[0]) {
			if (this.pieces[this.getLowerLeftPosition()] != ChessPiece.valueOf(true, ROOK, (byte) 0, (byte) 0, this.rankCount, this.fileCount)) throw new IllegalArgumentException();
			this.castlingAbilities |= MASK_CASTLE_WHITE_LEFT;
		}
		if (castlingAbilities[1]) {
			if (this.pieces[this.getLowerRightPosition()] != ChessPiece.valueOf(true, ROOK, (byte) 0, (byte) (this.fileCount - 1), this.rankCount, this.fileCount)) throw new IllegalArgumentException();
			this.castlingAbilities |= MASK_CASTLE_WHITE_RIGHT;
		}
		if (castlingAbilities[2]) {
			if (this.pieces[this.getUpperLeftPosition()] != ChessPiece.valueOf(false, ROOK, (byte) (this.rankCount - 1), (byte) 0, this.rankCount, this.fileCount)) throw new IllegalArgumentException();
			this.castlingAbilities |= MASK_CASTLE_BLACK_LEFT;
		}
		if (castlingAbilities[3]) {
			if (this.pieces[this.getUpperRightPosition()] != ChessPiece.valueOf(false, ROOK, (byte) (this.rankCount - 1), (byte) (this.fileCount - 1), this.rankCount, this.fileCount)) throw new IllegalArgumentException();
			this.castlingAbilities |= MASK_CASTLE_BLACK_RIGHT;
		}
		if ((castlingAbilities[0] | castlingAbilities[1]) && (this.whiteKing.getFile() != (this.fileCount >> 1) | this.whiteKing.getRank() != 0)) throw new IllegalArgumentException();
		if ((castlingAbilities[2] | castlingAbilities[3]) && (this.blackKing.getFile() != (this.fileCount >> 1) | this.blackKing.getRank() != (this.rankCount - 1))) throw new IllegalArgumentException();

		if (passingPawnCoordinates != null) {
			final byte passedRank = passingPawnCoordinates[0], rank = (byte) (passedRank + (this.isWhiteActive() ? -1 : +1)), file = passingPawnCoordinates[1];
			this.passingPawn = ChessPiece.valueOf(!this.isWhiteActive(), PAWN, passedRank, file, this.rankCount, this.fileCount);
			if (this.pieces[this.passingPawn.getPosition()] != null) throw new IllegalArgumentException();
			if (this.getPiece(rank, file) != ChessPiece.valueOf(!this.isWhiteActive(), PAWN, rank, file, this.rankCount, this.fileCount)) throw new IllegalArgumentException();
		}

		final Piece<ChessPieceType> passiveKing = this.isWhiteActive() ? this.blackKing : this.whiteKing;
		if (this.isThreatened(passiveKing.getRank(), passiveKing.getFile(), this.isWhiteActive())) throw new IllegalArgumentException();
	}


	/**
	 * {@inheritDoc}
	 */
	public byte getCastlingAbilities () {
		return this.castlingAbilities;
	}


	/**
	 * {@inheritDoc}
	 */
	public Piece<ChessPieceType> getKing (final boolean white) {
		return white ? this.whiteKing : this.blackKing;
	}


	/**
	 * {@inheritDoc}
	 */
	public Piece<ChessPieceType> getPassingPawn () {
		return this.passingPawn;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getRating () {
		if (this.whiteKing == null) return -Integer.MAX_VALUE;
		if (this.blackKing == null) return +Integer.MAX_VALUE;
		return super.getRating();
	}


	/**
	 * {@inheritDoc}
	 */
	public List<AbsoluteMotion[]> getCandidateMoves () {
		final boolean whiteActive = this.isWhiteActive();
		final List<AbsoluteMotion[]> candidatesMoves = new ArrayList<>(256);
		if (this.getKing(whiteActive) == null) return candidatesMoves;

		boolean captureKingMode = false;
		final Stream<Piece<ChessPieceType>> stream = this.pieceStream().filter((Piece<ChessPieceType> piece) -> piece.isWhite() == whiteActive);
		for (final Piece<ChessPieceType> piece : (Iterable<Piece<ChessPieceType>>) () -> stream.iterator()) {
			captureKingMode = this.collectCandidateMoves(candidatesMoves, piece, captureKingMode);
		}
		return candidatesMoves;
	}


	/**
	 * Collects the candidate moves of the given piece into the given collection. Returns
	 * {@code true} if further candidate moves must capture the opposing king, {@code false}
	 * otherwise.
	 * @param moves the collected moves, each consisting of possibly multiple motions
	 * @param activePiece the moving piece
	 * @param captureKingMode whether or not the opposing king must to be captured for moves to be
	 *        valid
	 * @return whether or not the opposing king needs to be captured for moves to be valid
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	private boolean collectCandidateMoves (final Collection<AbsoluteMotion[]> moves, final Piece<ChessPieceType> activePiece, boolean captureKingMode) {
		final boolean whiteActive = activePiece.isWhite();
		if (this.reversibleMoveClock > 100 && activePiece.getType() != PAWN) return true;

		for (final AbsoluteMotion[] directedMotions : activePiece.getMotions()) {
			for (final AbsoluteMotion motion : directedMotions) {
				final Piece<ChessPieceType> sinkPiece = this.getPiece(motion.getSinkRank(), motion.getSinkFile());
				if (motion.isCaptureForbidden() & (captureKingMode | sinkPiece != null)) break;

				if (motion.isTouchForbidden()) {
					assert directedMotions.length == 1 & motion.isCaptureForbidden();
					final byte interRank = (byte) ((motion.getSourceRank() + motion.getSinkRank()) >> 1);
					final byte interFile = (byte) ((motion.getSourceFile() + motion.getSinkFile()) >> 1);
					if (sinkPiece != null | this.getPiece(interRank, interFile) != null) break;

					switch (activePiece.getType()) {
						case PAWN: {
							moves.add(directedMotions);
							break;
						}
						case KING: {
							final byte rookFile = motion.getSinkFile() < motion.getSourceFile() ? 0 : (byte) (this.fileCount - 1);
							final int castlingMask = motion.getSinkFile() < motion.getSourceFile()
								? (whiteActive ? MASK_CASTLE_WHITE_LEFT : MASK_CASTLE_BLACK_LEFT)
								: (whiteActive ? MASK_CASTLE_WHITE_RIGHT : MASK_CASTLE_BLACK_RIGHT);
							boolean castlingPermitted = (this.castlingAbilities & castlingMask) != 0;

							for (byte file = (byte) (rookFile + 1); castlingPermitted & (file < motion.getSinkFile()); ++file) {
								castlingPermitted &= this.getPiece(motion.getSourceRank(), file) == null;
							}
							for (byte file = (byte) (motion.getSinkFile() + 1); castlingPermitted & (file < rookFile); ++file) {
								castlingPermitted &= this.getPiece(motion.getSourceRank(), file) == null;
							}
							for (byte file = rookFile; castlingPermitted & (file <= motion.getSourceFile()); ++file) {
								castlingPermitted &= !this.isThreatened(motion.getSourceRank(), file, !whiteActive);
							}
							for (byte file = motion.getSourceFile(); castlingPermitted & (file <= rookFile); ++file) {
								castlingPermitted &= !this.isThreatened(motion.getSourceRank(), file, !whiteActive);
							}
							if (castlingPermitted) moves.add(directedMotions);
							break;
						}
						default: {
							throw new AssertionError();
						}
					}
				} else {
					if (sinkPiece != null) {
						if (sinkPiece.isWhite() == whiteActive | motion.isCaptureForbidden()) break;
						if (sinkPiece.getType() != KING) {
							if (captureKingMode) break;
						} else {
							if (!captureKingMode) moves.clear();
							captureKingMode = true;
						}
						moves.add(new AbsoluteMotion[] { motion });
						break;
					}

					if ((activePiece.getType() == PAWN & this.passingPawn != null) && (this.passingPawn.getRank() == motion.getSinkRank() & this.passingPawn.getFile() == motion.getSinkFile())) {
						assert directedMotions.length == 1;
						if (!captureKingMode & !motion.isCaptureForbidden()) {
							moves.add(directedMotions);
						}
						break;
					}

					if (!motion.isCaptureRequired()) {
						moves.add(new AbsoluteMotion[] { motion });
					}
				}
			}
		}

		return captureKingMode;
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	public boolean isThreatened (final byte rank, final byte file, final boolean white) {
		if (rank < 0 | rank >= this.rankCount | file < 0 | file >= this.fileCount) throw new IllegalArgumentException();
		final Piece<ChessPieceType>[][] pieceCache = ChessPiece.values(this.rankCount, this.fileCount)[1 - (this.moveClock & 1)];
		final int position = Board.coordinatesToPosition(rank, file, this.fileCount);
		Piece<ChessPieceType> virtualPiece;

		// check for pieces that capture like knights, i.e. knight, archbishop, chancellor, empress.
		virtualPiece = pieceCache[KNIGHT.ordinal()][position];
		for (final AbsoluteMotion[] directedMoves : virtualPiece.getMotions()) {
			for (final AbsoluteMotion move : directedMoves) {
				final Piece<ChessPieceType> piece = this.getPiece(move.getSinkRank(), move.getSinkFile());
				if (piece != null) {
					if (piece.isWhite() == white) {
						switch (piece.getType()) {
							case KNIGHT:
							case CHANCELLOR:
							case ARCHBISHOP:
							case EMPRESS:
								return true;
							default:
								break;
						}
					}
					break;
				}
			}
		}

		// check for pieces that capture like bishops, i.e. bishop, archbishop, queen, empress, and
		// king/pawn (first sink position only). Note that pawn capture is additionally constrained by direction.
		virtualPiece = pieceCache[BISHOP.ordinal()][position];
		for (final AbsoluteMotion[] directedMoves : virtualPiece.getMotions()) {
			for (int index = 0; index < directedMoves.length; ++index) {
				final AbsoluteMotion move = directedMoves[index];
				final Piece<ChessPieceType> piece = this.getPiece(move.getSinkRank(), move.getSinkFile());
				if (piece != null) {
					if (piece.isWhite() == white) {
						switch (piece.getType()) {
							case BISHOP:
							case ARCHBISHOP:
							case QUEEN:
							case EMPRESS:
								return true;
							case KING:
								if (index == 0) return true;
								break;
							case PAWN:
								if (index == 0 & (white ^ (move.getSourceRank() < move.getSinkRank()))) return true;
								break;
							default:
								break;
						}
					}
					break;
				}
			}
		}

		// check for pieces that capture like rooks, i.e. rook, chancellor, queen, empress, and
		// king (first sink position only).
		virtualPiece = pieceCache[ROOK.ordinal()][position];
		for (final AbsoluteMotion[] directedMoves : virtualPiece.getMotions()) {
			for (int index = 0; index < directedMoves.length; ++index) {
				final AbsoluteMotion move = directedMoves[index];
				final Piece<ChessPieceType> piece = this.getPiece(move.getSinkRank(), move.getSinkFile());
				if (piece != null) {
					if (piece.isWhite() == white) {
						switch (piece.getType()) {
							case ROOK:
							case CHANCELLOR:
							case QUEEN:
							case EMPRESS:
								return true;
							case KING:
								if (index == 0) return true;
								break;
							default:
								break;
						}
					}
					break;
				}
			}
		}

		return false;
	}


	/**
	 * {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IllegalArgumentException if more or less than one motion is passed, or if any
	 *         position is out of range, or if the piece to be moved is not active
	 */
	public void move (final AbsoluteMotion... move) {
		if (move.length == 0 | move.length > 2) throw new IllegalArgumentException();
		final AbsoluteMotion motion = move[0];

		final int colorOrdinal = this.moveClock & 1, rookOffset = (this.rankCount - 1) * this.fileCount;
		final Piece<ChessPieceType>[][][] pieceCache = ChessPiece.values(this.rankCount, this.fileCount);
		final Piece<ChessPieceType> activeLeftUntouchedRook = pieceCache[colorOrdinal][ROOK.ordinal()][colorOrdinal * rookOffset];
		final Piece<ChessPieceType> activeRightUntouchedRook = pieceCache[colorOrdinal][ROOK.ordinal()][colorOrdinal * rookOffset + this.fileCount - 1];
		final Piece<ChessPieceType> passiveLeftUntouchedRook = pieceCache[1 - colorOrdinal][ROOK.ordinal()][(1 - colorOrdinal) * rookOffset];
		final Piece<ChessPieceType> passiveRightUntouchedRook = pieceCache[1 - colorOrdinal][ROOK.ordinal()][(1 - colorOrdinal) * rookOffset + this.fileCount - 1];
		final Piece<ChessPieceType> passingPawn = this.passingPawn;
		final boolean whiteActive = this.isWhiteActive();
		this.passingPawn = null;
		this.reversibleMoveClock += 1;
		this.moveClock += 1;

		final int sourcePosition = Board.coordinatesToPosition(motion.getSourceRank(), motion.getSourceFile(), this.fileCount);
		final int sinkPosition = Board.coordinatesToPosition(motion.getSinkRank(), motion.getSinkFile(), this.fileCount);
		final Piece<ChessPieceType> sourcePiece = this.pieces[sourcePosition];
		final Piece<ChessPieceType> sinkPiece = this.pieces[sinkPosition];
		if (sourcePiece == null || sourcePiece.isWhite() != whiteActive) throw new IllegalArgumentException();

		ChessPieceType sourceType = sourcePiece.getType();
		switch (sourceType) {
			case PAWN:
				this.reversibleMoveClock = 0;
				if (motion.isCaptureForbidden() & motion.isTouchForbidden()) {
					this.passingPawn = pieceCache[colorOrdinal][PAWN.ordinal()][(sourcePosition + sinkPosition) >> 1];
				} else if ((motion.isCaptureRequired() & passingPawn != null) && (passingPawn.getRank() == motion.getSinkRank() & passingPawn.getFile() == motion.getSinkFile())) {
					this.pieces[sinkPosition + (whiteActive ? -this.fileCount : +this.fileCount)] = null;
				} else if (motion.getSinkRank() == 0 | motion.getSinkRank() == this.rankCount - 1) {
					sourceType = QUEEN;
				}
				break;
			case ROOK:
				if (sourcePiece == activeLeftUntouchedRook) {
					this.castlingAbilities &= (whiteActive ? ~MASK_CASTLE_WHITE_LEFT : ~MASK_CASTLE_BLACK_LEFT);
				} else if (sourcePiece == activeRightUntouchedRook) {
					this.castlingAbilities &= (whiteActive ? ~MASK_CASTLE_WHITE_RIGHT : ~MASK_CASTLE_BLACK_RIGHT);
				}
				break;
			case KING:
				if (motion.isTouchForbidden()) {
					final Piece<ChessPieceType> rook = sinkPosition < sourcePosition ? activeLeftUntouchedRook : activeRightUntouchedRook;
					this.pieces[rook.getPosition()] = null;
					final int rookSinkPosition = (sourcePosition + sinkPosition) >> 1;
					this.pieces[rookSinkPosition] = pieceCache[colorOrdinal][ROOK.ordinal()][rookSinkPosition];
				}
				this.castlingAbilities &= whiteActive
					? ~(MASK_CASTLE_WHITE_LEFT | MASK_CASTLE_WHITE_RIGHT)
					: ~(MASK_CASTLE_BLACK_LEFT | MASK_CASTLE_BLACK_RIGHT);
				final Piece<ChessPieceType> king = pieceCache[colorOrdinal][KING.ordinal()][sinkPosition];
				if (whiteActive) { this.whiteKing = king; } else { this.blackKing = king; }
				break;
			default:
				break;
		}

		if (sinkPiece != null) {
			if (sinkPiece.isWhite() == whiteActive) throw new IllegalArgumentException();
			this.reversibleMoveClock = 0;

			switch (sinkPiece.getType()) {
				case KING:
					this.castlingAbilities &= whiteActive
						? ~(MASK_CASTLE_BLACK_LEFT | MASK_CASTLE_BLACK_RIGHT)
						: ~(MASK_CASTLE_WHITE_LEFT | MASK_CASTLE_WHITE_RIGHT);
					if (whiteActive) { this.blackKing = null; } else { this.whiteKing = null; }
					break;
				case ROOK:
					if (sinkPiece == passiveLeftUntouchedRook) {
						this.castlingAbilities &= (whiteActive ? ~MASK_CASTLE_BLACK_LEFT : ~MASK_CASTLE_WHITE_LEFT);
					} else if (sinkPiece == passiveRightUntouchedRook) {
						this.castlingAbilities &= (whiteActive ? ~MASK_CASTLE_BLACK_RIGHT : ~MASK_CASTLE_WHITE_RIGHT);
					}
					break;
				default:
					break;
			}
		}

		this.pieces[sourcePosition] = null;
		this.pieces[sinkPosition] = pieceCache[colorOrdinal][sourceType.ordinal()][sinkPosition];
		return;
	}


	/**
	 * {@inheritDoc} Note that this implementation is single-threaded.
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws InterruptedException {@inheritDoc}
	 */
	@Override
	protected Prediction analyzeRecursively (final int depth) throws InterruptedException {
		if (depth <= 0) throw new IllegalArgumentException();

		final boolean whitePerspective = this.isWhiteActive();
		final List<Prediction> alternatives = new ArrayList<>();

		final Collection<AbsoluteMotion[]> moves = this.getCandidateMoves();
		for (final AbsoluteMotion[] move : moves) {
			final Prediction prediction = this.analyzeRecursively(move, depth);
			if (alternatives.isEmpty()) {
				alternatives.add(prediction);
			} else {
				final Comparator<Prediction> comparator = whitePerspective ? WHITE_PREDICTION_COMPARATOR : BLACK_PREDICTION_COMPARATOR;
				final int compare = comparator.compare(prediction, alternatives.get(0));
				if (compare > 0) alternatives.clear();
				if (compare >= 0) alternatives.add(prediction);
			}
		}

		// randomly select one of the equally rated prediction alternatives
		final Prediction prediction = alternatives.isEmpty()
			? null
			: alternatives.get(ThreadLocalRandom.current().nextInt(alternatives.size()));
		if (prediction != null && !prediction.getMoveSequence().isEmpty()) return prediction;

		// distinguish check mate (loss) and stale mate (draw)
		final Piece<ChessPieceType> king = this.getKing(whitePerspective);
		return king == null || this.isThreatened(king.getRank(), king.getFile(), !whitePerspective)
			? new Prediction(whitePerspective ? -Integer.MAX_VALUE : +Integer.MAX_VALUE)
			: new Prediction(0);
	}


	/**
	 * Performs the given move on a clone of this board, and recursively analyzes the counter moves
	 * to it up until the given search depth, implementing the minimax game theory principle. The
	 * result contains the next {@code depth} (half) moves predicted given optimum play from both
	 * sides, and the board rating after performing said moves.
	 * @param move the move to be analyzed
	 * @param depth the search depth in half moves
	 * @return the prediction for the next {@code depth} (half) moves including the given one, and
	 *         the board rating after performing said moves
	 * @throws NullPointerException if the given move is {@code null}
	 * @throws IllegalArgumentException if the given depth is negative
	 * @throws InterruptedException if this operation is interrupted by another thread
	 */
	protected Prediction analyzeRecursively (final AbsoluteMotion[] move, final int depth) throws InterruptedException {
		if (depth <= 0) throw new IllegalArgumentException();

		final boolean whitePerspective = this.isWhiteActive();
		final ChessTableBoard clonedBoard = (ChessTableBoard) this.clone();
		clonedBoard.move(move);

		// avoid moving into check
		final Piece<ChessPieceType> king = clonedBoard.getKing(whitePerspective);
		if (king == null || clonedBoard.isThreatened(king.getRank(), king.getFile(), !whitePerspective)) {
			return new Prediction(whitePerspective ? -Integer.MAX_VALUE : +Integer.MAX_VALUE);
		}

		final Prediction prediction = depth == 1
			? new Prediction(clonedBoard.getRating())
			: clonedBoard.analyzeRecursively(depth - 1);
		prediction.getMoveSequence().addFirst(move);
		return prediction;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getXfenState () {
		return ChessXfenCodec.singleton().encode(this);
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException {@inheritDoc}
	 */
	public void setXfenState (String xfenState) {
		if (xfenState == null) {
			if (this.rankCount != 8 | this.fileCount < MIN_FILE_COUNT | this.fileCount > 10) throw new IllegalStateException();
			xfenState = DEFAULT_EIGHT_RANK_SETUPS[this.fileCount - MIN_FILE_COUNT];
		}

		final ChessTableBoard boardTemplate = ChessXfenCodec.singleton().decode(this.getClass(), xfenState);
		if (boardTemplate.rankCount != this.rankCount | boardTemplate.fileCount != this.fileCount) throw new IllegalStateException();

		this.whiteKing = boardTemplate.whiteKing;
		this.blackKing = boardTemplate.blackKing;
		this.passingPawn = boardTemplate.passingPawn;
		this.moveClock = boardTemplate.moveClock;
		this.reversibleMoveClock = boardTemplate.reversibleMoveClock;
		this.castlingAbilities = boardTemplate.castlingAbilities;
		System.arraycopy(boardTemplate.pieces, 0, this.pieces, 0, this.pieces.length);
	}


	/**
	 * Returns the given integer value as a byte.
	 * @param value the integer value
	 * @return the byte value
	 * @throws IllegalArgumentException if the given value is outside range {@code [0, 127]}
	 */
	static private byte toByte (final int value) {
		if (value < 0 | value > Byte.MAX_VALUE) throw new IllegalArgumentException();
		return (byte) value;
	}
}