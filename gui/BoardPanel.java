package fr.esisar.in450.gomoku.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

import javax.swing.JPanel;
import javax.swing.Timer;

import fr.esisar.in450.gomoku.gamecore.enums.CellColor;
import fr.esisar.in450.gomoku.gamecore.enums.WinnerState;
import fr.esisar.in450.gomoku.gamecore.model.Coords;

/**
 * Dessine le plateau en bois, les pierres en relief et les animations,
 * et transforme les clics en coordonnées de case.
 */
public class BoardPanel extends JPanel
{
	/** Marge entre le bord de la zone et le plateau, en pixels. */
	private static final int OUTER_PADDING = 28;
	/** Espace entre le bord du plateau et la première ligne, en nombre de cases. */
	private static final double BORDER_CELLS = 1.0;
	/** Rayon des coins du plateau, en fraction de case. */
	private static final double CORNER_RATIO = 0.35;
	/** Diamètre d'une pierre, en fraction de case. */
	private static final double STONE_RATIO = 0.92;
	/** Diamètre d'un point de repère (hoshi), en fraction de case. */
	private static final double STAR_RATIO = 0.16;
	/** Distance des points de repère au bord de la grille. */
	private static final int STAR_OFFSET = 3;
	/** Rayon de clic accepté autour d'une intersection, en fraction de case. */
	private static final double CLICK_TOLERANCE = 0.48;

	/** Durée de l'animation de pose d'une pierre. */
	private static final int PLACE_ANIMATION_MS = 260;
	/** Taille de départ d'une pierre qui tombe. */
	private static final double PLACE_START_SCALE = 1.35;
	/** Période du clignotement de l'alignement gagnant. */
	private static final int VICTORY_PULSE_MS = 1400;
	/** Durée du tracé progressif de l'alignement gagnant. */
	private static final int VICTORY_REVEAL_MS = 450;
	/** Délai avant l'apparition du bandeau de fin de partie. */
	private static final int BANNER_DELAY_MS = 700;
	/** Position verticale du bandeau, en fraction du plateau, du côté opposé à l'alignement gagnant. */
	private static final double BANNER_FAR_POSITION = 0.80;
	/** Durée d'apparition du bandeau de fin de partie. */
	private static final int BANNER_FADE_MS = 400;
	/** Intervalle entre deux images d'animation (environ 60 images par seconde). */
	private static final int FRAME_MS = 16;

	/** Graine fixe pour que le bois garde le même dessin d'une partie à l'autre. */
	private static final long WOOD_SEED = 20261113L;

	private final BiConsumer<Integer, Integer> onCellClicked;
	private final Timer animationTimer;

	private GameSnapshot snapshot;
	private int hoverRow = -1;
	private int hoverCol = -1;
	private boolean showMoveNumbers = false;
	private boolean bannerHidden = false;

	private long placeAnimationStart = 0;
	private int animatedHistorySize = 0;
	private long gameOverStart = 0;

	private BufferedImage woodCache;
	private int woodCacheSize = -1;

	private double cell;
	private double gridX;
	private double gridY;
	private double boardX;
	private double boardY;
	private double boardSide;

	/**
	 * @param onCellClicked appelé avec (ligne, colonne) quand l'utilisateur clique sur une intersection
	 */
	public BoardPanel(BiConsumer<Integer, Integer> onCellClicked)
	{
		this.onCellClicked = onCellClicked;
		setPreferredSize(new Dimension(760, 760));
		setMinimumSize(new Dimension(420, 420));
		setOpaque(true);

		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				updateHover(e.getX(), e.getY());
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				updateHover(Integer.MIN_VALUE, Integer.MIN_VALUE);
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				handleClick(e.getX(), e.getY());
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);

