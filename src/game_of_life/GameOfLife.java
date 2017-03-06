package game_of_life;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;

public class GameOfLife extends JFrame implements ActionListener, Runnable {

	private static final Dimension CANVAS_MINSIZE = new Dimension(150, 150);

	private static final Dimension CANVAS_SIZE = new Dimension(500, 500);

	private static final int DEFAULT_CELLSIZE = 1;

	/*
	 * Simulation
	 */

	private Simulation simulation;

	/*
	 * Canvas
	 */

	private BoardCanvas canvas;

	/*
	 * Group Simulation
	 */

	private JTextField ruleTextField;

	private JButton ruleButton;

	private JSpinner stepsSpinner;

	private JButton generateButton;

	/*
	 * Group Board
	 */
	
	private JButton randomizeButton;

	private JButton clearButton;

	/*
	 * Group Draw
	 */

	private JCheckBox gridCheckBox;

	private JSpinner cellSizeSpinner;

	private JButton cellSizeButton;

	/*
	 * Background thread
	 */

	private Thread generateThread;

	private boolean generateRunning;

	/*
	 * Menu
	 */
	private JMenuBar menuBar;

	private JMenu menuFile;

	private JMenuItem openMenuItem;

	private JMenuItem saveMenuItem;

	private JMenuItem exitMenuItem;

	/*
	 * Save/Open Dialog
	 */

	private JFileChooser fileChooser;

	public GameOfLife(String[] argv) throws IOException {
		// Exit on close
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		simulation = new Simulation();
		
		fileChooser = new JFileChooser(new File(".").getAbsolutePath());
		fileChooser.setFileFilter(new FileFilter() {
			public boolean accept(File file) {
				if (file.isDirectory())
					return true;
				
				String name = file.getName();
				int n = name.lastIndexOf('.');
				if (n < 0 || n == name.length() - 1)
					return false;
				return name.substring(n + 1).equalsIgnoreCase("rle");
			}
			
			public String getDescription() {
				return "RLE files";
			}
		});

		// Create GUI
		createMenu();
		getContentPane().add(createContentPane());
		pack();

		setTitle("Conway's Game of Life");
		setVisible(true);
	}

	private void createMenu() {
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		// Create File menu
		menuFile = new JMenu("Datei");
		menuBar.add(menuFile);

		openMenuItem = new JMenuItem("Öffnen");
		openMenuItem.addActionListener(this);
		menuFile.add(openMenuItem);

		saveMenuItem = new JMenuItem("Speichern");
		saveMenuItem.addActionListener(this);
		menuFile.add(saveMenuItem);

		exitMenuItem = new JMenuItem("Beenden");
		exitMenuItem.addActionListener(this);
		menuFile.add(exitMenuItem);
	}

