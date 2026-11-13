package fr.esisar.in450.gomoku.gui;

import java.util.function.Supplier;

import fr.esisar.in450.gomoku.gamecore.AbstractPlayer;
import fr.esisar.in450.gomoku.player.BasicAIPlayer;
import fr.esisar.in450.gomoku.player.DummyPlayer;
import fr.esisar.in450.gomoku.player.MinMaxSimplePlayer;
import fr.esisar.in450.gomoku.player.SelectiveMinMaxPlayer;

/**
 * Types de joueurs proposés dans l'interface.
 * Chaque type sait créer un nouveau joueur, pour repartir d'un état propre à chaque partie.
 */
public enum PlayerType
{
	/** Joueur humain : il joue en cliquant sur le plateau. */
	HUMAN("Humain", "Clique sur une intersection", null),

	/** Min-Max exact avec alpha-bêta, coups candidats et tri des coups. */
	MINMAX("IA Min-Max", "Exacte, alpha-bêta", MinMaxSimplePlayer::new),

	/** Min-Max sélectif : seuls les meilleurs coups sont explorés, mais beaucoup plus profond. */
	SELECTIVE("IA sélective", "Profonde, faisceau étroit", SelectiveMinMaxPlayer::new),

	/** IA fournie avec la base de code. */
	BASIC("IA basique", "Fournie avec le sujet", BasicAIPlayer::new),

	/** Joueur de test fourni avec la base de code. */
	DUMMY("IA naïve", "Joueur de test du sujet", DummyPlayer::new);

	private final String label;
	private final String description;
	private final Supplier<AbstractPlayer> factory;

	PlayerType(String label, String description, Supplier<AbstractPlayer> factory)
	{
		this.label = label;
		this.description = description;
		this.factory = factory;
	}

	/** Nom affiché dans l'interface. */
	public String getLabel()
	{
		return label;
	}

	/** Courte description affichée sous le nom. */
	public String getDescription()
	{
		return description;
	}

	/** Vrai si ce type correspond à un joueur humain. */
	public boolean isHuman()
	{
		return factory == null;
	}

	/**
	 * Crée un nouveau joueur de ce type.
	 * Le joueur humain est fourni à part par le contrôleur, car il est relié aux clics.
	 */
	public AbstractPlayer createPlayer()
	{
		return factory.get();
	}
}
