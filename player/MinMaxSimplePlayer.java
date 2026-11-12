package fr.esisar.in450.gomoku.player;

import fr.esisar.in450.gomoku.gamecore.AbstractPlayer;
import fr.esisar.in450.gomoku.gamecore.enums.CellColor;
import fr.esisar.in450.gomoku.gamecore.enums.Direction;
import fr.esisar.in450.gomoku.gamecore.model.Coords;
import fr.esisar.in450.gomoku.player.utils.Sequence;

import java.awt.*;


/**
 * IA Min-Max pour le Gomoku (plateau SIZE x SIZE, victoire à WIN_LENGTH pierres alignées).
 * Min-Max récursif de profondeur DEPTH, avec trois optimisations qui réduisent le nombre
 * de nœuds explorés (et donc d'appels à evaluate) :
 * - coups candidats : seules les cases proches d'une pierre sont essayées ;
 * - coupure sur victoire : un coup qui fait 5 termine la recherche dans sa branche ;
 * - élagage alpha-bêta : on arrête d'explorer un nœud dès qu'on a la preuve qu'un des
 *   deux joueurs ne le choisira jamais. Exact : le coup choisi ne change pas.
 */
public class MinMaxSimplePlayer extends AbstractPlayer {
    // Nombre de pierres alignées pour gagner.
    private static final int WIN_LENGTH = 5;
    // Une suite a au plus deux extrémités.
    private static final int MAX_OPEN_ENDS = 2;
    // Score d'une suite gagnante (5 ou plus), bien au-dessus de tous les autres motifs.
    private static final int WIN_SCORE = 1_000_000;
    // Score d'une suite qui ne pourra jamais faire 5.
    private static final int NO_SCORE = 0;
    // Score d'une suite ouverte des deux côtés, indexé par sa longueur (0 à 4).
    // Facteur 10 entre paliers : aucune accumulation de petits motifs ne bat un motif plus dangereux.
    private static final int[] OPEN_SCORE_BY_LENGTH = {0, 0, 10, 1_000, 100_000};
    // Une suite avec un seul bout libre vaut dix fois moins qu'avec deux.
    private static final int HALF_OPEN_FACTOR = 10;

    // true : barème asymétrique (l'adversaire joue juste après mon coup).
    // false : barème symétrique de la version de base. Permet de comparer les deux versions.
    private static final boolean USE_ASYMMETRIC_SCALE = true;
    // Barèmes asymétriques, indexés [longueur][extrémités libres] (longueur 0 à 4, bouts 0 à 2).
    // Ils dépendent de qui joue juste après la position évaluée (à la feuille).
    // Joueur qui attend : son 4 avec un seul bout libre sera simplement bouché.
    private static final int[][] WAITING_SCORES = {
            {0, 0, 0},
            {0, 0, 0},
            {0, 1, 10},
            {0, 100, 1_000},
            {0, 1_000, 100_000}
    };
    // Joueur qui va jouer : son 4 (même avec un seul bout libre) est une victoire assurée,
    // et son 3 ouvert devient un 4 ouvert.
    private static final int[][] TO_MOVE_SCORES = {
            {0, 0, 0},
            {0, 0, 0},
            {0, 1, 10},
            {0, 100, 50_000},
            {0, 500_000, 500_000}
    };

    // Nombre d'appels à evaluate() pendant le coup en cours (remis à zéro dans play()).
    private int moveEvaluations = 0;
    // Nombre d'appels à evaluate() depuis le début de la partie.
    private int totalEvaluations = 0;

    // Profondeur du Min-Max : nombre de demi-coups explorés (mon coup compte pour 1).
    private static final int DEPTH = 3;

    // Coups candidats : une case vide n'est essayée que si une pierre se trouve
    // à au plus NEIGHBOUR_DISTANCE cases d'elle, en ligne et en colonne.
    private static final int NEIGHBOUR_DISTANCE = 2;

