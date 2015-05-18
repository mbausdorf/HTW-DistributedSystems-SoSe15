package de.htw.ds.board.chess;

import de.htw.ds.board.AbsoluteMotion;
import de.htw.ds.board.Piece;
import de.htw.ds.board.Prediction;
import de.sb.java.Threads;
import de.sb.java.TypeMetadata;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static de.htw.ds.board.chess.ChessPieceType.KING;
import static de.htw.ds.board.chess.ChessPieceType.PAWN;


/**
 * This is a template for ChessTableBoard subclasses.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class ChessTableBoard2 extends ChessTableBoard {
	static private final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
	static private ExecutorService executor = Executors.newFixedThreadPool(PROCESSOR_COUNT, Threads.newDaemonThreadFactory());

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
	public ChessTableBoard2(final byte rankCount, final byte fileCount, final short moveClock, final short reversibleMoveClock) {
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
	public ChessTableBoard2(final char[][] pieceMatrix, final short moveClock, final short reversibleMoveClock, final boolean[] castlingAbilities, byte[] passingPawnCoordinates) {
		super(pieceMatrix, moveClock, reversibleMoveClock, castlingAbilities, passingPawnCoordinates);
	}

	@Override
	public List<AbsoluteMotion[]> getCandidateMoves() {
		final boolean whiteActive = this.isWhiteActive();
		final List<AbsoluteMotion[]> candidatesMoves = Collections.synchronizedList(new ArrayList<>(256));
		if (this.getKing(whiteActive) == null) return candidatesMoves;

		boolean captureKingMode = false;
		final Stream<Piece<ChessPieceType>> stream = this.pieceStream().filter((Piece<ChessPieceType> piece) -> piece.isWhite() == whiteActive).parallel();
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
	 * {@inheritDoc} Note that this implementation is single-threaded.
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws InterruptedException {@inheritDoc}
	 */
	protected Prediction analyzeRecursivelyMT (final int depth) throws InterruptedException {
		if (depth <= 0) throw new IllegalArgumentException();

		final boolean whitePerspective = this.isWhiteActive();
		final List<Prediction> alternatives = new ArrayList<>();

		final Collection<AbsoluteMotion[]> moves = this.getCandidateMoves();
		List<Future<Prediction>> futures = Collections.synchronizedList(new ArrayList<>(moves.size()));
		for (final AbsoluteMotion[] move : moves) {
			futures.add(executor.submit(() -> this.analyzeRecursively(move, depth)));
		}

		for (final Future<Prediction> predictionFuture : futures) {
			try {
				final Prediction prediction = predictionFuture.get();
				if (alternatives.isEmpty()) {
					alternatives.add(prediction);
				} else {
					final Comparator<Prediction> comparator = whitePerspective ? WHITE_PREDICTION_COMPARATOR : BLACK_PREDICTION_COMPARATOR;
					final int compare = comparator.compare(prediction, alternatives.get(0));
					if (compare > 0) alternatives.clear();
					if (compare >= 0) alternatives.add(prediction);
				}
			}
			catch (ExecutionException executionEx) {
				try{
					throw executionEx.getCause(); //is a Throwable!
				} catch(NullPointerException e){
					Logger.getGlobal().log(Level.WARNING, e.getMessage());
				} catch(IllegalArgumentException e){
					Logger.getGlobal().log(Level.WARNING, e.getMessage());
				} catch (InterruptedException e) {
				} catch (Throwable e) {
					throw new AssertionError();
				}
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

	@Override
	public Prediction analyze(int depth) throws InterruptedException {
		if (depth > 4)
			return analyzeRecursivelyMT(depth);
		else
			return analyzeRecursively(depth);
	}
}