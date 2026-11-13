package fr.esisar.in450.gomoku.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import fr.esisar.in450.gomoku.gamecore.AbstractPlayer;
import fr.esisar.in450.gomoku.gamecore.enums.CellColor;
import fr.esisar.in450.gomoku.gamecore.enums.WinnerState;
import fr.esisar.in450.gomoku.gamecore.model.Coords;
import fr.esisar.in450.gomoku.gamecore.model.GomokuBoard;

/**
 * Fait tourner une partie dans un fil séparé, pour que la fenêtre ne gèle pas
 * pendant que l'IA réfléchit. Reprend la boucle de LocalGameManager (les Blancs commencent),
 * avec en plus l'annulation de coup et l'abandon d'une partie en cours.
 */
public class GameController
{
	/** Nombre de pierres alignées pour gagner. */
	public static final int WIN_LENGTH = 5;

	/** Pause entre deux coups quand deux IA s'affrontent, pour pouvoir suivre la partie. */
	private static final long AI_VS_AI_DELAY_MS = 350;

	/** Directions de recherche de l'alignement gagnant : droite, bas, diagonale ↘, diagonale ↙. */
	private static final int[][] LINE_DIRECTIONS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

	private final Consumer<GameSnapshot> listener;
	private volatile Session current;
	private long nextGameId = 0;

	/**
	 * @param listener appelé dans le fil de Swing à chaque changement de la partie
	 */
	public GameController(Consumer<GameSnapshot> listener)
	{
		this.listener = listener;
	}

	/** Abandonne la partie en cours et en lance une nouvelle. */
	public void startNewGame(PlayerType white, PlayerType black)
	{
		Session old = current;
		if (old != null)
		{
			old.cancel();
		}
		Session session = new Session(++nextGameId, white, black);
		current = session;
		session.start();
	}

	/** Transmet un clic sur une intersection au joueur humain dont c'est le tour. */
	public void humanMove(int row, int col)
	{
		Session session = current;
		if (session != null)
		{
			session.humanMove(row, col);
		}
	}

	/** Annule le dernier coup humain (et les réponses de l'IA qui ont suivi). */
	public void undo()
	{
		Session session = current;
		if (session != null)
		{
			session.undo();
		}
	}

	/**
	 * Une partie : son plateau, ses joueurs et son fil.
	 * Chaque partie a son propre plateau, donc une IA abandonnée en pleine réflexion
	 * ne peut pas abîmer la partie suivante.
	 */
	private final class Session implements Runnable
	{
		private final long gameId;
		private final PlayerType whiteType;
		private final PlayerType blackType;
		private final GomokuBoard board = new GomokuBoard();
		private final Map<CellColor, AbstractPlayer> players = new EnumMap<>(CellColor.class);
		private final Map<CellColor, GuiHumanPlayer> humans = new EnumMap<>(CellColor.class);
		private final List<GameSnapshot.Move> history = new ArrayList<>();
		private final Thread thread;
		private volatile boolean cancelled = false;
		private volatile GameSnapshot lastSnapshot;

		Session(long gameId, PlayerType whiteType, PlayerType blackType)
		{
			this.gameId = gameId;
			this.whiteType = whiteType;
			this.blackType = blackType;
			createPlayer(CellColor.WHITE, whiteType);
			createPlayer(CellColor.BLACK, blackType);
			thread = new Thread(this, "gomoku-game-" + gameId);
			thread.setDaemon(true);
		}

		private void createPlayer(CellColor color, PlayerType type)
		{
			AbstractPlayer player;
			if (type.isHuman())
			{
				GuiHumanPlayer human = new GuiHumanPlayer();
				humans.put(color, human);
				player = human;
			}
			else
			{
				player = type.createPlayer();
			}
			player.setColor(color);
			player.setBoard(board);
			players.put(color, player);
		}

		void start()
		{
			publish(false, 0, null);
			thread.start();
		}

		void cancel()
		{
			cancelled = true;
			thread.interrupt();
		}

		void humanMove(int row, int col)
		{
			GameSnapshot snapshot = lastSnapshot;
			if (snapshot == null || !snapshot.isHumanTurn() || snapshot.cells()[row][col] != null)
			{
				return;
			}
			humans.get(snapshot.toMove()).submitMove(row, col);
		}

		void undo()
		{
			GameSnapshot snapshot = lastSnapshot;
			if (snapshot == null || !snapshot.canUndo())
			{
				return;
			}
			if (snapshot.isOver())
			{
				undoMoves();
				new Thread(this, "gomoku-game-" + gameId + "-resumed").start();
				return;
			}
			if (snapshot.isHumanTurn())
			{
				humans.get(snapshot.toMove()).submitUndo();
			}
		}

		private CellColor colorToMove()
		{
			return history.size() % 2 == 0 ? CellColor.WHITE : CellColor.BLACK;
		}

