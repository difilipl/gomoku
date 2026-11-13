package fr.esisar.in450.gomoku.gui;

import java.util.List;

import fr.esisar.in450.gomoku.gamecore.enums.CellColor;
import fr.esisar.in450.gomoku.gamecore.enums.WinnerState;
import fr.esisar.in450.gomoku.gamecore.model.Coords;

/**
 * Photographie immuable d'une partie, envoyée par le fil de jeu à l'interface.
 * L'interface ne lit jamais le plateau partagé : les IA y posent et retirent des pierres
 * pendant leur recherche, ce qui ferait clignoter l'affichage.
 *
 * @param gameId       numéro de la partie, pour ignorer les messages d'une partie abandonnée
 * @param cells        copie des cases du plateau
 * @param history      coups joués, dans l'ordre
 * @param toMove       couleur du joueur dont c'est le tour
 * @param white        type du joueur blanc
 * @param black        type du joueur noir
 * @param winner       état de fin de partie
 * @param winningLine  pierres de l'alignement gagnant, vide sinon
 * @param thinking     vrai si une IA est en train de chercher son coup
 * @param thinkStartMs instant où l'IA a commencé à chercher
 * @param error        message d'erreur, ou null
 */
public record GameSnapshot(
		long gameId,
		CellColor[][] cells,
		List<Move> history,
		CellColor toMove,
		PlayerType white,
		PlayerType black,
		WinnerState winner,
		List<Coords> winningLine,
		boolean thinking,
		long thinkStartMs,
		String error)
{
	/**
	 * Un coup joué.
	 *
	 * @param row        ligne, à partir de 0
	 * @param col        colonne, à partir de 0
	 * @param color      couleur de la pierre
	 * @param durationMs temps de réflexion du joueur
	 * @param byAi       vrai si le coup vient d'une IA
	 */
	public record Move(int row, int col, CellColor color, long durationMs, boolean byAi)
	{
	}

	/** Type du joueur d'une couleur donnée. */
	public PlayerType typeOf(CellColor color)
	{
		return color == CellColor.WHITE ? white : black;
	}

	/** Vrai si la partie est terminée. */
	public boolean isOver()
	{
		return winner != WinnerState.NONE || error != null;
	}

	/** Vrai si l'humain peut poser une pierre maintenant. */
	public boolean isHumanTurn()
	{
		return !isOver() && typeOf(toMove).isHuman();
	}

	/** Dernier coup joué, ou null en début de partie. */
	public Move lastMove()
	{
		return history.isEmpty() ? null : history.get(history.size() - 1);
	}

	/** Vrai si au moins un coup humain peut être annulé. */
	public boolean canUndo()
	{
		if (thinking && !isOver())
		{
			return false;
		}
		for (Move move : history)
		{
			if (!move.byAi())
			{
				return true;
			}
		}
		return false;
	}

	/** Temps total de réflexion d'une couleur, en millisecondes. */
	public long totalTime(CellColor color)
	{
		long total = 0;
		for (Move move : history)
		{
			if (move.color() == color)
			{
				total += move.durationMs();
			}
		}
		return total;
	}
}
