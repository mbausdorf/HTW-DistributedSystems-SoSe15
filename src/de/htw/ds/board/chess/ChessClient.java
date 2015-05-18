package de.htw.ds.board.chess;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import de.htw.ds.board.Board;
import de.htw.ds.board.GamePanel;
import de.htw.ds.board.Prediction;
import de.sb.java.TypeMetadata;
import de.sb.java.io.Streams;


/**
 * Chess client class using on a Swing based BoardPanel. Note that this class is declared final
 * because it provides an application entry point, and is therefore not supposed to be extended by
 * subclassing.
 * @see <a href="http://www.chessville.com/downloads/misc_downloads.htm#ChessIcons4">The original
 *      source of the chess piece icons</a>
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class ChessClient {
	static private enum Mode { USER_INTERFACE, ANALYZE }

	private final JComponent contentPane;
	private final Map<ChessPieceType,Image> whitePieceImages;
	private final Map<ChessPieceType,Image> blackPieceImages;


	/**
	 * Creates a new instance.
	 * @param board the board
	 * @param searchDepth the search depth in half moves
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given search depth is negative or odd
	 */
	public ChessClient (final Board<ChessPieceType> board, final int searchDepth) {
		this.whitePieceImages = defaultPieceImages(true);
		this.blackPieceImages = defaultPieceImages(false);
		this.contentPane = new GamePanel<ChessPieceType>(board, searchDepth, this.whitePieceImages, this.blackPieceImages);
	}


	/**
	 * Returns the content pane.
	 * @return the content pane
	 */
	public JComponent getContentPane () {
		return this.contentPane;
	}


	/**
	 * Returns the frame image.
	 * @return the frame image
	 */
	public Image getFrameImage () {
		return this.whitePieceImages.get(ChessPieceType.KING);
	}


	/**
	 * Returns the default mappings of chess piece types to their images.
	 * @param white whether or not the pieces are white
	 * @return a piece type to image map.
	 */
	static private Map<ChessPieceType,Image> defaultPieceImages (final boolean white) {
		final Map<String,byte[]> fileContents;
		try (InputStream fileSource = ChessClient.class.getResourceAsStream("chess-images.zip")) {
			fileContents = Streams.readAllAsZipEntries(fileSource);
		} catch (final IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}

		final Map<ChessPieceType,Image> pieceImages = new HashMap<>();
		for (final ChessPieceType type : ChessPieceType.values()) {
			final String imageName = (white ? "white" : "black") + "-" + type.name().toLowerCase() + ".png";
			final Image image = Toolkit.getDefaultToolkit().createImage(fileContents.get(imageName));
			pieceImages.put(type, image);
		}

		return pieceImages;
	}


	/**
	 * Returns a chess board reflectively created using the given class name and arguments.
	 * @param boardClassName the chess board class name
	 * @return the chess board created
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given class name is illegal, or the class does not
	 *         feature a suitable constructor
	 */
	@SuppressWarnings("unchecked")
	private static ChessBoard newBoard (final String boardClassName, final String... args) {
		final Class<ChessBoard> boardClass;
		try {
			boardClass = (Class<ChessBoard>) Class.forName(boardClassName, true, Thread.currentThread().getContextClassLoader());
		} catch (final ReflectiveOperationException exception) {
			throw new IllegalArgumentException(exception);
		}
		if (!ChessBoard.class.isAssignableFrom(boardClass)) throw new IllegalArgumentException();

		switch (args.length) {
			case 0: {
				final ChessBoard board = ChessBoard.newInstance(boardClass, (byte) 8, (byte) 8, (short) 0, (short) 0);
				board.setXfenState(null);
				return board;
			}
			case 1: {
				return ChessXfenCodec.singleton().decode(boardClass, args[0]);
			}
			default: {
				final byte rankCount = Byte.parseByte(args[0]);
				final byte fileCount = Byte.parseByte(args[1]);

				final ChessBoard board = ChessBoard.newInstance(boardClass, rankCount, fileCount, (short) 0, (short) 0);
				board.setXfenState(null);
				return board;
			}
		}
	}


	/**
	 * Client for playing chess as the white player on a given chess board, featuring plugable chess
	 * board implementations.
	 * @param args the mode (USER_INTERFACE or ANALYZE), the chess board class name, the analyzer
	 *        search depth (5 is a good value to start with), and then either an X-FEN like board
	 *        representation, or a rank count followed by a file count; all arguments are optional
	 * @throws IllegalArgumentException if any of the given class names is illegal, if the given
	 *         search depth is negative, if the given rank or file count is negative, or if the
	 *         given X-FEN board representation is invalid
	 * @throws IllegalStateException if there is no default layout for the given board dimensions
	 * @throws NumberFormatException if the given searchDepth, rank or file count is not a number
	 * @throws UnsupportedLookAndFeelException if the VM does not support Nimbus look-and-feel
	 * @throws InterruptedException if board analysis is interrupted by another thread
	 */
	static public void main (final String[] args) throws InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, InterruptedException {
		LogManager.getLogManager();

		final Mode mode = args.length == 0 ? Mode.USER_INTERFACE : Mode.valueOf(args[0].toUpperCase());
		final String boardClassName = args.length <= 1 ? ChessTableBoard.class.getName() : args[1];
		final int searchDepth = args.length <= 2 ? 5 : Integer.parseInt(args[2]);
		final String[] boardArguments = args.length <= 3 ? new String[0] : Arrays.copyOfRange(args, 3, args.length);
		final Board<ChessPieceType> board = newBoard(boardClassName, boardArguments);

		switch (mode) {
			case USER_INTERFACE: {
				UIManager.setLookAndFeel(new NimbusLookAndFeel());
				final ChessClient client = new ChessClient(board, searchDepth);

				final JFrame frame = new JFrame("Distributed Chess");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setContentPane(client.getContentPane());
				frame.setIconImage(client.getFrameImage());
				frame.pack();
				frame.setVisible(true);
				break;
			}
			case ANALYZE: {
				System.out.println(board.toString());

				final long before = System.currentTimeMillis();
				final Prediction prediction = board.analyze(searchDepth);
				final long after = System.currentTimeMillis();

				System.out.format("%s moves: %s\n", board.isWhiteActive() ? "White" : "Black", Arrays.toString(prediction.getMoveSequence().peekFirst()));
				System.out.format("Predicted minimax move sequence: %s\n", Arrays.deepToString(prediction.getMoveSequence().toArray()));
				System.out.format("Predicted board rating is %s.\n", prediction.getRating());
				System.out.format("Analysis time was %sms.\n", after - before);
				break;
			}
		}
	}
}