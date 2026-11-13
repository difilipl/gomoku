package fr.esisar.in450.gomoku.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

import fr.esisar.in450.gomoku.gamecore.enums.CellColor;

/**
 * Panneau latéral : choix des joueurs, état de la partie, boutons et historique des coups.
 */
public class SidePanel extends JPanel
{
	/** Largeur du panneau. */
	private static final int WIDTH = 330;
	/** Marge intérieure du panneau. */
	private static final int PADDING = 22;
	/** Hauteur d'une carte joueur. */
	private static final int CARD_HEIGHT = 78;
	/** Rayon des coins des cartes. */
	private static final int CARD_CORNER = 16;
	/** Hauteur d'une ligne de l'historique. */
	private static final int HISTORY_ROW_HEIGHT = 26;
	/** Intervalle de rafraîchissement du chronomètre de l'IA. */
	private static final int SPINNER_FRAME_MS = 40;
	/** Durée d'un tour complet de l'indicateur de réflexion. */
	private static final int SPINNER_TURN_MS = 900;
	/** Texte du bouton quand les numéros sont cachés. */
	private static final String NUMBERS_OFF = "Afficher n°";
	/** Texte du bouton quand les numéros sont affichés. */
	private static final String NUMBERS_ON = "Cacher n°";
	/** Nombre de millisecondes dans une seconde. */
	private static final double MS_PER_SECOND = 1000.0;

	private final Map<CellColor, PlayerType> selection = new EnumMap<>(CellColor.class);
	private final Map<CellColor, PlayerCard> cards = new EnumMap<>(CellColor.class);
	private final BiConsumer<CellColor, PlayerType> onPlayerChanged;
	private final JLabel statusLabel = new JLabel(" ");
	private final JLabel pendingLabel = new JLabel(" ");
	private final FlatButton undoButton;
	private final FlatButton numbersButton;
	private final DefaultListModel<GameSnapshot.Move> historyModel = new DefaultListModel<>();
	private final JList<GameSnapshot.Move> historyList = new JList<>(historyModel);
	private final Timer spinnerTimer;

	private GameSnapshot snapshot;
	private boolean numbersShown = false;

