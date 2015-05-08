package de.htw.ds.board;

import java.awt.BorderLayout;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import de.sb.java.TypeMetadata;


/**
 * Swing based 2D game panel based on a control panel and a board panel.
 * @param <T> the piece type
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public class GamePanel<T extends PieceType> extends JPanel {
	static private final long serialVersionUID = 1L;

	private final ControlPanel controlPanel;
	private final BoardPanel<T> boardPanel;


	/**
	 * Creates a new instance.
	 * @param board the board to be visualized
	 * @param searchDepth the search depth in half moves
	 * @param whitePieceImages the white piece images to be visualized
	 * @param blackPieceImages the black piece images to be visualized
	 * @param <T> the piece type
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given search depth is negative
	 */
	public GamePanel (final Board<T> board, final int searchDepth, final Map<T,Image> whitePieceImages, final Map<T,Image> blackPieceImages) {
		this.controlPanel = new ControlPanel(board);
		this.boardPanel = new BoardPanel<>(board, searchDepth, whitePieceImages, blackPieceImages);
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.controlPanel, this.boardPanel);

		this.setLayout(new BorderLayout());
		this.add(splitPane);

		splitPane.setDividerLocation(-1);
		splitPane.setBorder(new EmptyBorder(2, 2, 2, 2));
		((JComponent) splitPane.getTopComponent()).setBorder(new EmptyBorder(0, 0, 0, 2));
		((JComponent) splitPane.getBottomComponent()).setBorder(new EmptyBorder(0, 2, 0, 0));

		this.boardPanel.addChangeListener((ChangeEvent event) -> this.handleChangeEvent(event));
		this.controlPanel.addChangeListener((ChangeEvent event) -> this.handleChangeEvent(event));
		this.controlPanel.addPropertyChangeListener((PropertyChangeEvent event) -> this.handlePropertyChangeEvent(event));
	}


	/**
	 * Handles change events.
	 * @param event the change event
	 */
	private void handleChangeEvent (final ChangeEvent event) {
		if (event.getSource() == this.controlPanel) {
			this.boardPanel.interruptAsynchronousOperation();
			this.boardPanel.refresh();
		} else if (event.getSource() == this.boardPanel && (event instanceof MoveEvent)) {
			final MoveEvent moveEvent = (MoveEvent) event;
			this.controlPanel.handleMovePerformed(moveEvent.getMove(), moveEvent.getCapture(), moveEvent.getType());
			if (moveEvent.isGameOver()) this.controlPanel.handleGameOver(moveEvent.getRating());
		}
	}


	/**
	 * Handles property change events.
	 * @param event the property change event
	 */
	private void handlePropertyChangeEvent (final PropertyChangeEvent event) {
		final Object value = event.getNewValue();
		if (event.getSource() == this.controlPanel) {
			if ("controlsEnabled".equals(event.getPropertyName())) {
				this.boardPanel.setControlsEnabled((Boolean) value);
			}
		}
	}
}