    /**
     * Choisit le coup à jouer.
     * Plateau vide : on joue au centre, la case qui offre le plus d'alignements.
     * Sinon : on parcourt les SIZE * SIZE cases avec un seul indice i
     * (ligne = i / SIZE, colonne = i % SIZE) et on n'essaie que les coups candidats
     * (case vide proche d'une pierre). Un coup qui fait 5 est joué tout de suite.
     * Pour les autres : poser la pierre, lancer minMax, retirer la pierre,
     * garder le meilleur coup.
     */
    @Override
    public Coords play()
    {
        int bestScore = Integer.MIN_VALUE;
        Coords bestMove = null;
        moveEvaluations = 0;

        if(board.isEmpty()){
            int center = (board.SIZE - 1) / 2;
            return new Coords(center, center);
        }

        for (int i = 0; i < board.SIZE * board.SIZE; i++){
            int row = i / board.SIZE;
            int column = i % board.SIZE;
            // Case vide testée d'abord : c'est le test le moins cher.
            if(board.getCellColor(row, column) == null && isNeighbour(row, column)){
                board.setCellColor(row, column, playerColor);
                // Coup gagnant : rien ne peut faire mieux, on le joue sans chercher plus loin.
                if(isWinningMove(row, column, playerColor)){
                    board.setCellColor(row, column, null);
                    return new Coords(row, column);
                }
                // Mon coup est posé : c'est à l'adversaire, il reste DEPTH - 1 niveaux.
                // Fenêtre alpha-bêta la plus large possible : personne n'est encore sûr de rien.
                int score = minMax(DEPTH - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
                board.setCellColor(row, column, null);
                if(score > bestScore){
                    bestScore = score;
                    bestMove = new Coords(row, column);
                }
            }
        }
        System.out.println("Appels à evaluate : " + moveEvaluations + " ce coup, " + totalEvaluations + " depuis le début");
        return bestMove;
    }

    /**
     * Score du plateau, du point de vue de l'IA, en explorant encore depth demi-coups.
     * Profondeur 0 : on évalue le plateau. Sinon, on essaie chaque coup candidat :
     * à mon tour je garde le plus grand score, au tour adverse le plus petit
     * (l'adversaire choisit le coup le pire pour moi).
     * Si un coup fait 5, la partie est finie : on ne descend pas plus bas et on renvoie
     * WIN_SCORE + depth (victoire) ou -(WIN_SCORE + depth) (défaite). Plus depth est grand,
     * plus la fin est proche : l'IA préfère gagner vite et perdre le plus tard possible.
     * Élagage alpha-bêta : après chaque fils, le joueur de ce niveau met à jour sa borne
     * (alpha si c'est moi, bêta si c'est l'adversaire). Si alpha >= bêta, un des deux joueurs
     * a déjà mieux ailleurs et ne laissera jamais la partie arriver ici : on arrête la boucle.
     * La valeur renvoyée est alors une borne (au moins / au plus), pas la valeur exacte,
     * mais elle suffit au parent pour écarter ce coup.
     * @param depth nombre de demi-coups restant à explorer
     * @param isMyTurn vrai si c'est à l'IA de poser une pierre à ce niveau
     * @param alpha meilleur score que l'IA est déjà sûre d'obtenir sur le chemin actuel
     *              (plancher, ne fait que monter)
     * @param beta meilleur score que l'adversaire est déjà sûr d'obtenir sur le chemin actuel,
     *             donc le pire pour l'IA (plafond, ne fait que descendre)
     * @return score de la position, ou une borne si la boucle a été coupée
     */
    private int minMax(int depth, boolean isMyTurn, int alpha, int beta){
        int bestScore;
        CellColor color;
        if(depth == 0) return evaluate(isMyTurn);

        if(isMyTurn){
            bestScore = Integer.MIN_VALUE;
            color = playerColor;
        }
        else{
            bestScore = Integer.MAX_VALUE;
            color = getEnemyColor();
        }

        for (int i = 0; i < board.SIZE * board.SIZE; i++){
            int row = i / board.SIZE;
            int column = i % board.SIZE;
            if(board.getCellColor(row, column) == null && isNeighbour(row, column)){
                board.setCellColor(row, column, color);
                // Coupure sur victoire : testée avant l'appel récursif, sinon on ne coupe rien.
                if(isWinningMove(row, column, color)){
                    board.setCellColor(row, column, null);
                    if(isMyTurn) return WIN_SCORE + depth;
                    else return -(WIN_SCORE + depth);
                }
                // Le fils reçoit les bornes actuelles. Ce sont des copies (int) :
                // ce qu'il en fait ne modifie pas les nôtres, seule sa valeur remonte.
                int score = minMax(depth - 1, !(isMyTurn), alpha, beta);
                // Pierre retirée avant toute sortie de boucle, pour rendre le plateau intact.
                board.setCellColor(row, column, null);
                // MAX remonte son plancher, MIN baisse son plafond.
                if(isMyTurn) alpha = Math.max(score, alpha);
                else beta = Math.min(score, beta);
                // bestScore mis à jour avant la coupure : le score qui provoque la coupure
                // doit être pris en compte, sinon on renverrait la valeur de départ (±infini).
                if(isMyTurn ? bestScore < score : bestScore > score){
                    bestScore = score;
                }
                // Coupure alpha-bêta : fenêtre vide, les coups restants ne peuvent pas
                // changer le choix du parent.
                if(alpha >= beta) break;

            }
        }
        return bestScore;
    }


    /**
     * Évalue le plateau du point de vue de l'IA : mes points - points adverses.
     * Version naïve (référence) : on parcourt toutes les cases et, pour chaque pierre,
     * les 4 directions. Une suite n'est mesurée qu'à partir de sa première pierre
     * (isStarting), pour ne pas la compter plusieurs fois.
     * @param isMyTurn vrai si c'est à l'IA de jouer dans la position évaluée
     *                 (choisit le barème de chaque suite)
     */
    private int evaluate(boolean isMyTurn){
        moveEvaluations++;
        totalEvaluations++;

        int myPoints = 0;
        int enemyPoints = 0;

        for (int i = 0; i < board.SIZE * board.SIZE; i++){
            int row = i / board.SIZE;
            int column = i % board.SIZE;
            CellColor color = board.getCellColor(row, column);
            if(color == null) continue;

            for(Direction d : Direction.values()){
                if(isStarting(new Coords(row, column), d, color)){
                    boolean isMine = color == playerColor;
                    boolean ownerMovesNext = isMine == isMyTurn;
                    int score = scoreSequence(lenthcut(row, column, d, color), ownerMovesNext);
                    if(isMine) myPoints += score;
                    else enemyPoints += score;
                }
            }
        }
        return myPoints - enemyPoints;
    }

    /**
     * Dit si une case est un coup candidat : une pierre (de n'importe quelle couleur)
     * se trouve à au plus NEIGHBOUR_DISTANCE cases, en ligne et en colonne
     * (carré de (2 * NEIGHBOUR_DISTANCE + 1)² cases centré sur la case).
     * Les cases hors plateau sont sautées : !isColor(r, c, null) vaudrait true hors plateau.
     * On s'arrête dès la première pierre trouvée.
     */
    private boolean isNeighbour(int row, int column){
        for (int i = row - NEIGHBOUR_DISTANCE; i <= row + NEIGHBOUR_DISTANCE; i++) {
            for (int j = column - NEIGHBOUR_DISTANCE; j <= column + NEIGHBOUR_DISTANCE; j++) {
                if(!isValid(i) || !isValid(j)) continue;
                if(!isColor(i, j, null)) return true;
            }
        }
        return false;
    }

    /**
     * Dit si la pierre qu'on vient de poser en (row, column) fait un alignement de WIN_LENGTH.
     * Seule cette pierre peut créer un nouveau 5 : on ne regarde qu'autour d'elle.
     * Pour chaque direction, i va de -(WIN_LENGTH - 1) à WIN_LENGTH - 1 (les deux sens) :
     * le compteur repart à 0 sur une case qui n'est pas de la couleur (la suite doit être
     * continue) et on teste à chaque case, pour ne pas rater un 5 suivi d'une case vide.
     */
    private boolean isWinningMove(int row, int column, CellColor color){
        int length;
        for (Direction d : Direction.values()){
            length = 0;
            for (int i = -(WIN_LENGTH - 1); i <= WIN_LENGTH - 1; i++) {
                if(isColor(row + d.getRow() * i, column + d.getColumn() * i, color)){
                    length++;
                }
                else {
                    length = 0;
                }
                if(length >= WIN_LENGTH) return true;
            }
        }
        return false;
    }

    /**
     * Score d'une suite selon sa longueur et ses extrémités libres.
     * 5 pierres ou plus : victoire, quels que soient les bouts.
     * Bloquée des deux côtés : ne pourra jamais faire 5, vaut 0.
     * Un seul bout libre : le score d'une suite ouverte divisé par HALF_OPEN_FACTOR.
     * Avec USE_ASYMMETRIC_SCALE, on lit TO_MOVE_SCORES si le propriétaire de la suite
     * joue juste après, WAITING_SCORES sinon.
     * @param ownerMovesNext vrai si le propriétaire de la suite est le prochain à jouer
     */
    private int scoreSequence(Sequence sequence, boolean ownerMovesNext) {
        int length = sequence.getLength();
        int openEnds = sequence.getOpenEnds();

        if (length >= WIN_LENGTH) return WIN_SCORE;
        if (USE_ASYMMETRIC_SCALE) {
            return ownerMovesNext ? TO_MOVE_SCORES[length][openEnds] : WAITING_SCORES[length][openEnds];
        }
        if (openEnds == 0) return NO_SCORE;

        int openScore = OPEN_SCORE_BY_LENGTH[length];
        return openEnds == MAX_OPEN_ENDS ? openScore : openScore / HALF_OPEN_FACTOR;
    }

    /**
     * Mesure la suite de pierres de la couleur donnée qui commence en (row, column)
     * et avance dans la direction d.
     * Extrémité libre : la case juste après la fin, ou juste avant le début,
     * est vide et sur le plateau.
     * @return la longueur de la suite et son nombre d'extrémités libres (0, 1 ou 2)
     */
    private Sequence lenthcut(int row, int column, Direction d, CellColor color){
        int lenth = 0;
        int cut = 0;
        int r = row;
        int c = column;
        while(isColor(r, c,color)) {
            lenth++;
            r += d.getRow();
            c += d.getColumn();

        }
        if(!(isColor(r,c,null))) cut += 1;
        if(!(isColor(row - d.getRow(), column - d.getColumn(), null))) cut += 1;
        return new Sequence(lenth,MAX_OPEN_ENDS - cut);
    }


    /**
     * Vrai si la case (row, column) est sur le plateau et contient la couleur donnée.
     * Avec color = null, teste si la case est vide. Hors plateau : toujours faux.
     */
    private boolean isColor(int row, int column, CellColor color){
        if(isValid(row) && isValid(column)) return color == board.getCellColor(row,column);
        return false;
    }

    /**
     * Couleur de l'adversaire. CellColor n'a que deux valeurs, et playerColor n'est
     * connu qu'après setColor, d'où une méthode plutôt qu'un attribut fixé au constructeur.
     */
    private CellColor getEnemyColor(){
        return playerColor == CellColor.BLACK ? CellColor.WHITE : CellColor.BLACK;
    }


    /**
     * Vrai si la pierre est le début d'une suite dans la direction d,
     * c'est-à-dire si la case juste avant n'a pas la même couleur.
     * Évite de compter une suite de 4 aussi comme un 3, un 2, etc.
     */
    private boolean isStarting(Coords pion, Direction d, CellColor color){
        return !(isColor(pion.row - d.getRow(), pion.col - d.getColumn(), color));
    }


    /** Vrai si l'indice (ligne ou colonne) est dans le plateau. */
    private boolean isValid(int rowOrCol)
    {
        return rowOrCol>=0 && rowOrCol <board.SIZE;
    }
































}