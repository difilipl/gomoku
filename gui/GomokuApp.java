package fr.esisar.in450.gomoku.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fr.esisar.in450.gomoku.gamecore.enums.CellColor;

/**
 * Interface graphique pour jouer au Gomoku contre les IA du projet.
 * Hors notation : elle ne modifie ni la base de code fournie ni les joueurs.
 * Lancer la méthode {@link #main(String[])}.
 */
public class GomokuApp
{
	/** Joueur blanc proposé au lancement (les Blancs commencent). */
	private static final PlayerType DEFAULT_WHITE = PlayerType.HUMAN;
	/** Joueur noir proposé au lancement. */
	private static final PlayerType DEFAULT_BLACK = PlayerType.MINMAX;
	/** Délai avant l'apparition des infobulles. */
	private static final int TOOLTIP_DELAY_MS = 400;

	private final JFrame frame = new JFrame("Gomoku");
	private final BoardPanel boardPanel;
	private final SidePanel sidePanel;
	private final GameController controller;
	private GameSnapshot snapshot;

	/** Construit la fenêtre et lance une première partie. */
	public GomokuApp()
	{
		controller = new GameController(this::onSnapshot);
		boardPanel = new BoardPanel(controller::humanMove);
		sidePanel = new SidePanel(DEFAULT_WHITE, DEFAULT_BLACK,
				this::newGame, controller::undo, this::swapColors,
				this::onPlayerChanged, boardPanel::setShowMoveNumbers, this::onModeChosen);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(boardPanel, BorderLayout.CENTER);
		frame.add(sidePanel, BorderLayout.EAST);
		frame.getContentPane().setBackground(Theme.BACKGROUND_EDGE);
		frame.setMinimumSize(new Dimension(820, 560));
		frame.pack();
		frame.setLocationRelativeTo(null);
		newGame();
	}

	/** Affiche la fenêtre. */
	public void show()
	{
		frame.setVisible(true);
	}

	/** Fenêtre principale, utile pour des captures d'écran. */
	public JFrame getFrame()
	{
		return frame;
	}

	private void onSnapshot(GameSnapshot next)
	{
		snapshot = next;
		boardPanel.setSnapshot(next);
		sidePanel.setSnapshot(next);
	}

	private void newGame()
	{
		controller.startNewGame(sidePanel.getSelection(CellColor.WHITE), sidePanel.getSelection(CellColor.BLACK));
	}

	private void swapColors()
	{
		PlayerType white = sidePanel.getSelection(CellColor.WHITE);
		sidePanel.setSelection(CellColor.WHITE, sidePanel.getSelection(CellColor.BLACK));
		sidePanel.setSelection(CellColor.BLACK, white);
		newGame();
	}

	/**
	 * Applique un mode de jeu et lance la partie tout de suite.
	 * L'humain garde sa couleur actuelle (les Blancs s'il n'y avait pas d'humain).
	 */
	private void onModeChosen(GameMode mode)
	{
		boolean humanIsBlack = sidePanel.getSelection(CellColor.BLACK).isHuman()
				&& !sidePanel.getSelection(CellColor.WHITE).isHuman();
		CellColor humanColor = humanIsBlack ? CellColor.BLACK : CellColor.WHITE;
		CellColor opponentColor = humanIsBlack ? CellColor.WHITE : CellColor.BLACK;
		sidePanel.setSelection(humanColor, PlayerType.HUMAN);
		sidePanel.setSelection(opponentColor, mode.getOpponent());
		newGame();
	}

	/** Un changement de joueur relance tout de suite une partie pas encore commencée. */
	private void onPlayerChanged(CellColor color, PlayerType type)
	{
		sidePanel.setSelection(color, type);
		if (snapshot == null || snapshot.history().isEmpty() || snapshot.isOver())
		{
			newGame();
		}
	}

	/** Point d'entrée de l'interface graphique. */
	public static void main(String[] args)
	{
		System.setProperty("awt.useSystemAAFontSettings", "on");
		SwingUtilities.invokeLater(() ->
		{
			try
			{
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			}
			catch (ReflectiveOperationException | UnsupportedLookAndFeelException e)
			{
				System.err.println("Thème Swing par défaut : " + e.getMessage());
			}
			ToolTipManager.sharedInstance().setInitialDelay(TOOLTIP_DELAY_MS);
			new GomokuApp().show();
		});
	}
}