		private boolean bothAi()
		{
			return humans.isEmpty();
		}

		@Override
		public void run()
		{
			try
			{
				playLoop();
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			catch (RuntimeException e)
			{
				if (!cancelled)
				{
					publish(false, 0, "Erreur pendant la partie : " + e);
				}
			}
		}

		private void playLoop() throws InterruptedException
		{
			while (!cancelled && board.getWinnerState() == WinnerState.NONE)
			{
				CellColor color = colorToMove();
				AbstractPlayer player = players.get(color);
				GuiHumanPlayer human = humans.get(color);
				Coords move;
				long duration;
				if (human != null)
				{
					human.clearPending();
					publish(false, 0, null);
					long start = System.currentTimeMillis();
					GuiHumanPlayer.Action action = human.nextAction();
					duration = System.currentTimeMillis() - start;
					if (action.isUndo())
					{
						undoMoves();
						continue;
					}
					move = action.move();
				}
				else
				{
					if (bothAi() && !history.isEmpty())
					{
						Thread.sleep(AI_VS_AI_DELAY_MS);
					}
					long start = System.currentTimeMillis();
					publish(true, start, null);
					move = player.play();
					duration = System.currentTimeMillis() - start;
				}
				if (cancelled)
				{
					return;
				}
				if (!isValid(move))
				{
					publish(false, 0, "Coup invalide de " + (color == CellColor.WHITE ? "Blanc" : "Noir"));
					return;
				}
				board.setCellColor(move.row, move.col, color);
				history.add(new GameSnapshot.Move(move.row, move.col, color, duration, human == null));
			}
			if (!cancelled)
			{
				publish(false, 0, null);
			}
		}

		private boolean isValid(Coords move)
		{
			return move != null
					&& move.row >= 0 && move.row < board.SIZE
					&& move.col >= 0 && move.col < board.SIZE
					&& board.getCellColor(move.row, move.col) == null;
		}

		/** Retire les réponses de l'IA puis le dernier coup humain. */
		private void undoMoves()
		{
			while (!history.isEmpty() && history.get(history.size() - 1).byAi())
			{
				removeLastMove();
			}
			if (!history.isEmpty())
			{
				removeLastMove();
			}
		}

		private void removeLastMove()
		{
			GameSnapshot.Move move = history.remove(history.size() - 1);
			board.setCellColor(move.row(), move.col(), null);
		}

		private void publish(boolean thinking, long thinkStartMs, String error)
		{
			int size = board.SIZE;
			CellColor[][] cells = new CellColor[size][size];
			for (int row = 0; row < size; row++)
			{
				for (int col = 0; col < size; col++)
				{
					cells[row][col] = thinking ? cellFromHistory(row, col) : board.getCellColor(row, col);
				}
			}
			WinnerState winner = thinking ? WinnerState.NONE : board.getWinnerState();
			GameSnapshot snapshot = new GameSnapshot(
					gameId,
					cells,
					List.copyOf(history),
					colorToMove(),
					whiteType,
					blackType,
					winner,
					findWinningLine(cells),
					thinking,
					thinkStartMs,
					error);
			lastSnapshot = snapshot;
			SwingUtilities.invokeLater(() ->
			{
				if (current == this)
				{
					listener.accept(snapshot);
				}
			});
		}

		/** Pendant la réflexion de l'IA, le plateau partagé contient des pierres d'essai : on repart de l'historique. */
		private CellColor cellFromHistory(int row, int col)
		{
			for (GameSnapshot.Move move : history)
			{
				if (move.row() == row && move.col() == col)
				{
					return move.color();
				}
			}
			return null;
		}
	}

	/** Cherche un alignement d'au moins WIN_LENGTH pierres et renvoie ses cases. */
	static List<Coords> findWinningLine(CellColor[][] cells)
	{
		int size = cells.length;
		for (int row = 0; row < size; row++)
		{
			for (int col = 0; col < size; col++)
			{
				CellColor color = cells[row][col];
				if (color == null)
				{
					continue;
				}
				for (int[] direction : LINE_DIRECTIONS)
				{
					int previousRow = row - direction[0];
					int previousCol = col - direction[1];
					if (inside(previousRow, previousCol, size) && cells[previousRow][previousCol] == color)
					{
						continue;
					}
					List<Coords> line = new ArrayList<>();
					int r = row;
					int c = col;
					while (inside(r, c, size) && cells[r][c] == color)
					{
						line.add(new Coords(r, c));
						r += direction[0];
						c += direction[1];
					}
					if (line.size() >= WIN_LENGTH)
					{
						return Collections.unmodifiableList(line);
					}
				}
			}
		}
		return List.of();
	}

	private static boolean inside(int row, int col, int size)
	{
		return row >= 0 && row < size && col >= 0 && col < size;
	}
}
