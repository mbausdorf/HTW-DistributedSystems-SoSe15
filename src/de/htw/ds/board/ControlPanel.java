package de.htw.ds.board;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import de.sb.java.TypeMetadata;


/**
 * Swing based control panel based on an abstract board.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class ControlPanel extends JPanel {
	static private final long serialVersionUID = 1L;
	static private final String MESSAGE_WHITE_WINS = "White wins the game!";
	static private final String MESSAGE_BLACK_WINS = "Black wins the game!";
	static private final String MESSAGE_DRAW = "Game is a draw!";

	private final Board<? extends PieceType> board;
	private final Deque<String> moveHistory;
	private final Deque<String> boardHistory;

	private final JProgressBar ratingBar;
	private final JTextArea displayArea;
	private final AbstractButton backButton;
	private final AbstractButton resetButton;


	/**
	 * Public constructor.
	 * @param board the board to be visualized
	 * @throws NullPointerException if the given board is {@code null}
	 */
	public ControlPanel (final Board<? extends PieceType> board) {
		if (board == null) throw new NullPointerException();

		this.board = board;
		this.moveHistory = new ArrayDeque<String>();
		this.boardHistory = new ArrayDeque<String>();
		this.boardHistory.addLast(board.getXfenState());

		this.ratingBar = new JProgressBar(JProgressBar.HORIZONTAL, -2000, 2000);
		this.displayArea = new JTextArea();

		{
			final Component versusLabel, ratingLabel, historyLabel, historyPane;
			final JTextField player1Field, player2Field;
			final SpringLayout layoutManager = new SpringLayout();
			this.setLayout(layoutManager);

			this.add(player1Field = new JTextField("You", 6));
			this.add(player2Field = new JTextField("Computer", 6));
			this.add(versusLabel = new JLabel("vs."));
			this.add(ratingLabel = new JLabel("Current rating"));
			this.add(this.ratingBar);
			this.add(historyLabel = new JLabel("Move History"));
			this.add(historyPane = new JScrollPane(this.displayArea));
			this.add(this.backButton = new JButton("Back"));
			this.add(this.resetButton = new JButton("Reset"));

			this.ratingBar.setForeground(Color.WHITE);
			this.ratingBar.setBackground(Color.BLACK);
			player2Field.setEditable(false);

			// same-faced alignment against parent
			for (final Component component : new Component[] { player1Field, player2Field }) {
				layoutManager.putConstraint(SpringLayout.NORTH, component, 0, SpringLayout.NORTH, this);
			}
			for (final Component component : new Component[] { this.backButton, this.resetButton }) {
				layoutManager.putConstraint(SpringLayout.SOUTH, component, 0, SpringLayout.SOUTH, this);
			}
			for (final Component component : new Component[] { player1Field, ratingLabel, historyLabel, historyPane, this.backButton }) {
				layoutManager.putConstraint(SpringLayout.WEST, component, 0, SpringLayout.WEST, this);
			}
			for (final Component component : new Component[] { this.ratingBar, historyPane, this.resetButton }) {
				layoutManager.putConstraint(SpringLayout.EAST, component, 0, SpringLayout.EAST, this);
			}
			layoutManager.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, player2Field);

			// same-faced alignment against sibling
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, versusLabel, 0, SpringLayout.VERTICAL_CENTER, player1Field);
			layoutManager.putConstraint(SpringLayout.VERTICAL_CENTER, ratingLabel, 0, SpringLayout.VERTICAL_CENTER, this.ratingBar);

			// opposite-faced alignment against sibling
			layoutManager.putConstraint(SpringLayout.NORTH, this.ratingBar, 5, SpringLayout.SOUTH, player1Field);
			layoutManager.putConstraint(SpringLayout.NORTH, historyLabel, 5, SpringLayout.SOUTH, this.ratingBar);
			layoutManager.putConstraint(SpringLayout.NORTH, historyPane, 5, SpringLayout.SOUTH, historyLabel);
			layoutManager.putConstraint(SpringLayout.SOUTH, historyPane, -5, SpringLayout.NORTH, this.backButton);
			layoutManager.putConstraint(SpringLayout.SOUTH, historyPane, -5, SpringLayout.NORTH, this.resetButton);
			layoutManager.putConstraint(SpringLayout.WEST, versusLabel, 15, SpringLayout.EAST, player1Field);
			layoutManager.putConstraint(SpringLayout.WEST, player2Field, 15, SpringLayout.EAST, versusLabel);
			layoutManager.putConstraint(SpringLayout.WEST, this.ratingBar, 5, SpringLayout.EAST, ratingLabel);
			layoutManager.putConstraint(SpringLayout.WEST, this.resetButton, 2, SpringLayout.HORIZONTAL_CENTER, this);
			layoutManager.putConstraint(SpringLayout.EAST, this.backButton, -3, SpringLayout.HORIZONTAL_CENTER, this);
		}

		this.backButton.addActionListener( (ActionEvent event) -> this.handleBackButtonPressed());
		this.resetButton.addActionListener( (ActionEvent event) -> this.handleResetButtonPressed());
		this.refresh();
	}


	/**
	 * {@inheritDoc}
	 */
	public void setControlsEnabled (final boolean enabled) {
		this.backButton.setEnabled(enabled & this.boardHistory.size() >= 2);
		this.resetButton.setEnabled(enabled);
	}


	/**
	 * Refreshes this panel.
	 */
	public void refresh () {
		final int rating = this.board.getRating();

		this.ratingBar.setValue(rating);
		this.ratingBar.setToolTipText(displayText(rating));
		this.displayArea.setText(displayText(this.moveHistory, this.board.getMoveClock() - this.moveHistory.size()));
		this.backButton.setEnabled(this.boardHistory.size() > 2);

		Logger.getGlobal().log(Level.INFO, "Board changed: \"{0}\"", this.board);
	}


	/**
	 * Logs the move history to this panel and the console.
	 * @param move the move performed
	 * @param capturing whether or not the move resulted in a capture
	 * @param activeType the type of the piece that moved
	 */
	public void handleMovePerformed (final AbsoluteMotion[] move, final boolean capturing, final PieceType activeType) {
		this.moveHistory.addLast(displayText(move, capturing, activeType, this.board.getFileCount()));
		this.boardHistory.addLast(this.board.getXfenState());
		this.refresh();
	}


	/**
	 * Logs the game result.
	 */
	public void handleGameOver (final int rating) {
		final String message = displayText(rating);
		this.displayArea.setText(this.displayArea.getText() + "\n\n" + message);
		this.ratingBar.setValue(rating);
		this.ratingBar.setToolTipText(message);
		Logger.getGlobal().log(Level.INFO, message);
	}


	/**
	 * Handles the pressing of the back button.
	 */
	private void handleBackButtonPressed () {
		if (this.board.isWhiteActive()) {
			this.moveHistory.removeLast();
			this.boardHistory.removeLast();
		}
		this.moveHistory.removeLast();
		this.boardHistory.removeLast();
		this.board.setXfenState(this.boardHistory.peekLast());
		this.refresh();

		final ChangeEvent changeEvent = new ChangeEvent(this);
		for (final ChangeListener listener : this.listenerList.getListeners(ChangeListener.class)) {
			listener.stateChanged(changeEvent);
		}
	}


	/**
	 * Handles the pressing of the reset button.
	 */
	private void handleResetButtonPressed () {
		this.moveHistory.clear();
		this.boardHistory.clear();
		this.board.setXfenState(null);
		this.boardHistory.addLast(this.board.getXfenState());
		this.refresh();

		final ChangeEvent changeEvent = new ChangeEvent(this);
		for (final ChangeListener listener : this.listenerList.getListeners(ChangeListener.class)) {
			listener.stateChanged(changeEvent);
		}
	}


	/**
	 * Adds the given change listener to this component.
	 * @param listener the change listener
	 */
	public void addChangeListener (final ChangeListener listener) {
		this.listenerList.add(ChangeListener.class, listener);
	}


	/**
	 * Removes the given change listener from this component.
	 * @param listener the change listener
	 */
	public void removeChangeListener (final ChangeListener listener) {
		this.listenerList.remove(ChangeListener.class, listener);
	}


	/**
	 * Returns a text representation for the given move history.
	 * @param moveHistory the move history
	 * @param moveClockOffset the move clock offset
	 */
	static private String displayText (final Deque<String> moveHistory, final int moveClockOffset) {
		final StringWriter writer = new StringWriter();
		int moveClock = moveClockOffset;

		if ((moveClock & 1) == 1) writer.write(String.format("%3d. ... ", (moveClock >> 1) + 1));

		for (final String move : moveHistory) {
			if ((moveClock & 1) == 0) writer.write(String.format("%3d. ", (moveClock >> 1) + 1));
			writer.write(move);
			writer.write((moveClock & 1) == 0 ? " " : "\n");

			moveClock += 1;
		}

		return writer.toString();
	}


	/**
	 * Returns a text representation for the given move. Note that the default piece type is
	 * intentionally unrepresented.
	 * @param move the move
	 * @param capturing whether or not the move captured any pieces
	 * @param pieceType the piece type that moved
	 * @param fileCount the number of files on a board
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given move consists of no partial moves
	 */
	static private String displayText (final AbsoluteMotion[] move, final boolean capturing, final PieceType pieceType, final byte fileCount) {
		if (move.length == 0) throw new IllegalArgumentException();

		final StringWriter writer = new StringWriter();
		if (pieceType.ordinal() > 0) writer.append(pieceType.getAlias());
		writer.append(Board.coordinatesToAlias(move[0].getSourceRank(), move[0].getSourceFile(), fileCount));
		for (AbsoluteMotion motion : move) {
			writer.append(capturing ? 'x' : '-');
			writer.append(Board.coordinatesToAlias(motion.getSinkRank(), motion.getSinkFile(), fileCount));
		}

		return writer.toString();
	}


	/**
	 * Returns a representation for the given rating. Usually that consists of the text equivalent
	 * of the given rating, but will deviate for the mix and max values to indicate the game has
	 * ended.
	 * @param rating the rating
	 * @return the text representation
	 */
	static private String displayText (final int rating) {
		final String text;
		switch (rating) {
			case +Integer.MAX_VALUE:
				text = MESSAGE_WHITE_WINS;
				break;
			case 0:
				text = MESSAGE_DRAW;
				break;
			case -Integer.MAX_VALUE:
				text = MESSAGE_BLACK_WINS;
				break;
			default:
				text = String.format("%.2f", 0.01 * rating);
				break;
		}
		return text;
	}
}