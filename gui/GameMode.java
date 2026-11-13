package fr.esisar.in450.gomoku.gui;

/**
 * Modes de jeu proposés en un clic : l'humain contre une des deux IA du projet, ou deux humains.
 */
public enum GameMode
{
	/** Humain contre l'IA Min-Max exacte. */
	VS_EXACT("IA exacte", PlayerType.MINMAX),

	/** Humain contre l'IA sélective. */
	VS_SELECTIVE("IA sélective", PlayerType.SELECTIVE),

	/** Deux humains sur le même plateau. */
	HUMAN_VS_HUMAN("2 joueurs", PlayerType.HUMAN);

	private final String label;
	private final PlayerType opponent;

	GameMode(String label, PlayerType opponent)
	{
		this.label = label;
		this.opponent = opponent;
	}

	/** Nom affiché sur le bouton du mode. */
	public String getLabel()
	{
		return label;
	}

	/** Type du joueur qui affronte l'humain. */
	public PlayerType getOpponent()
	{
		return opponent;
	}

	/** Mode correspondant à deux types de joueurs, ou null si aucun mode ne correspond. */
	public static GameMode of(PlayerType white, PlayerType black)
	{
		for (GameMode mode : values())
		{
			boolean humanWhite = white == PlayerType.HUMAN && black == mode.opponent;
			boolean humanBlack = black == PlayerType.HUMAN && white == mode.opponent;
			if (humanWhite || humanBlack)
			{
				return mode;
			}
		}
		return null;
	}
}