	/**
	 * @param initialWhite    type de départ du joueur blanc
	 * @param initialBlack    type de départ du joueur noir
	 * @param onNewGame       appelé par le bouton « Nouvelle partie »
	 * @param onUndo          appelé par le bouton « Annuler »
	 * @param onSwap          appelé par le bouton qui échange les couleurs
	 * @param onPlayerChanged appelé quand l'utilisateur choisit un autre type de joueur
	 * @param onNumbersToggle appelé avec le nouvel état de l'affichage des numéros
	 */
	public SidePanel(PlayerType initialWhite, PlayerType initialBlack,
			Runnable onNewGame, Runnable onUndo, Runnable onSwap,
			BiConsumer<CellColor, PlayerType> onPlayerChanged,
			Consumer<Boolean> onNumbersToggle)
	{
		this.onPlayerChanged = onPlayerChanged;
		selection.put(CellColor.WHITE, initialWhite);
		selection.put(CellColor.BLACK, initialBlack);

		setBackground(Theme.PANEL);
		setPreferredSize(new Dimension(WIDTH, 0));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

		JLabel title = new JLabel("GOMOKU");
		title.setFont(Theme.TITLE);
		title.setForeground(Theme.TEXT);
		add(left(title));
		JLabel subtitle = new JLabel("Cinq pierres alignées pour gagner");
		subtitle.setFont(Theme.BODY);
		subtitle.setForeground(Theme.TEXT_MUTED);
		add(left(subtitle));
		add(Box.createVerticalStrut(22));

		add(left(section("JOUEURS")));
		add(Box.createVerticalStrut(8));
		for (CellColor color : new CellColor[] {CellColor.WHITE, CellColor.BLACK})
		{
			PlayerCard card = new PlayerCard(color);
			cards.put(color, card);
			add(left(card));
			add(Box.createVerticalStrut(8));
		}
		FlatButton swapButton = new FlatButton("Échanger les couleurs", Theme.CARD, Theme.TEXT_MUTED);
		swapButton.setFont(Theme.BODY);
		swapButton.addActionListener(e -> onSwap.run());
		add(left(swapButton));

		pendingLabel.setFont(Theme.SMALL);
		pendingLabel.setForeground(Theme.VICTORY);
		add(Box.createVerticalStrut(6));
		add(left(pendingLabel));
		add(Box.createVerticalStrut(12));

		add(left(section("PARTIE")));
		add(Box.createVerticalStrut(8));
		statusLabel.setFont(Theme.BODY_BOLD);
		statusLabel.setForeground(Theme.TEXT);
		add(left(statusLabel));
		add(Box.createVerticalStrut(12));

		FlatButton newGameButton = new FlatButton("Nouvelle partie", Theme.ACCENT, Color.WHITE);
		newGameButton.addActionListener(e -> onNewGame.run());
		add(left(newGameButton));
		add(Box.createVerticalStrut(8));

		JPanel row = new JPanel(new GridLayout(1, 2, 8, 0));
		row.setOpaque(false);
		undoButton = new FlatButton("Annuler le coup", Theme.CARD, Theme.TEXT);
		undoButton.addActionListener(e -> onUndo.run());
		numbersButton = new FlatButton(NUMBERS_OFF, Theme.CARD, Theme.TEXT);
		numbersButton.addActionListener(e ->
		{
			numbersShown = !numbersShown;
			numbersButton.setText(numbersShown ? NUMBERS_ON : NUMBERS_OFF);
			onNumbersToggle.accept(numbersShown);
		});
		row.add(undoButton);
		row.add(numbersButton);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		add(left(row));
		add(Box.createVerticalStrut(20));

		add(left(section("HISTORIQUE")));
		add(Box.createVerticalStrut(8));
		historyList.setCellRenderer(new MoveRenderer());
		historyList.setBackground(Theme.CARD);
		historyList.setFixedCellHeight(HISTORY_ROW_HEIGHT);
		historyList.setFocusable(false);
		historyList.setSelectionModel(new DefaultListSelectionModel()
		{
			@Override
			public void setSelectionInterval(int index0, int index1)
			{
			}
		});
		JScrollPane scroll = new JScrollPane(historyList);
		scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
		scroll.getViewport().setBackground(Theme.CARD);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(HISTORY_ROW_HEIGHT);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		scroll.getVerticalScrollBar().setBackground(Theme.CARD);
		add(left(scroll));

		spinnerTimer = new Timer(SPINNER_FRAME_MS, e -> cards.values().forEach(Component::repaint));
	}

	/** Affiche un nouvel état de la partie. */
	public void setSnapshot(GameSnapshot next)
	{
		boolean historyChanged = snapshot == null || snapshot.gameId() != next.gameId()
				|| snapshot.history().size() != next.history().size();
		snapshot = next;
		statusLabel.setText(statusText(next));
		statusLabel.setForeground(next.isOver() ? Theme.VICTORY : Theme.TEXT);
		undoButton.setEnabled(next.canUndo());
		updatePendingLabel();
		if (historyChanged)
		{
			historyModel.clear();
			next.history().forEach(historyModel::addElement);
			if (!historyModel.isEmpty())
			{
				historyList.ensureIndexIsVisible(historyModel.size() - 1);
			}
		}
		if (next.thinking() && !next.isOver())
		{
			spinnerTimer.start();
		}
		else
		{
			spinnerTimer.stop();
		}
		cards.values().forEach(Component::repaint);
	}

	/** Type choisi pour une couleur (appliqué à la prochaine partie). */
	public PlayerType getSelection(CellColor color)
	{
		return selection.get(color);
	}

	/** Change le type choisi pour une couleur. */
	public void setSelection(CellColor color, PlayerType type)
	{
		selection.put(color, type);
		updatePendingLabel();
		cards.get(color).repaint();
	}