		animationTimer = new Timer(FRAME_MS, e -> onFrame());
	}

	/** Affiche un nouvel état de la partie. */
	public void setSnapshot(GameSnapshot next)
	{
		GameSnapshot previous = snapshot;
		boolean newGame = previous == null || previous.gameId() != next.gameId();
		int size = next.history().size();
		if (newGame || size != animatedHistorySize)
		{
			if (size > animatedHistorySize && !newGame)
			{
				placeAnimationStart = System.currentTimeMillis();
			}
			animatedHistorySize = size;
		}
		if (next.isOver() && (previous == null || !previous.isOver() || newGame))
		{
			gameOverStart = System.currentTimeMillis();
			bannerHidden = false;
		}
		snapshot = next;
		refreshCursor();
		startAnimationIfNeeded();
		repaint();
	}

	/** Affiche ou cache le numéro des coups sur les pierres. */
	public void setShowMoveNumbers(boolean show)
	{
		showMoveNumbers = show;
		repaint();
	}

	private void onFrame()
	{
		repaint();
		if (!needsAnimation())
		{
			animationTimer.stop();
		}
	}

	private boolean needsAnimation()
	{
		long now = System.currentTimeMillis();
		boolean placing = now - placeAnimationStart < PLACE_ANIMATION_MS;
		boolean victory = snapshot != null && !snapshot.winningLine().isEmpty();
		boolean banner = snapshot != null && snapshot.isOver() && now - gameOverStart < BANNER_DELAY_MS + BANNER_FADE_MS;
		return placing || victory || banner;
	}

	private void startAnimationIfNeeded()
	{
		if (needsAnimation() && !animationTimer.isRunning())
		{
			animationTimer.start();
		}
	}

	private void updateHover(int x, int y)
	{
		int[] cellAt = cellAt(x, y);
		int row = cellAt == null ? -1 : cellAt[0];
		int col = cellAt == null ? -1 : cellAt[1];
		if (row != hoverRow || col != hoverCol)
		{
			hoverRow = row;
			hoverCol = col;
			refreshCursor();
			repaint();
		}
	}

	private void handleClick(int x, int y)
	{
		if (snapshot != null && snapshot.isOver() && !bannerHidden && isBannerVisible())
		{
			bannerHidden = true;
			repaint();
			return;
		}
		int[] cellAt = cellAt(x, y);
		if (cellAt != null && canPlayAt(cellAt[0], cellAt[1]))
		{
			onCellClicked.accept(cellAt[0], cellAt[1]);
		}
	}

	private boolean canPlayAt(int row, int col)
	{
		return snapshot != null && snapshot.isHumanTurn() && snapshot.cells()[row][col] == null;
	}

	private void refreshCursor()
	{
		boolean playable = hoverRow >= 0 && canPlayAt(hoverRow, hoverCol);
		setCursor(Cursor.getPredefinedCursor(playable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
	}

	/** Intersection la plus proche d'un point, ou null si le point est trop loin de toute intersection. */
	private int[] cellAt(int x, int y)
	{
		if (snapshot == null || cell <= 0)
		{
			return null;
		}
		int size = boardSize();
		int col = (int) Math.round((x - gridX) / cell);
		int row = (int) Math.round((y - gridY) / cell);
		if (row < 0 || row >= size || col < 0 || col >= size)
		{
			return null;
		}
		double dx = x - (gridX + col * cell);
		double dy = y - (gridY + row * cell);
		if (Math.hypot(dx, dy) > CLICK_TOLERANCE * cell)
		{
			return null;
		}
		return new int[] {row, col};
	}

	private int boardSize()
	{
		return snapshot.cells().length;
	}

	private void computeGeometry()
	{
		int size = boardSize();
		double available = Math.min(getWidth(), getHeight()) - 2.0 * OUTER_PADDING;
		cell = available / (size - 1 + 2 * BORDER_CELLS);
		boardSide = cell * (size - 1 + 2 * BORDER_CELLS);
		boardX = (getWidth() - boardSide) / 2.0;
		boardY = (getHeight() - boardSide) / 2.0;
		gridX = boardX + BORDER_CELLS * cell;
		gridY = boardY + BORDER_CELLS * cell;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		paintBackground(g);
		if (snapshot != null)
		{
			computeGeometry();
			paintBoard(g);
			paintGrid(g);
			paintStones(g);
			paintVictoryLine(g);
			paintHover(g);
			paintBanner(g);
		}
		g.dispose();
	}

	private void paintBackground(Graphics2D g)
	{
		float radius = (float) Math.max(getWidth(), getHeight());
		g.setPaint(new RadialGradientPaint(
				new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0), radius,
				new float[] {0f, 1f},
				new Color[] {Theme.BACKGROUND_CENTER, Theme.BACKGROUND_EDGE}));
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	private void paintBoard(Graphics2D g)
	{
		double corner = cell * CORNER_RATIO;
		for (int layer = 6; layer >= 1; layer--)
		{
			double spread = layer * cell * 0.06;
			g.setColor(new Color(0, 0, 0, 22));
			g.fill(new RoundRectangle2D.Double(boardX - spread / 2, boardY + spread, boardSide + spread, boardSide + spread / 2,
					corner + spread, corner + spread));
		}
		Shape boardShape = new RoundRectangle2D.Double(boardX, boardY, boardSide, boardSide, corner, corner);
		Shape oldClip = g.getClip();
		g.clip(boardShape);
		g.drawImage(woodTexture((int) Math.ceil(boardSide)), (int) boardX, (int) boardY, null);
		g.setClip(oldClip);

		g.setStroke(new BasicStroke(1.2f));
		g.setColor(new Color(255, 240, 210, 70));
		g.draw(new RoundRectangle2D.Double(boardX + 1, boardY + 1, boardSide - 2, boardSide - 2, corner, corner));
		g.setColor(new Color(60, 30, 5, 120));
		g.draw(boardShape);
	}

	/** Texture de bois calculée une fois par taille de plateau. */
	private BufferedImage woodTexture(int side)
	{
		if (woodCache != null && woodCacheSize == side)
		{
			return woodCache;
		}
		BufferedImage image = new BufferedImage(Math.max(side, 1), Math.max(side, 1), BufferedImage.TYPE_INT_RGB);
		Random random = new Random(WOOD_SEED);
		double[] rowNoise = new double[side + 1];
		for (int i = 0; i < rowNoise.length; i++)
		{
			rowNoise[i] = random.nextDouble();
		}
		double scale = 760.0 / Math.max(side, 1);
		for (int y = 0; y < side; y++)
		{
			for (int x = 0; x < side; x++)
			{
				double sx = x * scale;
				double sy = y * scale;
				double wave = sy * 0.12 + 2.2 * Math.sin(sx * 0.0052 + sy * 0.0035) + 0.5 * Math.sin(sx * 0.031 + 1.3);
				double ring = (Math.sin(wave) + 1) / 2;
				double fine = (Math.sin(sy * 0.75 + 2.0 * Math.sin(sx * 0.012) + rowNoise[y] * 4.0) + 1) / 2;
				double shade = 0.20 + 0.22 * ring + 0.10 * fine + 0.30 * ((double) (x + y) / (2.0 * side));
				shade += (random.nextDouble() - 0.5) * 0.04;
				Color base = mix(Theme.WOOD_LIGHT, Theme.WOOD_DARK, clamp(shade));
				double grain = Math.pow(ring, 10) * 0.16;
				image.setRGB(x, y, mix(base, Theme.WOOD_GRAIN, grain).getRGB());
			}
		}
		woodCache = image;
		woodCacheSize = side;
		return image;
	}

	private void paintGrid(Graphics2D g)
	{
		int size = boardSize();
		double end = (size - 1) * cell;
		g.setColor(Theme.GRID);
		g.setStroke(new BasicStroke((float) Math.max(1.0, cell * 0.035)));
		for (int i = 0; i < size; i++)
		{
			g.draw(new Line2D.Double(gridX, gridY + i * cell, gridX + end, gridY + i * cell));
			g.draw(new Line2D.Double(gridX + i * cell, gridY, gridX + i * cell, gridY + end));
		}
		g.setStroke(new BasicStroke((float) Math.max(1.5, cell * 0.06)));
		g.draw(new RoundRectangle2D.Double(gridX, gridY, end, end, 0, 0));

		int center = (size - 1) / 2;
		int[] stars = {STAR_OFFSET, center, size - 1 - STAR_OFFSET};
		double starDiameter = cell * STAR_RATIO;
		for (int row : stars)
		{
			for (int col : stars)
			{
				boolean corner = row != center && col != center;
				if (corner || (row == center && col == center))
				{
					g.fill(new Ellipse2D.Double(gridX + col * cell - starDiameter / 2, gridY + row * cell - starDiameter / 2,
							starDiameter, starDiameter));
				}
			}
		}

		g.setColor(Theme.COORDINATES);
		g.setFont(Theme.SMALL.deriveFont(Font.BOLD, (float) Math.max(9, cell * 0.30)));
		FontMetrics metrics = g.getFontMetrics();
		double labelOffset = cell * 0.62;
		for (int i = 0; i < size; i++)
		{
			String label = String.valueOf(i + 1);
			int width = metrics.stringWidth(label);
			float x = (float) (gridX + i * cell - width / 2.0);
			g.drawString(label, x, (float) (gridY - labelOffset + metrics.getAscent() / 2.0));
			g.drawString(label, x, (float) (gridY + end + labelOffset + metrics.getAscent() / 2.0 - 2));
			float y = (float) (gridY + i * cell + metrics.getAscent() / 2.0 - 2);
			g.drawString(label, (float) (gridX - labelOffset - width / 2.0), y);
			g.drawString(label, (float) (gridX + end + labelOffset - width / 2.0), y);
		}
	}

	private void paintStones(Graphics2D g)
	{
		List<GameSnapshot.Move> history = snapshot.history();
		long elapsed = System.currentTimeMillis() - placeAnimationStart;
		double progress = Math.min(1.0, elapsed / (double) PLACE_ANIMATION_MS);
		double eased = 1 - Math.pow(1 - progress, 3);
		int lastIndex = history.size() - 1;
		CellColor[][] cells = snapshot.cells();

		for (int index = 0; index < history.size(); index++)
		{
			GameSnapshot.Move move = history.get(index);
			if (cells[move.row()][move.col()] != move.color())
			{
				continue;
			}
			boolean animated = index == lastIndex && progress < 1.0;
			double scale = animated ? PLACE_START_SCALE + (1 - PLACE_START_SCALE) * eased : 1.0;
			float alpha = animated ? (float) Math.min(1.0, 0.2 + eased) : 1f;
			paintStone(g, move.row(), move.col(), move.color(), scale, alpha, animated ? 1 + (1 - eased) * 2.5 : 1);
			if (showMoveNumbers)
			{
				paintMoveNumber(g, move, index + 1, alpha);
			}
		}

		GameSnapshot.Move last = snapshot.lastMove();
		if (last != null && !showMoveNumbers && progress >= 1.0)
		{
			double cx = gridX + last.col() * cell;
			double cy = gridY + last.row() * cell;
			double marker = cell * 0.22;
			g.setColor(Theme.ACCENT);
			g.fill(new Ellipse2D.Double(cx - marker / 2, cy - marker / 2, marker, marker));
			g.setColor(new Color(255, 255, 255, last.color() == CellColor.BLACK ? 60 : 0));
			g.setStroke(new BasicStroke((float) (cell * 0.03)));
			g.draw(new Ellipse2D.Double(cx - marker / 2, cy - marker / 2, marker, marker));
		}
	}

	/**
	 * Dessine une pierre avec son ombre, son dégradé et son reflet.
	 *
	 * @param shadowFactor éloignement de l'ombre (plus grand pendant la chute)
	 */
	private void paintStone(Graphics2D g, int row, int col, CellColor color, double scale, float alpha, double shadowFactor)
	{
		double diameter = cell * STONE_RATIO * scale;
		double cx = gridX + col * cell;
		double cy = gridY + row * cell;
		double x = cx - diameter / 2;
		double y = cy - diameter / 2;
		Composite oldComposite = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

		double shadowOffset = cell * 0.06 * shadowFactor;
		g.setColor(new Color(0, 0, 0, 45));
		g.fill(new Ellipse2D.Double(x + shadowOffset * 0.6, y + shadowOffset * 1.4, diameter * 1.04, diameter * 1.04));
		g.setColor(new Color(0, 0, 0, 70));
		g.fill(new Ellipse2D.Double(x + shadowOffset * 0.3, y + shadowOffset, diameter, diameter));

		boolean black = color == CellColor.BLACK;
		Color highlight = black ? Theme.BLACK_STONE_HIGHLIGHT : Theme.WHITE_STONE_HIGHLIGHT;
		Color body = black ? Theme.BLACK_STONE_BODY : Theme.WHITE_STONE_BODY;
		Color edge = black ? Theme.BLACK_STONE_EDGE : Theme.WHITE_STONE_EDGE;
		Point2D focus = new Point2D.Double(cx - diameter * 0.22, cy - diameter * 0.26);
		g.setPaint(new RadialGradientPaint(
				new Point2D.Double(cx - diameter * 0.12, cy - diameter * 0.14), (float) (diameter * 0.72), focus,
				new float[] {0f, 0.55f, 1f},
				new Color[] {highlight, body, edge},
				MultipleGradientPaint.CycleMethod.NO_CYCLE));
		g.fill(new Ellipse2D.Double(x, y, diameter, diameter));

		double glint = diameter * 0.26;
		g.setPaint(new RadialGradientPaint(
				new Point2D.Double(cx - diameter * 0.2, cy - diameter * 0.24), (float) glint,
				new float[] {0f, 1f},
				new Color[] {new Color(255, 255, 255, black ? 70 : 150), new Color(255, 255, 255, 0)}));
		g.fill(new Ellipse2D.Double(cx - diameter * 0.2 - glint, cy - diameter * 0.24 - glint, glint * 2, glint * 2));

		g.setComposite(oldComposite);
	}

	private void paintMoveNumber(Graphics2D g, GameSnapshot.Move move, int number, float alpha)
	{
		String text = String.valueOf(number);
		boolean last = number == snapshot.history().size();
		float fontSize = (float) (cell * (text.length() >= 3 ? 0.30 : 0.38));
		g.setFont(Theme.BODY_BOLD.deriveFont(fontSize));
		FontMetrics metrics = g.getFontMetrics();
		Color textColor = last ? Theme.ACCENT : (move.color() == CellColor.BLACK ? new Color(235, 235, 235) : new Color(30, 30, 30));
		g.setColor(new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), Math.round(alpha * 255)));
		double cx = gridX + move.col() * cell;
		double cy = gridY + move.row() * cell;
		g.drawString(text, (float) (cx - metrics.stringWidth(text) / 2.0),
				(float) (cy + (metrics.getAscent() - metrics.getDescent()) / 2.0));
	}

	private void paintVictoryLine(Graphics2D g)
	{
		List<Coords> line = snapshot.winningLine();
		if (line.isEmpty())
		{
			return;
		}
		long elapsed = System.currentTimeMillis() - gameOverStart;
		double pulse = (Math.sin(2 * Math.PI * elapsed / VICTORY_PULSE_MS) + 1) / 2;
		Coords first = line.get(0);
		Coords last = line.get(line.size() - 1);
		double x1 = gridX + first.col * cell;
		double y1 = gridY + first.row * cell;
		double x2 = gridX + last.col * cell;
		double y2 = gridY + last.row * cell;

		double reveal = Math.min(1.0, elapsed / (double) VICTORY_REVEAL_MS);
		double xe = x1 + (x2 - x1) * reveal;
		double ye = y1 + (y2 - y1) * reveal;

		for (int glow = 4; glow >= 1; glow--)
		{
			int alpha = (int) ((25 + 25 * pulse) / glow * 2);
			g.setColor(new Color(Theme.VICTORY.getRed(), Theme.VICTORY.getGreen(), Theme.VICTORY.getBlue(), Math.min(255, alpha)));
			g.setStroke(new BasicStroke((float) (cell * 0.12 * glow), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new Line2D.Double(x1, y1, xe, ye));
		}
		g.setColor(Theme.VICTORY);
		g.setStroke(new BasicStroke((float) (cell * 0.08), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(x1, y1, xe, ye));

		double ring = cell * STONE_RATIO * (1.04 + 0.06 * pulse);
		g.setStroke(new BasicStroke((float) (cell * 0.06)));
		for (Coords coords : line)
		{
			double cx = gridX + coords.col * cell;
			double cy = gridY + coords.row * cell;
			g.setColor(new Color(Theme.VICTORY.getRed(), Theme.VICTORY.getGreen(), Theme.VICTORY.getBlue(), (int) (140 + 100 * pulse)));
			g.draw(new Ellipse2D.Double(cx - ring / 2, cy - ring / 2, ring, ring));
		}
	}

	private void paintHover(Graphics2D g)
	{
		if (hoverRow < 0 || !canPlayAt(hoverRow, hoverCol))
		{
			return;
		}
		paintStone(g, hoverRow, hoverCol, snapshot.toMove(), 1.0, 0.45f, 0.5);
		double cx = gridX + hoverCol * cell;
		double cy = gridY + hoverRow * cell;
		double size = cell * 0.18;
		g.setColor(Theme.ACCENT);
		g.setStroke(new BasicStroke((float) (cell * 0.05), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(cx - size, cy, cx + size, cy));
		g.draw(new Line2D.Double(cx, cy - size, cx, cy + size));
	}

	private boolean isBannerVisible()
	{
		return System.currentTimeMillis() - gameOverStart >= BANNER_DELAY_MS;
	}

	private void paintBanner(Graphics2D g)
	{
		if (!snapshot.isOver() || bannerHidden || !isBannerVisible())
		{
			return;
		}
		long elapsed = System.currentTimeMillis() - gameOverStart - BANNER_DELAY_MS;
		double fade = Math.min(1.0, elapsed / (double) BANNER_FADE_MS);
		double eased = 1 - Math.pow(1 - fade, 3);

		String title;
		CellColor winnerColor = null;
		if (snapshot.error() != null)
		{
			title = "Partie interrompue";
		}
		else if (snapshot.winner() == WinnerState.TIE)
		{
			title = "Égalité";
		}
		else
		{
			winnerColor = snapshot.winner() == WinnerState.BLACK ? CellColor.BLACK : CellColor.WHITE;
			title = (winnerColor == CellColor.BLACK ? "Les Noirs" : "Les Blancs") + " gagnent !";
		}
		String subtitle = snapshot.error() != null
				? snapshot.error()
				: snapshot.history().size() + " coups · " + winnerDescription(winnerColor);
		String hint = "Clique pour voir le plateau";

		Font titleFont = Theme.TITLE.deriveFont((float) Math.max(20, cell * 0.75));
		Font subtitleFont = Theme.BODY.deriveFont((float) Math.max(12, cell * 0.34));
		Font hintFont = Theme.SMALL.deriveFont((float) Math.max(10, cell * 0.26));
		FontMetrics titleMetrics = g.getFontMetrics(titleFont);
		FontMetrics subtitleMetrics = g.getFontMetrics(subtitleFont);
		FontMetrics hintMetrics = g.getFontMetrics(hintFont);

		double iconSize = winnerColor == null ? 0 : cell * 1.1;
		double padding = cell * 0.6;
		double textWidth = Math.max(titleMetrics.stringWidth(title) + (iconSize > 0 ? iconSize + padding / 2 : 0),
				Math.max(subtitleMetrics.stringWidth(subtitle), hintMetrics.stringWidth(hint)));
		double width = textWidth + 2 * padding;
		double height = titleMetrics.getHeight() + subtitleMetrics.getHeight() + hintMetrics.getHeight() + 2 * padding;
		double x = boardX + (boardSide - width) / 2;
		double y = bannerCenterY() - height / 2 + (1 - eased) * cell;

		Composite oldComposite = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) eased));
		g.setColor(new Color(0, 0, 0, 90));
		g.fill(new RoundRectangle2D.Double(x + 3, y + 8, width, height, cell, cell));
		g.setColor(new Color(Theme.PANEL.getRed(), Theme.PANEL.getGreen(), Theme.PANEL.getBlue(), 235));
		g.fill(new RoundRectangle2D.Double(x, y, width, height, cell, cell));
		g.setColor(winnerColor == null ? Theme.BORDER : Theme.VICTORY);
		g.setStroke(new BasicStroke(1.5f));
		g.draw(new RoundRectangle2D.Double(x, y, width, height, cell, cell));

		double titleWidth = titleMetrics.stringWidth(title) + (iconSize > 0 ? iconSize + padding / 2 : 0);
		double titleX = x + (width - titleWidth) / 2;
		double baseline = y + padding + titleMetrics.getAscent();
		if (iconSize > 0)
		{
			paintIconStone(g, titleX + iconSize / 2, baseline - titleMetrics.getAscent() / 2.0 + titleMetrics.getDescent() / 2.0,
					iconSize * 0.8, winnerColor);
			titleX += iconSize + padding / 2;
		}
		g.setFont(titleFont);
		g.setColor(Theme.TEXT);
		g.drawString(title, (float) titleX, (float) baseline);

		baseline += subtitleMetrics.getHeight();
		g.setFont(subtitleFont);
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(subtitle, (float) (x + (width - subtitleMetrics.stringWidth(subtitle)) / 2), (float) baseline);

		baseline += hintMetrics.getHeight();
		g.setFont(hintFont);
		g.setColor(new Color(Theme.TEXT_MUTED.getRed(), Theme.TEXT_MUTED.getGreen(), Theme.TEXT_MUTED.getBlue(), 160));
		g.drawString(hint, (float) (x + (width - hintMetrics.stringWidth(hint)) / 2), (float) baseline);
		g.setComposite(oldComposite);
	}

	/** Place le bandeau dans la moitié du plateau opposée à l'alignement gagnant, pour ne pas le cacher. */
	private double bannerCenterY()
	{
		List<Coords> line = snapshot.winningLine();
		double middle = boardY + boardSide / 2;
		if (line.isEmpty())
		{
			return middle;
		}
		double lineRow = (line.get(0).row + line.get(line.size() - 1).row) / 2.0;
		boolean lineInTopHalf = lineRow < (boardSize() - 1) / 2.0;
		return lineInTopHalf ? boardY + boardSide * BANNER_FAR_POSITION : boardY + boardSide * (1 - BANNER_FAR_POSITION);
	}

	private String winnerDescription(CellColor winnerColor)
	{
		if (winnerColor == null)
		{
			return "plateau plein";
		}
		PlayerType type = snapshot.typeOf(winnerColor);
		return type.isHuman() ? "bravo !" : "victoire de " + type.getLabel();
	}

	/** Petite pierre en relief, réutilisée par le panneau latéral. */
	static void paintIconStone(Graphics2D g, double cx, double cy, double diameter, CellColor color)
	{
		boolean black = color == CellColor.BLACK;
		g.setColor(new Color(0, 0, 0, 70));
		g.fill(new Ellipse2D.Double(cx - diameter / 2 + diameter * 0.04, cy - diameter / 2 + diameter * 0.08, diameter, diameter));
		g.setPaint(new RadialGradientPaint(
				new Point2D.Double(cx - diameter * 0.12, cy - diameter * 0.14), (float) (diameter * 0.72),
				new Point2D.Double(cx - diameter * 0.22, cy - diameter * 0.26),
				new float[] {0f, 0.55f, 1f},
				new Color[] {
						black ? Theme.BLACK_STONE_HIGHLIGHT : Theme.WHITE_STONE_HIGHLIGHT,
						black ? Theme.BLACK_STONE_BODY : Theme.WHITE_STONE_BODY,
						black ? Theme.BLACK_STONE_EDGE : Theme.WHITE_STONE_EDGE},
				MultipleGradientPaint.CycleMethod.NO_CYCLE));
		g.fill(new Ellipse2D.Double(cx - diameter / 2, cy - diameter / 2, diameter, diameter));
	}

	private static Color mix(Color a, Color b, double ratio)
	{
		double r = clamp(ratio);
		return new Color(
				(int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * r),
				(int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * r),
				(int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * r));
	}

	private static double clamp(double value)
	{
		return Math.max(0.0, Math.min(1.0, value));
	}
}
