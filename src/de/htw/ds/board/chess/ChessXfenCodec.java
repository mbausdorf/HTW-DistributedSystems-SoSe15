package de.htw.ds.board.chess;

import java.io.StringWriter;
import de.htw.ds.board.Board;
import de.htw.ds.board.Piece;
import de.htw.ds.board.XfenCodec;
import de.sb.java.TypeMetadata;


/**
 * This singleton class provides encoding/decoding capabilities for XFEN chess board representations.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class ChessXfenCodec extends XfenCodec {
	static private final ChessXfenCodec SINGLETON = new ChessXfenCodec();
	static private final byte XFEN_SECTION_COUNT = 6;


	/**
	 * Returns the singleton instance of this class.
	 * @return the process singleton
	 */
	static public ChessXfenCodec singleton () {
		return SINGLETON;
	}


	/**
	 * Prevents instantiation.
	 */
	protected ChessXfenCodec () {}


	/**
	 * Returns a chess board for the given X-FEN state.
	 * @param boardClass the board class
	 * @param xfenState the X-FEN state
	 * @return the chess board
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given X-FEN state is syntactically malformed, or
	 *         would result in an illegal board state
	 */
	public <T extends ChessBoard> T decode (final Class<T> boardClass, final String xfenState) {
		final String[] xfenSections = xfenState.split("\\s+");
		if (xfenSections.length != XFEN_SECTION_COUNT) throw new IllegalArgumentException();

		final char[][] pieceMatrix = this.decodePieceMatrix(xfenSections[0]);
		final byte rankCount = (byte) pieceMatrix.length;
		final byte fileCount = (byte) pieceMatrix[0].length;
		if (rankCount < ChessBoard.MIN_RANK_COUNT | rankCount > Byte.MAX_VALUE | fileCount < ChessBoard.MIN_FILE_COUNT | fileCount > Byte.MAX_VALUE) throw new IllegalArgumentException();

		final boolean[] castlingAbilities = new boolean[4];
		if (!xfenSections[2].equals("-")) {
			for (final char alias : xfenSections[2].toCharArray()) {
				switch (alias) {
					case 'Q':
						castlingAbilities[0] = true;
						break;
					case 'K':
						castlingAbilities[1] = true;
						break;
					case 'q':
						castlingAbilities[2] = true;
						break;
					case 'k':
						castlingAbilities[3] = true;
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
		}

		final byte[] passingPawnCoordinates = xfenSections[3].equals("-")
			? null
			: Board.aliasToCoordinates(xfenSections[3], fileCount);
		final boolean whiteActive = this.decodeColor(xfenSections[1]);
		final short reversibleMoveClock = this.decodeReversibleMoveClock(xfenSections[4]);
		final short moveClock = this.decodeMoveClock(xfenSections[5], whiteActive);
		return ChessBoard.newInstance(boardClass, pieceMatrix, moveClock, reversibleMoveClock, castlingAbilities, passingPawnCoordinates);
	}


	/**
	 * Returns an X-FEN state for the given chess board.
	 * @param board the chess board
	 * @return the X-FEN state
	 * @throws NullPointerException if the given chess board is {@code null}
	 */
	public String encode (final ChessBoard board) {
		final StringWriter writer = new StringWriter();

		final char[][] pieceMatrix = new char[board.getRankCount()][board.getFileCount()];
		board.pieceStream().forEach((Piece<ChessPieceType> piece) -> pieceMatrix[piece.getRank()][piece.getFile()] = piece.getAlias());
		writer.write(this.encodePieceMatrix(pieceMatrix));
		writer.write(this.encodeColor(board.isWhiteActive()));
		writer.write(' ');

		if ((board.getCastlingAbilities() & (ChessBoard.MASK_CASTLE_WHITE_LEFT | ChessBoard.MASK_CASTLE_WHITE_RIGHT | ChessBoard.MASK_CASTLE_BLACK_LEFT | ChessBoard.MASK_CASTLE_BLACK_RIGHT)) == 0) {
			writer.write('-');
		} else {
			if ((board.getCastlingAbilities() & ChessBoard.MASK_CASTLE_WHITE_RIGHT) != 0) writer.write('K');
			if ((board.getCastlingAbilities() & ChessBoard.MASK_CASTLE_WHITE_LEFT) != 0) writer.write('Q');
			if ((board.getCastlingAbilities() & ChessBoard.MASK_CASTLE_BLACK_RIGHT) != 0) writer.write('k');
			if ((board.getCastlingAbilities() & ChessBoard.MASK_CASTLE_BLACK_LEFT) != 0) writer.write('q');
		}
		writer.write(' ');

		writer.write(board.getPassingPawn() == null ? "-" : Board.coordinatesToAlias(board.getPassingPawn().getRank(), board.getPassingPawn().getFile(), board.getFileCount()));
		writer.write(' ');

		writer.write(this.encodeReversibleMoveClock(board.getReversibleMoveClock()));
		writer.write(' ');

		writer.write(this.encodeMoveClock(board.getMoveClock()));
		return writer.toString();
	}
}