	private void updatePendingLabel()
	{
		boolean pending = snapshot != null
				&& (snapshot.white() != selection.get(CellColor.WHITE) || snapshot.black() != selection.get(CellColor.BLACK));
		pendingLabel.setText(pending ? "Changement appliqué à la prochaine partie" : " ");
	}

	private static String statusText(GameSnapshot s)
	{
		if (s.error() != null)
		{
			return "Partie interrompue";
		}
		switch (s.winner())
		{
			case WHITE:
				return "Victoire des Blancs";
			case BLACK:
				return "Victoire des Noirs";
			case TIE:
				return "Égalité, plateau plein";
			default:
				break;
		}
		String side = s.toMove() == CellColor.WHITE ? "Blancs" : "Noirs";
		if (s.thinking())
		{
			return "L'IA réfléchit pour les " + side + "…";
		}
		if (s.isHumanTurn())
		{
			return "À toi de jouer (" + side + ")";
		}
		return "Tour des " + side;
	}

	private static String seconds(long ms)
	{
		return String.format(Locale.FRANCE, "%.1f s", ms / MS_PER_SECOND);
	}

	private static JLabel section(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(Theme.SECTION);
		label.setForeground(Theme.TEXT_MUTED);
		return label;
	}

	private static <T extends JComponent> T left(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		return component;
	}

	/**
	 * Carte d'un joueur : sa couleur, son type (cliquer pour le changer) et son temps de réflexion.
	 */
	private final class PlayerCard extends JComponent
	{
		private final CellColor color;
		private boolean hover = false;

