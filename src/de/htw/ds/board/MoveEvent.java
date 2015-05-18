package de.htw.ds.board;

import javax.swing.event.ChangeEvent;
import de.sb.java.TypeMetadata;


/**
 * These change events are thrown by board panels once a move has been performed.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class MoveEvent extends ChangeEvent {
	private static final long serialVersionUID = 1L;

	private final PieceType pieceType;
	private final AbsoluteMotion[] move;
	private final boolean capture;
	private final int rating;
	private final boolean gameOver;


	/**
	 * Creates a new instance
	 * @param source the source
	 * @param PieceType the piece type than moved
	 * @param move the move as a sequence of partial moves
	 * @param capture whether or not a piece was captured
	 * @param rating the projected rating
	 * @param gameOver whether or not the game is over
	 */
	public MoveEvent (final Object source, final PieceType PieceType, final AbsoluteMotion[] move, final boolean capture, final int rating, final boolean gameOver) {
		super(source);

		this.pieceType = PieceType;
		this.move = move;
		this.capture = capture;
		this.rating = rating;
		this.gameOver = gameOver;
	}


	/**
	 * Returns the piece type that moved.
	 * @return the piece type
	 */
	public PieceType getType () {
		return this.pieceType;
	}


	/**
	 * Returns the move.
	 * @return the move as a sequence of partial moves
	 */
	public AbsoluteMotion[] getMove () {
		return this.move;
	}


	/**
	 * Returns whether or not a piece was captured.
	 * @return {@code true} if a piece was captured, {@code false} otherwise
	 */
	public boolean getCapture () {
		return this.capture;
	}


	/**
	 * Returns the projected rating.
	 * @return the projected rating
	 */
	public int getRating () {
		return this.rating;
	}


	/**
	 * Returns {@code true} if the game is over, {@code false} if it is in progress.
	 * @return whether or not the game is over
	 */
	public boolean isGameOver () {
		return this.gameOver;
	}
}