	private JPanel createContentPane() {
		JPanel panel = new JPanel(new GridBagLayout());

		/*
		 * Canvas
		 */

		canvas = new BoardCanvas(simulation, false, DEFAULT_CELLSIZE);
		canvas.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		canvas.setMinimumSize(CANVAS_SIZE);
		canvas.setPreferredSize(CANVAS_MINSIZE);

		panel.add(canvas, new GridBagConstraints(0, 0, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.BOTH, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		/*
		 * Tool Panel
		 */

		panel.add(createToolPanel(), new GridBagConstraints(1, 0, // x,y
				1, 1, // w,h
				0, 0, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.NONE, // fill
				new Insets(0, 0, 0, 0), // external padding
				0, 0)); // inner padding

		return panel;
	}

	private JPanel createToolPanel() {
		JPanel panel = new JPanel(new GridBagLayout());

		panel.add(createSimulationGroup(), new GridBagConstraints(0, 0, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.BOTH, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		panel.add(createBoardGroup(), new GridBagConstraints(0, 1, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.BOTH, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding
		
		panel.add(createDrawGroup(), new GridBagConstraints(0, 2, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.BOTH, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		return panel;
	}

	private JPanel createDrawGroup() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Zeichnen"));

		/*
		 * Grid check box
		 */

		gridCheckBox = new JCheckBox("Zeichne Raster", false);
		gridCheckBox.addActionListener(this);
		panel.add(gridCheckBox, new GridBagConstraints(0, 0, // x,y
				2, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.HORIZONTAL, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		/*
		 * Cell size
		 */

		cellSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
		panel.add(cellSizeSpinner, new GridBagConstraints(0, 1, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.HORIZONTAL, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		cellSizeButton = new JButton("Zellengröße");
		cellSizeButton.addActionListener(this);
		panel.add(cellSizeButton, new GridBagConstraints(1, 1, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.HORIZONTAL, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		return panel;
	}

	private JPanel createSimulationGroup() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Simulation"));

		/*
		 * Rule
		 */

		ruleTextField = new JTextField(simulation.getRule());
		panel.add(ruleTextField, new GridBagConstraints(0, 0, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.HORIZONTAL, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		ruleButton = new JButton("Regel setzen");
		ruleButton.addActionListener(this);
		panel.add(ruleButton, new GridBagConstraints(1, 0, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.HORIZONTAL, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		/*
		 * Spinner
		 */

		panel.add(new JLabel("Schritte:"), new GridBagConstraints(0, 1, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.NONE, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		stepsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		panel.add(stepsSpinner, new GridBagConstraints(1, 1, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.HORIZONTAL, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		/*
		 * Generate button
		 */

		generateButton = new JButton("Simulieren");
		generateButton.addActionListener(this);
		panel.add(generateButton, new GridBagConstraints(0, 2, // x,y
				2, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.HORIZONTAL, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		return panel;
	}


	private JPanel createBoardGroup() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Feld"));

		/*
		 * Randomize button
		 */

		randomizeButton = new JButton("Zufällig füllen");
		randomizeButton.addActionListener(this);
		panel.add(randomizeButton, new GridBagConstraints(0, 0, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.HORIZONTAL, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		/*
		 * Clear button
		 */

		clearButton = new JButton("Löschen");
		clearButton.addActionListener(this);
		panel.add(clearButton, new GridBagConstraints(0, 1, // x,y
				1, 1, // w,h
				1, 1, // weightx, weighty
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.HORIZONTAL, // fill
				new Insets(3, 3, 3, 3), // external padding
				0, 0)); // inner padding

		return panel;
	}
	
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		
		// Randomize
		if (source == randomizeButton) {
			simulation.randomizeBoard();
			canvas.repaint();
		}
		// Generate
		else if (source == generateButton) {
			generateRunning = !generateRunning;
			
			if (generateRunning) {
				generateButton.setText("Anhalten");
				enableElements();

				generateThread = new Thread(this);
				generateThread.start();
			}
		}
		// Clear board
		else if (source == clearButton) {
			simulation.clearBoard();
			canvas.repaint();
		}
		// Save board
		else if (source == saveMenuItem) {
			if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				try {
					simulation.saveFile(fileChooser.getSelectedFile());
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		// Open board
		else if (source == openMenuItem) {
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				try {
					simulation.readFile(fileChooser.getSelectedFile());
					ruleTextField.setText(simulation.getRule());
					canvas.repaint();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		// Exit program
		else if (source == exitMenuItem) {
			System.exit(0);
		}
		// Grid check box
		else if (source == gridCheckBox) {
			updateGrid();
		}
		// Cell size
		else if (source == cellSizeButton) {
			updateGrid();
			canvas.setCellSize(((Integer) cellSizeSpinner.getValue()).intValue());
		}
		// Generation rule
		else if (source == ruleButton) {
			try {
			    simulation.setRule(ruleTextField.getText());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private void updateGrid() {
		int size = ((Integer) cellSizeSpinner.getValue()).intValue();
		if (size <= 3)
			canvas.setGrid(false);
		else
		    canvas.setGrid(gridCheckBox.isSelected());
	}

	private void enableElements() {
		boolean e = !generateRunning;
		stepsSpinner.setEnabled(e);
		cellSizeSpinner.setEnabled(e);
		cellSizeButton.setEnabled(e);
		clearButton.setEnabled(e);
		randomizeButton.setEnabled(e);
		openMenuItem.setEnabled(e);
		saveMenuItem.setEnabled(e);
		ruleTextField.setEnabled(e);
		ruleButton.setEnabled(e);
	}

	public void run() {
		try {
			int maxSteps = ((Integer) stepsSpinner.getValue()).intValue();
			while (--maxSteps >= 0 && generateRunning) {
				simulation.generate();
				
				final int steps = maxSteps; 
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						generateButton.setText("Anhalten " + steps);
						canvas.repaint();
					}
				});
			}

			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					generateRunning = false;
					generateButton.setText("Simulieren");
					enableElements();
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] argv) throws IOException {
		new GameOfLife(argv);
	}
}

// vim:et:sw=4:ts=4:fileencoding=utf-8