		PlayerCard(CellColor color)
		{
			this.color = color;
			setPreferredSize(new Dimension(WIDTH - 2 * PADDING, CARD_HEIGHT));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, CARD_HEIGHT));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Cliquer pour choisir le joueur");
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					hover = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					hover = false;
					repaint();
				}

				@Override
				public void mousePressed(MouseEvent e)
				{
					showMenu();
				}
			});
		}

		private void showMenu()
		{
			JPopupMenu menu = new JPopupMenu();
			menu.setBackground(Theme.CARD);
			menu.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
			for (PlayerType type : PlayerType.values())
			{
				boolean selected = selection.get(color) == type;
				JMenuItem item = new JMenuItem((selected ? "•  " : "    ") + type.getLabel() + "  –  " + type.getDescription());
				item.setFont(selected ? Theme.BODY_BOLD : Theme.BODY);
				item.setBackground(Theme.CARD);
				item.setForeground(Theme.TEXT);
				item.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 12));
				item.addActionListener(e -> onPlayerChanged.accept(color, type));
				menu.add(item);
			}
			menu.show(this, 0, getHeight());
		}

		private boolean isActive()
		{
			return snapshot != null && !snapshot.isOver() && snapshot.toMove() == color;
		}

		private boolean isWinner()
		{
			return snapshot != null && snapshot.winner().name().equals(color.name());
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int w = getWidth();
			int h = getHeight();
			boolean active = isActive();

			g.setColor(active || hover ? Theme.CARD_ACTIVE : Theme.CARD);
			g.fill(new RoundRectangle2D.Double(0, 0, w, h, CARD_CORNER, CARD_CORNER));
			if (active || isWinner())
			{
				g.setColor(isWinner() ? Theme.VICTORY : Theme.ACCENT);
				g.setStroke(new BasicStroke(2f));
				g.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, CARD_CORNER, CARD_CORNER));
			}

			double stoneSize = 38;
			BoardPanel.paintIconStone(g, 16 + stoneSize / 2, h / 2.0, stoneSize, color);

			int textX = 16 + (int) stoneSize + 14;
			PlayerType shown = selection.get(color);
			g.setFont(Theme.SMALL);
			g.setColor(Theme.TEXT_MUTED);
			String side = color == CellColor.WHITE ? "BLANCS · commencent" : "NOIRS";
			g.drawString(side, textX, 24);
			g.setFont(Theme.BODY_BOLD);
			g.setColor(Theme.TEXT);
			g.drawString(shown.getLabel(), textX, 44);
			int chevronX = textX + g.getFontMetrics().stringWidth(shown.getLabel()) + 8;
			g.setColor(Theme.TEXT_MUTED);
			g.fillPolygon(new int[] {chevronX, chevronX + 8, chevronX + 4}, new int[] {36, 36, 41}, 3);
			g.setFont(Theme.SMALL);
			g.setColor(Theme.TEXT_MUTED);
			g.drawString(shown.getDescription(), textX, 62);

			paintTime(g, w, h);
			g.dispose();
		}

		private void paintTime(Graphics2D g, int w, int h)
		{
			if (snapshot == null)
			{
				return;
			}
			boolean thinking = snapshot.thinking() && !snapshot.isOver() && snapshot.toMove() == color;
			String text;
			if (thinking)
			{
				long elapsed = System.currentTimeMillis() - snapshot.thinkStartMs();
				text = seconds(elapsed);
				double spinner = 18;
				double sx = w - 16 - spinner;
				double sy = h / 2.0 - spinner - 2;
				double angle = -360.0 * (elapsed % SPINNER_TURN_MS) / SPINNER_TURN_MS;
				g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(Theme.BORDER);
				g.draw(new Ellipse2D.Double(sx, sy, spinner, spinner));
				g.setColor(Theme.ACCENT);
				g.draw(new Arc2D.Double(sx, sy, spinner, spinner, angle, 100, Arc2D.OPEN));
				g.setFont(Theme.SMALL);
				g.setColor(Theme.TEXT);
				FontMetrics metrics = g.getFontMetrics();
				g.drawString(text, w - 16 - metrics.stringWidth(text), h / 2 + 20);
				return;
			}
			text = seconds(snapshot.totalTime(color));
			g.setFont(Theme.SMALL);
			FontMetrics metrics = g.getFontMetrics();
			g.setColor(Theme.TEXT_MUTED);
			g.drawString("temps", w - 16 - metrics.stringWidth("temps"), h / 2 - 4);
			g.setColor(Theme.TEXT);
			g.drawString(text, w - 16 - metrics.stringWidth(text), h / 2 + 14);
		}
	}

	/** Affichage d'une ligne de l'historique : numéro, pierre, position et temps. */
	private static final class MoveRenderer extends JComponent implements ListCellRenderer<GameSnapshot.Move>
	{
		private GameSnapshot.Move move;
		private int index;
		private boolean last;

		@Override
		public Component getListCellRendererComponent(JList<? extends GameSnapshot.Move> list, GameSnapshot.Move value,
				int index, boolean isSelected, boolean cellHasFocus)
		{
			this.move = value;
			this.index = index;
			this.last = index == list.getModel().getSize() - 1;
			return this;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int h = getHeight();
			g.setColor(last ? Theme.CARD_ACTIVE : (index % 2 == 0 ? Theme.CARD : new Color(0x2F, 0x34, 0x3E)));
			g.fillRect(0, 0, getWidth(), h);
			if (last)
			{
				g.setColor(Theme.ACCENT);
				g.fillRect(0, 0, 3, h);
			}
			g.setFont(Theme.SMALL);
			FontMetrics metrics = g.getFontMetrics();
			int baseline = (h + metrics.getAscent() - metrics.getDescent()) / 2;
			g.setColor(Theme.TEXT_MUTED);
			String number = (index + 1) + ".";
			g.drawString(number, 34 - metrics.stringWidth(number), baseline);
			BoardPanel.paintIconStone(g, 50, h / 2.0, 13, move.color());
			g.setFont(Theme.BODY);
			g.setColor(Theme.TEXT);
			g.drawString("Ligne " + (move.row() + 1) + "  ·  Colonne " + (move.col() + 1), 66, baseline);
			g.setFont(Theme.SMALL);
			g.setColor(move.byAi() ? Theme.TEXT_MUTED : new Color(0x7F, 0xC8, 0x9A));
			String time = move.byAi() ? seconds(move.durationMs()) : "humain";
			g.drawString(time, getWidth() - 12 - g.getFontMetrics().stringWidth(time), baseline);
			g.dispose();
		}
	}

	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(WIDTH, super.getMinimumSize().height);
	}
}
