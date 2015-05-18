package de.htw.ds.board;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import de.sb.java.TypeMetadata;


/**
 * Instances of this class represent the results of minimax board analysis. They contain the
 * predicted move sequence considering best play by both sides, and the board rating after
 * performing said move sequence.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class Prediction {

	private final int rating;
	private final Deque<AbsoluteMotion[]> moveSequence;


	/**
	 * Creates a new instance with the given projected rating.
	 * @param rating the predicted board rating in cents
	 */
	public Prediction (final int rating) {
		this.rating = rating;
		this.moveSequence = new LinkedList<>();
	}


	/**
	 * Returns the predicted board rating.
	 * @return the predicted board rating, in cents
	 */
	public int getRating () {
		return this.rating;
	}


	/**
	 * Returns the predicted move sequence.
	 * @return the predicted move sequence, with each move represented by possibly multiple partial
	 *         motions
	 */
	public Deque<AbsoluteMotion[]> getMoveSequence () {
		return this.moveSequence;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return String.format("%s rates %+.2f", Arrays.deepToString(this.moveSequence.toArray()), 0.01 * this.rating);
	}
}