package fr.esisar.in450.gomoku.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * Bouton plat aux coins arrondis, dessiné à la main pour s'accorder au thème sombre.
 */
public class FlatButton extends JButton
{
	/** Rayon des coins. */
	private static final int CORNER = 12;
	/** Hauteur du bouton. */
	private static final int HEIGHT = 40;
	/** Éclaircissement au survol. */
	private static final float HOVER_BRIGHTEN = 0.12f;
	/** Assombrissement au clic. */
	private static final float PRESS_DARKEN = 0.15f;

	private final Color background;
	private final Color foreground;

	/**
	 * @param text       texte du bouton
	 * @param background couleur de fond
	 * @param foreground couleur du texte
	 */
	public FlatButton(String text, Color background, Color foreground)
	{
		super(text);
		this.background = background;
		this.foreground = foreground;
		setFont(Theme.BODY_BOLD);
		setContentAreaFilled(false);
		setFocusPainted(false);
		setBorderPainted(false);
		setBorder(BorderFactory.createEmptyBorder());
		setRolloverEnabled(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setPreferredSize(new Dimension(120, HEIGHT));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		Color fill = background;
		if (!isEnabled())
		{
			fill = new Color(background.getRed(), background.getGreen(), background.getBlue(), 90);
		}
		else if (getModel().isPressed())
		{
			fill = shade(background, -PRESS_DARKEN);
		}
		else if (getModel().isRollover())
		{
			fill = shade(background, HOVER_BRIGHTEN);
		}
		g.setColor(fill);
		g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), CORNER, CORNER));

		g.setFont(getFont());
		FontMetrics metrics = g.getFontMetrics();
		Color text = isEnabled() ? foreground : Theme.TEXT_MUTED;
		g.setColor(text);
		String label = getText();
		g.drawString(label, (getWidth() - metrics.stringWidth(label)) / 2,
				(getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
		g.dispose();
	}

	private static Color shade(Color color, float amount)
	{
		int target = amount > 0 ? 255 : 0;
		float ratio = Math.abs(amount);
		return new Color(
				Math.round(color.getRed() + (target - color.getRed()) * ratio),
				Math.round(color.getGreen() + (target - color.getGreen()) * ratio),
				Math.round(color.getBlue() + (target - color.getBlue()) * ratio),
				color.getAlpha());
	}
}
