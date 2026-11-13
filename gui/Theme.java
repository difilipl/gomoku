package fr.esisar.in450.gomoku.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Set;

/**
 * Couleurs et polices de l'interface, regroupées pour garder un style cohérent.
 */
public final class Theme
{
	/** Fond de la zone du plateau, au centre. */
	public static final Color BACKGROUND_CENTER = new Color(0x34, 0x3A, 0x44);
	/** Fond de la zone du plateau, sur les bords. */
	public static final Color BACKGROUND_EDGE = new Color(0x16, 0x19, 0x1F);

	/** Bois clair du plateau. */
	public static final Color WOOD_LIGHT = new Color(0xE8, 0xC0, 0x7D);
	/** Bois foncé du plateau. */
	public static final Color WOOD_DARK = new Color(0xC4, 0x8E, 0x4C);
	/** Veines du bois. */
	public static final Color WOOD_GRAIN = new Color(0x8A, 0x5A, 0x2B);
	/** Lignes de la grille. */
	public static final Color GRID = new Color(0x3B, 0x26, 0x12, 200);
	/** Repères de coordonnées autour de la grille. */
	public static final Color COORDINATES = new Color(0x4A, 0x30, 0x18, 190);

	/** Reflet d'une pierre noire. */
	public static final Color BLACK_STONE_HIGHLIGHT = new Color(0x6B, 0x6E, 0x75);
	/** Corps d'une pierre noire. */
	public static final Color BLACK_STONE_BODY = new Color(0x1A, 0x1B, 0x1E);
	/** Bord d'une pierre noire. */
	public static final Color BLACK_STONE_EDGE = new Color(0x05, 0x05, 0x06);
	/** Reflet d'une pierre blanche. */
	public static final Color WHITE_STONE_HIGHLIGHT = new Color(0xFF, 0xFF, 0xFF);
	/** Corps d'une pierre blanche. */
	public static final Color WHITE_STONE_BODY = new Color(0xEE, 0xEC, 0xE6);
	/** Bord d'une pierre blanche. */
	public static final Color WHITE_STONE_EDGE = new Color(0xB4, 0xB0, 0xA8);

	/** Couleur d'accent : dernier coup, boutons principaux. */
	public static final Color ACCENT = new Color(0xE2, 0x5B, 0x45);
	/** Couleur de l'alignement gagnant. */
	public static final Color VICTORY = new Color(0xFF, 0xD2, 0x4A);

	/** Fond du panneau latéral. */
	public static final Color PANEL = new Color(0x1F, 0x23, 0x2A);
	/** Fond des cartes du panneau latéral. */
	public static final Color CARD = new Color(0x2A, 0x2F, 0x38);
	/** Fond d'une carte mise en avant (joueur dont c'est le tour). */
	public static final Color CARD_ACTIVE = new Color(0x36, 0x3C, 0x48);
	/** Bordure discrète. */
	public static final Color BORDER = new Color(0x3A, 0x40, 0x4C);
	/** Texte principal. */
	public static final Color TEXT = new Color(0xEC, 0xEE, 0xF2);
	/** Texte secondaire. */
	public static final Color TEXT_MUTED = new Color(0x9A, 0xA1, 0xAE);

	private static final String FONT_FAMILY = pickFontFamily();

	/** Police du titre. */
	public static final Font TITLE = new Font(FONT_FAMILY, Font.BOLD, 28);
	/** Police des sous-titres de section. */
	public static final Font SECTION = new Font(FONT_FAMILY, Font.BOLD, 11);
	/** Police du texte courant. */
	public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
	/** Police du texte courant en gras. */
	public static final Font BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
	/** Police des petits textes. */
	public static final Font SMALL = new Font(FONT_FAMILY, Font.PLAIN, 11);
	/** Police des grands chiffres. */
	public static final Font NUMBER = new Font(FONT_FAMILY, Font.BOLD, 20);

	private Theme()
	{
	}

	private static String pickFontFamily()
	{
		Set<String> available = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
		for (String candidate : new String[] {"Segoe UI", "Inter", "Helvetica Neue", "Ubuntu", "DejaVu Sans"})
		{
			if (available.contains(candidate))
			{
				return candidate;
			}
		}
		return Font.SANS_SERIF;
	}
}
