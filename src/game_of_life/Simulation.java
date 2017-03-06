package game_of_life;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

public class Simulation {

	private byte[][] board;

	private byte[][] tempBoard;

	private int boardWidth;

	private int boardHeight;

	private int numGenerations;

	private int numLiving;

	private boolean[] ruleLiving;

	private boolean[] ruleDead;

	private static final String CONWAY_RULE = "23/3";
	
	private static final Random rand = new Random();

	public Simulation() {
		setRule(CONWAY_RULE);
	}
	
	public void setBoardSize(int width, int height) {
		if (board == null || height > board.length || width > board[0].length) {
			System.out.println("Creating board: width=" + width + ", height="
					+ height);

			byte[][] newBoard = new byte[height][width];
			if (board != null) {

				int w = Math.min(width, boardWidth);
				int h = Math.min(height, boardHeight);
				for (int y = 0; y < h; ++y)
					for (int x = 0; x < w; ++x)
						newBoard[y][x] = board[y][x];
			}

			board = newBoard;
			tempBoard = new byte[height][width];
		}

		boardWidth = width;
		boardHeight = height;
	}

	public void setRule(String rule) {
		ruleLiving = new boolean[9];
		ruleDead = new boolean[9];

		char[] ch = rule.toCharArray();

		int n = 0;
		for (; n < ch.length; ++n) {
			if (ch[n] == '/')
				break;
			ruleLiving[Character.digit(ch[n], 10)] = true;
		}

		for (++n; n < ch.length; ++n)
			ruleDead[Character.digit(ch[n], 10)] = true;

		System.out.println("Rule loaded: " + getRule());
	}

