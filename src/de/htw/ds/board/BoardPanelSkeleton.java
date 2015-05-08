package de.htw.ds.board;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import de.sb.java.TypeMetadata;


/**
 * Swing based 2D board panel based on an abstract board.
 * @param <T> the type of the board's pieces
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class BoardPanelSkeleton<T extends PieceType> extends JPanel {
	static private final long serialVersionUID = 1L;

	private final Map<T,Image> whitePieceImages;
	private final Map<T,Image> blackPieceImages;
	private final Board<T> board;
	private final int searchDepth;
	private final List<byte[]> selectedPositions;


	/**
	 * Creates a new instance.
	 * @param board the board to be visualized
	 * @param searchDepth the search depth in half moves
	 * @param whitePieceImages the white piece images to be visualized
	 * @param blackPieceImages the black piece images to be visualized
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given search depth is negative
	 */
	public BoardPanelSkeleton (final Board<T> board, final int searchDepth, final Map<T,Image> whitePieceImages, final Map<T,Image> blackPieceImages) {
		if (board == null | whitePieceImages == null | blackPieceImages == null) throw new NullPointerException();
		if (searchDepth <= 0) throw new IllegalArgumentException();

		this.board = board;
		this.searchDepth = searchDepth;
		this.whitePieceImages = whitePieceImages;
		this.blackPieceImages = blackPieceImages;
		this.selectedPositions = new ArrayList<>();

		this.setLayout(new GridLayout(this.board.getRankCount(), this.board.getFileCount()));
		for (byte rank = (byte) (this.board.getRankCount() - 1); rank >= 0; --rank) {
			for (byte file = 0; file < this.board.getFileCount(); ++file) {
				final byte constRank = rank, constFile = file;

				final JButton button = new JButton();
				button.addActionListener((ActionEvent event) -> this.handleFieldSelection(constRank, constFile));
				this.add(button);
				this.refreshButton(rank, file, false);
			}
		}
	}


	/**
	 * Adds the given change event listener.
	 * @param listener the change event listener
	 */
	public void addChangeListener (final ChangeListener listener) {
		this.listenerList.add(ChangeListener.class, listener);
	}


	/**
	 * Removes the given change event listener.
	 * @param listener the change event listener
	 */
	public void removeChangeListener (final ChangeListener listener) {
		this.listenerList.remove(ChangeListener.class, listener);
	}


	/**
	 * {@inheritDoc}
	 */
	public void setControlsEnabled (final boolean enabled) {
		for (final Component component : BoardPanelSkeleton.this.getComponents()) {
			component.setEnabled(enabled);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void refresh () {
		this.selectedPositions.clear();
		for (byte rank = 0; rank < this.board.getRankCount(); ++rank) {
			for (byte file = 0; file < this.board.getFileCount(); ++file) {
				this.refreshButton(rank, file, false);
			}
		}
	}


	/**
	 * Sets whether or not the controls of this component, and any of it's listeners, shall be
	 * enabled.
	 * @param enabled whether or not the controls shall be enabled
	 */
	private void setGlobalControlsEnabled (final boolean enabled) {
		this.setControlsEnabled(enabled);

		final PropertyChangeEvent event = new PropertyChangeEvent(this, "controlsEnabled", !enabled, enabled);
		for (final PropertyChangeListener listener : this.getListeners(PropertyChangeListener.class)) {
			listener.propertyChange(event);
		}
	}


	/**
	 * Refreshes the given button depending on the checker-board layout, the given marking, and the
	 * piece image to be displayed.
	 * @param rank the board rank
	 * @param file the board file
	 * @param marked whether or not the position is marked
	 * @throws IndexOutOfBoundsException if the given position is out of bounds
	 */
	private void refreshButton (final byte rank, final byte file, final boolean marked) {
		final Color color = Board.whiteField(rank, file)
			? (marked ? Color.LIGHT_GRAY : Color.WHITE)
			: (marked ? Color.DARK_GRAY : Color.BLACK);
		final Piece<?> piece = this.board.getPiece(rank, file);
		final ImageIcon icon;
		if (piece == null) {
			icon = null;
		} else {
			final Map<T,Image> pieceImages = piece.isWhite() ? this.whitePieceImages : this.blackPieceImages;
			final Image pieceImage = pieceImages.get(piece.getType()).getScaledInstance(60, 60, Image.SCALE_SMOOTH);
			icon = new ImageIcon(pieceImage);
		}

		final int position = Board.coordinatesToPosition(this.board.getRankCount() - rank - 1, file, this.board.getFileCount());
		final JButton button = (JButton) this.getComponent(position);
		button.setBackground(color);
		button.setIcon(icon);
		button.setDisabledIcon(icon);
	}


	/**
	 * Interrupts an asynchronous operation in progress.
	 */
	public void interruptAsynchronousOperation () {
		// TODO: interrupt asynchronous operation 
	}

	/**
	 * Performs the given player move, and if successful also initiates the KIs counter move.
	 * @param rank the board rank
	 * @param file the board rank
	 */
	private void handleFieldSelection (final byte rank, final byte file) {
		final AbsoluteMotion[] move = this.selectMove(rank, file);
		if (move != null && !this.performMove(move)) {
			// TODO: perform this in a new thread, and store thread in field
			// "asynchronousOperation". Optionally reset this field to null
			// once the operation finishes.
			this.performComputerMove();
		}
	}


	/**
	 * Selection the board field at the given rank and file, and returns the selected move if the
	 * cumulative field selections identify a valid candidate.
	 * @param rank the board rank
	 * @param file the board file
	 * @return the move, or {@code null} for none yet
	 * @throws IllegalArgumentException if the given rank or file is out of bounds
	 */
	private AbsoluteMotion[] selectMove (final byte rank, final byte file) {
		if (rank < 0 | rank > this.board.getRankCount() | file < 0 | file > this.board.getFileCount()) throw new IllegalArgumentException();
		final byte[] selectedPosition = new byte[] { rank, file };

		if (this.selectedPositions.isEmpty()) {
			final Piece<?> piece = this.board.getPiece(rank, file);
			if (piece != null && piece.isWhite()) {
				this.selectedPositions.add(selectedPosition);
				this.refreshButton(rank, file, true);
			}
			return null;
		}

		if (Arrays.equals(selectedPosition, this.selectedPositions.get(this.selectedPositions.size() - 1))) {
			this.refresh();
			return null;
		}

		this.selectedPositions.add(selectedPosition);
		this.refreshButton(rank, file, true);

		final AbsoluteMotion[] moveTemplate = new AbsoluteMotion[this.selectedPositions.size() - 1];
		for (int index = 1; index < this.selectedPositions.size(); ++index) {
			final byte[] leftPosition = this.selectedPositions.get(index - 1);
			final byte[] rightPosition = this.selectedPositions.get(index);
			moveTemplate[index - 1] = new AbsoluteMotion(leftPosition[0], leftPosition[1], rightPosition[0], rightPosition[1], (byte) 0);
		}

		for (final AbsoluteMotion[] move : this.board.getCandidateMoves()) {
			if (Arrays.equals(moveTemplate, move)) {
				this.refresh();
				return move;
			}
		}
		return null;
	}


	/**
	 * Performs the given move. Returns {@code true} if the game is over, {@code false} otherwise.
	 * @param move the move
	 * @return whether or not the game is over
	 * @throws NullPointerException if the given move is {@code null}
	 * @throws IllegalArgumentException if the given move is empty, does not move a piece, or moves
	 *         a piece of the wrong color
	 */
	private boolean performMove (final AbsoluteMotion[] move) {
		if (move.length == 0) throw new IllegalArgumentException();

		final Piece<?> piece = this.board.getPiece(move[0].getSourceRank(), move[0].getSourceFile());
		if (piece == null || piece.isWhite() != this.board.isWhiteActive()) throw new IllegalArgumentException();

		final long pieceCountBefore = this.board.pieceStream().count();
		this.board.move(move);
		final long pieceCountAfter = this.board.pieceStream().count();
		final Prediction prediction = this.board.analyzeUninterruptibly(1);

		this.refresh();
		Logger.getGlobal().log(Level.INFO, "Player moves {0}", Arrays.toString(move));

		final MoveEvent event = new MoveEvent(this, piece.getType(), move, pieceCountAfter < pieceCountBefore, prediction.getRating(), prediction.getMoveSequence().isEmpty());
		for (final ChangeListener listener : this.getListeners(ChangeListener.class)) {
			listener.stateChanged(event);
		}
		return event.isGameOver();
	}


	/**
	 * Calculates the next computer move and performs it.
	 */
	private void performComputerMove () {
		this.setGlobalControlsEnabled(false);
		try {
			final Prediction prediction;
			try {
				final long before = System.currentTimeMillis();
				prediction = this.board.analyze(this.searchDepth);
				final long after = System.currentTimeMillis();
				Logger.getGlobal().log(Level.INFO, "Prediction: {0} after {1} seconds.", new Object[] { prediction, new Double(0.001 * (after - before)) });
			} catch (final InterruptedException exception) {
				Logger.getGlobal().log(Level.INFO, "Board analysis interrupted!");
				return;
			}

			final AbsoluteMotion[] computerMove = prediction.getMoveSequence().peekFirst();
			if (computerMove != null) this.performMove(computerMove);
		} finally {
			this.setGlobalControlsEnabled(true);
		}
	}
}