package de.htw.ds.board.chess;

import de.htw.ds.board.Motion;
import de.htw.ds.board.RelativeMotion;
import de.htw.ds.board.PieceType;
import de.sb.java.Reflection;
import de.sb.java.TypeMetadata;


/**
 * Defines the piece types allowed for a chess game. The static valuation of pieces is based loosely
 * on {@code Hans Berliner, "The System: A World Champion's Approach to Chess", 1999}.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public enum ChessPieceType implements PieceType {
	PAWN ('P', 100, new RelativeMotion[] {
		new RelativeMotion((byte) +1, (byte) -1, Motion.CAPTURE_REQUIRED),
		new RelativeMotion((byte) +1, (byte) +1, Motion.CAPTURE_REQUIRED),
		new RelativeMotion((byte) +1, (byte) 0, Motion.CAPTURE_FORBIDDEN),
		new RelativeMotion((byte) +2, (byte) 0, (byte) (Motion.CAPTURE_FORBIDDEN | Motion.TOUCH_FORBIDDEN))
	}),
	KING ('K', 100000, new RelativeMotion[] {
		new RelativeMotion((byte) -1, (byte) -1, (byte) 0),
		new RelativeMotion((byte) -1, (byte) 0, (byte) 0),
		new RelativeMotion((byte) -1, (byte) +1, (byte) 0),
		new RelativeMotion((byte) +1, (byte) -1, (byte) 0),
		new RelativeMotion((byte) +1, (byte) 0, (byte) 0),
		new RelativeMotion((byte) +1, (byte) +1, (byte) 0),
		new RelativeMotion((byte) 0, (byte) -1, (byte) 0),
		new RelativeMotion((byte) 0, (byte) +1, (byte) 0),
		new RelativeMotion((byte) 0, (byte) -2, (byte) (Motion.CAPTURE_FORBIDDEN | Motion.TOUCH_FORBIDDEN)),
		new RelativeMotion((byte) 0, (byte) +2, (byte) (Motion.CAPTURE_FORBIDDEN | Motion.TOUCH_FORBIDDEN))
	}),
	KNIGHT ('N', 320, new RelativeMotion[] {
		new RelativeMotion((byte) +1, (byte) -2, (byte) 0),
		new RelativeMotion((byte) +2, (byte) -1, (byte) 0),
		new RelativeMotion((byte) +2, (byte) +1, (byte) 0),
		new RelativeMotion((byte) +1, (byte) +2, (byte) 0),
		new RelativeMotion((byte) -1, (byte) +2, (byte) 0),
		new RelativeMotion((byte) -2, (byte) +1, (byte) 0),
		new RelativeMotion((byte) -2, (byte) -1, (byte) 0),
		new RelativeMotion((byte) -1, (byte) -2, (byte) 0)
	}),
	BISHOP ('B', 330, new RelativeMotion[] {
		new RelativeMotion((byte) +1, (byte) -1, Motion.CONTINUOUS),
		new RelativeMotion((byte) +1, (byte) +1, Motion.CONTINUOUS),
		new RelativeMotion((byte) -1, (byte) +1, Motion.CONTINUOUS),
		new RelativeMotion((byte) -1, (byte) -1, Motion.CONTINUOUS)
	}),
	ROOK ('R', 510, new RelativeMotion[] {
		new RelativeMotion((byte) 0, (byte) -1, Motion.CONTINUOUS),
		new RelativeMotion((byte) 0, (byte) +1, Motion.CONTINUOUS),
		new RelativeMotion((byte) -1, (byte) 0, Motion.CONTINUOUS),
		new RelativeMotion((byte) +1, (byte) 0, Motion.CONTINUOUS),
	}),
	QUEEN ('Q', 880, Reflection.union(ROOK.motions, BISHOP.motions)),
	ARCHBISHOP ('A', 750, Reflection.union(BISHOP.motions, KNIGHT.motions)),
	CHANCELLOR ('C', 800, Reflection.union(ROOK.motions, KNIGHT.motions)),
	EMPRESS ('E', 999, Reflection.union(QUEEN.motions, KNIGHT.motions));

	private final char alias;
	private final int rating;
	private final RelativeMotion[] motions;


	/**
	 * Creates a new instance.
	 * @param alias the character alias
	 * @param rating the base rating in cents
	 * @param motions the motions
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given rating is negative
	 */
	private ChessPieceType (final char alias, final int rating, final RelativeMotion[] motions) {
		if (motions == null) throw new NullPointerException();
		if (rating <= 0) throw new IllegalArgumentException();

		this.alias = alias;
		this.rating = rating;
		this.motions = motions;
	}


	/**
	 * {@inheritDoc}
	 */
	public char getAlias () {
		return this.alias;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getRating () {
		return this.rating;
	}


	/**
	 * {@inheritDoc}
	 */
	public RelativeMotion[] getMotions () {
		return this.motions;
	}


	/**
	 * Returns a chess type for the given character alias.
	 * @param alias the character alias
	 * @return the associated type
	 * @throws IllegalArgumentException if the given alias is illegal
	 */
	static public ChessPieceType valueOf (final char alias) {
		switch (alias) {
			case 'P':
				return ChessPieceType.PAWN;
			case 'N':
				return ChessPieceType.KNIGHT;
			case 'B':
				return ChessPieceType.BISHOP;
			case 'R':
				return ChessPieceType.ROOK;
			case 'A':
				return ChessPieceType.ARCHBISHOP;
			case 'C':
				return ChessPieceType.CHANCELLOR;
			case 'Q':
				return ChessPieceType.QUEEN;
			case 'E':
				return ChessPieceType.EMPRESS;
			case 'K':
				return ChessPieceType.KING;
			default:
				throw new IllegalArgumentException();
		}
	}
}