	public String getRule() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < 9; ++i)
			if (ruleLiving[i])
				buffer.append(i);
		buffer.append('/');
		for (int i = 0; i < 9; ++i)
			if (ruleDead[i])
				buffer.append(i);
		return buffer.toString();
	}

	public void randomizeBoard() {
		numGenerations = 0;
		numLiving = 0;
		for (int y = 0; y < boardHeight; ++y) {
			for (int x = 0; x < boardWidth; ++x) {
				board[y][x] = (byte)rand.nextInt(2);
				
				if (board[y][x] > 0)
					++numLiving;
			}
		}
	}

	public void clearBoard() {
		for (int y = 0; y < boardHeight; ++y)
			for (int x = 0; x < boardWidth; ++x)
				board[y][x] = 0;
		numGenerations = 0;
		numLiving = 0;
	}

	/**
	 * Format example: <code>
	 *  #C Comment
	 *  #R Rule
	 *  x = [width], y = [height]
	 *  bobobobobobobo$bobobobobobobobobob!
	 * </code>
	 * <ul>
	 * <li># are optional</li>
	 * <li>Run length encoding can be used. Example: 32b, 10o</li>
	 * <li>$ marks a line break. RLE is also possible. Example: 77$</li>
	 * <li>! marks the end of file</li>
	 * </ul>
	 */
	public void readFile(File file) throws IOException {
		System.out.println("Reading file " + file);

		clearBoard();
		setRule(CONWAY_RULE); // Default file rule

		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			String line = null;
			while (reader.ready()) {
				line = reader.readLine().trim();
				if (line.charAt(0) != '#')
					break;

				if (Character.toUpperCase(line.charAt(1)) == 'R')
					setRule(line.substring(2).trim());
			}

			if (line == null)
				throw new IOException("No width and height defined");

			String[] vars = line.split(",");
			int width = Integer.parseInt(vars[0].split("=")[1].trim());
			int height = Integer.parseInt(vars[1].split("=")[1].trim());

			System.out.println("Size " + width + "/" + height);
			
			if (boardWidth <= width || boardHeight <= height) {
				setBoardSize(width + 10, height + 10);
				clearBoard();
			}

			int xStart = (boardWidth - width) / 2;
			int yStart = (boardHeight - height) / 2;

			// Read run length encoded board
			int x = xStart, y = yStart, runlen = 0;
			while (reader.ready()) {
				char ch = (char)reader.read();

				switch (ch) {
				// Dead cell
				case 'b':
					if (runlen > 0) {
						for (int i = 0; i < runlen; ++i)
							board[y][x++] = 0;
					} else
						board[y][x++] = 0;
					runlen = 0;
					break;

				// Living cell
				case 'o':
					if (runlen > 0) {
						for (int i = 0; i < runlen; ++i)
							board[y][x++] = 1;
						numLiving += runlen;
					} else {
						board[y][x++] = 1;
						++numLiving;
					}
					runlen = 0;
					break;

				// Line break
				case '$':
					x = xStart;
					if (runlen > 0) {
						y += runlen;
					} else
						++y;
					runlen = 0;
					break;

				// End
				case '!':
					return;

				default:
					// RLE
					if (Character.isDigit(ch)) {
						runlen *= 10;
						runlen += Character.digit(ch, 10);
					}
					break;
				}
			}
		} finally {
			reader.close();
		}
	}

	public void saveFile(File file) throws IOException {
		int xMin = Integer.MAX_VALUE, xMax = Integer.MIN_VALUE;
		int yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
		for (int y = 0; y < boardHeight; ++y) {
			for (int x = 0; x < boardWidth; ++x) {
				if (board[y][x] != 0) {
					if (x < xMin)
						xMin = x;
					if (x > xMax)
						xMax = x;

					if (y < yMin)
						yMin = y;
					if (y > yMax)
						yMax = y;
				}
			}
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		try {
			writer.write("#C Saved at " + new Date() + "\n#R "
					+ getRule() + "\n");

			writer.write("#R " + getRule() + "\n");
			
			int numBreaks = 0;
			for (int y = yMin; y <= yMax; ++y) {

				int runlen = 0;
				char lastCh = 'b';
				for (int x = xMin; x <= xMax; ++x) {
					char ch = board[y][x] != 0 ? 'o' : 'b';
					if (lastCh != ch) {
						if (numBreaks > 0) {
							if (numBreaks > 1)
								writer.write(Integer.toString(numBreaks));
							writer.write("$\n");
							numBreaks = 0;
						}

						if (runlen > 0) {
							if (runlen > 1)
								writer.write(Integer.toString(runlen));
							writer.write(lastCh);
						}

						runlen = 1;
						lastCh = ch;
					} else
						++runlen;
				}

				if (lastCh == 'o') {
					if (runlen > 1)
						writer.write(Integer.toString(runlen));
					writer.write('o');
				}

				++numBreaks;
			}

			writer.write("!\n");
		} finally {
			writer.close();
		}
	}

	public void generate() {
		numLiving = 0;
		for (int y = 0; y < boardHeight; ++y) {
			for (int x = 0; x < boardWidth; ++x) {
				byte neighbors = 0;

				int bottom = (y + 1) % boardHeight;
				int top = (y + boardHeight - 1) % boardHeight;
				int right = (x + 1) % boardWidth;
				int left = (x + boardWidth - 1) % boardWidth;

				// Neighbors on the right
				if (board[top][right] != 0)
					++neighbors;
				if (board[y][right] != 0)
					++neighbors;
				if (board[bottom][right] != 0)
					++neighbors;

				// Neighbors over and under the cell
				if (board[top][x] != 0)
					++neighbors;
				if (board[bottom][x] != 0)
					++neighbors;

				// Neighbors on the left
				if (board[top][left] != 0)
					++neighbors;
				if (board[y][left] != 0)
					++neighbors;
				if (board[bottom][left] != 0)
					++neighbors;

				if (board[y][x] != 0)
					tempBoard[y][x] = ruleLiving[neighbors] ? neighbors : 0;
				else
					tempBoard[y][x] = ruleDead[neighbors] ? neighbors : 0;

				if (tempBoard[y][x] != 0)
					++numLiving;
			}
		}

		byte[][] temp = board;
		board = tempBoard;
		tempBoard = temp;

		++numGenerations;
	}

	public int getNumGenerations() {
		return numGenerations;
	}

	public int getNumLiving() {
		return numLiving;
	}

	public int getBoardWidth() {
		return boardWidth;
	}

	public int getBoardHeight() {
		return boardHeight;
	}

	public void setBoardCell(int x, int y, int val) {
		board[y][x] = (byte)val;
	}

	public byte[][] getBoard() {
		return board;
	}
}
// vim:et:sw=4:ts=4:fileencoding=utf-8
