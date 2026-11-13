package fr.esisar.in450.gomoku.gui;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import fr.esisar.in450.gomoku.gamecore.AbstractPlayer;
import fr.esisar.in450.gomoku.gamecore.model.Coords;

/**
 * Joueur humain branché sur l'interface graphique.
 * Le fil de la partie attend dans {@link #nextAction()} ; l'interface dépose les clics
 * et les demandes d'annulation dans une file.
 */
public class GuiHumanPlayer extends AbstractPlayer
{
	/** Action envoyée par l'interface : un coup, ou une demande d'annulation si move vaut null. */
	public record Action(Coords move)
	{
		/** Vrai si l'action demande d'annuler le dernier coup. */
		public boolean isUndo()
		{
			return move == null;
		}
	}

	private final BlockingQueue<Action> actions = new LinkedBlockingQueue<>();

	/** Dépose un coup choisi par un clic. */
	public void submitMove(int row, int col)
	{
		actions.offer(new Action(new Coords(row, col)));
	}

	/** Dépose une demande d'annulation. */
	public void submitUndo()
	{
		actions.offer(new Action(null));
	}

	/** Vide les actions en attente (clics faits pendant que ce n'était pas le tour de l'humain). */
	public void clearPending()
	{
		actions.clear();
	}

	/**
	 * Attend la prochaine action de l'humain.
	 *
	 * @throws InterruptedException si la partie est abandonnée pendant l'attente
	 */
	public Action nextAction() throws InterruptedException
	{
		return actions.take();
	}

	/**
	 * Le contrôleur graphique utilise {@link #nextAction()} à la place, pour gérer l'annulation.
	 * Cette méthode reste utilisable avec un gestionnaire de partie classique.
	 */
	@Override
	public Coords play()
	{
		try
		{
			Action action = nextAction();
			while (action.isUndo())
			{
				action = nextAction();
			}
			return action.move();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return null;
		}
	}
}
