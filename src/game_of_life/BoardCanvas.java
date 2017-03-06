package game_of_life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

public class BoardCanvas extends JComponent implements MouseListener,
		MouseMotionListener, ComponentListener {

	private Simulation simulation;

	private boolean drawGrid;
	
	private int cellSize;
	
	private static final Color[] palette = { Color.BLACK, Color.GREEN,
			new Color(0, 128, 0), Color.RED, Color.ORANGE, Color.CYAN, Color.YELLOW,
			Color.BLUE, };

	private static final Color gridColor = Color.GRAY;

	public BoardCanvas(Simulation simul, boolean grid, int size) {
		simulation = simul;
		drawGrid = grid;
		cellSize = size;

		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
		
		updateBoardSize();
	}
	
	public void setCellSize(int size) {
		cellSize = size;
		updateBoardSize();
	}
	
	public void setSimulation(Simulation simul) {
		simulation = simul;
		updateBoardSize();
	}

	public void setGrid(boolean flag) {
		drawGrid = flag;
		repaint();
	}
	
	protected void paintComponent(Graphics g) {
		int width = getWidth();
		int height = getHeight();
		int boardWidth = simulation.getBoardWidth();
		int boardHeight = simulation.getBoardHeight();
		byte[][] board = simulation.getBoard();
		
		((Graphics2D) g).setBackground(Color.WHITE);
		g.clearRect(0, 0, width, height);

		for (int y = 0; y < boardHeight; ++y) {
			for (int x = 0; x < boardWidth; ++x) {
				if (board[y][x] == 0)
					continue;

				g.setColor(palette[board[y][x] - 1]);
				g.fillRect(x * cellSize, y * cellSize,
						cellSize, cellSize);
			}
		}

		if (drawGrid) {
			g.setColor(gridColor);

			for (int y = 1; y < boardHeight; ++y)
				g.drawLine(0, y * cellSize, width, y * cellSize);

			for (int x = 1; x < boardWidth; ++x)
				g.drawLine(x * cellSize, 0, x * cellSize,
						height);
		}
	}

	public void mouseDragged(MouseEvent e) {
		int x = e.getX() / cellSize;
		int y = e.getY() / cellSize;
		if (x < 0 || y < 0 || x >= simulation.getBoardWidth()
				|| y >= simulation.getBoardHeight())
			return;
		
		if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)
			simulation.setBoardCell(x, y, 1);
		else
			simulation.setBoardCell(x, y, 0);
		repaint();
	}

	public void mouseClicked(MouseEvent e) {
		mouseDragged(e);
	}
	
	public void componentResized(ComponentEvent e) {
		updateBoardSize();
	}
	
	private void updateBoardSize() {
		simulation.setBoardSize(getWidth() / cellSize, getHeight() / cellSize);
		repaint();
	}

	/*
	 * Unused listener methods
	 */
	
	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}
	
	public void componentHidden(ComponentEvent e) {
	}
	
	public void componentMoved(ComponentEvent e) {
	}
	
	public void componentShown(ComponentEvent e) {	
	}
}
// vim:et:sw=4:ts=4:fileencoding=utf-8
