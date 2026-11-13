package fr.esisar.in450.gomoku.gui;

/**
 * Modes de jeu proposés en un clic.
 * Un mode est une paire de joueurs ; les couleurs peuvent être échangées sans changer de mode.
 */
public enum GameMode
{
	/** Humain contre l'IA Min-Max exacte. */
	VS_EXACT("IA exacte", PlayerType.HUMAN, PlayerType.MINMAX),

	/** Humain contre l'IA sélective. */
	VS_SELECTIVE("IA sélective", PlayerType.HUMAN, PlayerType.SELECTIVE),

	/** Deux humains sur le même plateau. */
	HUMAN_VS_HUMAN("2 joueurs", PlayerType.HUMAN, PlayerType.HUMAN),

	/** Les deux IA du projet s'affrontent. */
	AI_VS_AI("IA vs IA", PlayerType.MINMAX, PlayerType.SELECTIVE);

	private final String label;
	private final PlayerType first;
	private final PlayerType second;

	GameMode(String label, PlayerType first, PlayerType second)
	{
		this.label = label;
		this.first = first;
		this.second = second;
	}

	/** Nom affiché sur le bouton du mode. */
	public String getLabel()
	{
		return label;
	}

	/** Joueur placé chez les Blancs par défaut (l'humain s'il y en a un). */
	public PlayerType getFirst()
	{
		return first;
	}

	/** Joueur placé chez les Noirs par défaut. */
	public PlayerType getSecond()
	{
		return second;
	}

	/** Vrai si ce mode correspond à ces deux joueurs, dans un sens ou dans l'autre. */
	public boolean matches(PlayerType white, PlayerType black)
	{
		return (white == first && black == second) || (white == second && black == first);
	}

	/** Mode correspondant à deux types de joueurs, ou null si aucun mode ne correspond. */
	public static GameMode of(PlayerType white, PlayerType black)
	{
		for (GameMode mode : values())
		{
			if (mode.matches(white, black))
			{
				return mode;
			}
		}
		return null;
	}
}
