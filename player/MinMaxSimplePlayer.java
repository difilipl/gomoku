package fr.esisar.in450.gomoku.player;

import fr.esisar.in450.gomoku.gamecore.AbstractPlayer;
import fr.esisar.in450.gomoku.gamecore.enums.CellColor;
import fr.esisar.in450.gomoku.gamecore.enums.Direction;
import fr.esisar.in450.gomoku.gamecore.model.Coords;
import fr.esisar.in450.gomoku.player.utils.Sequence;

import java.awt.*;


/**
 * IA Min-Max pour le Gomoku (plateau SIZE x SIZE, victoire à WIN_LENGTH pierres alignées).
 * Version de base : profondeur 1, toutes les cases vides sont essayées.
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
    // Mes suites : mon 4 avec un seul bout libre sera simplement bouché par l'adversaire.
    private static final int[][] MY_SCORES = {
            {0, 0, 0},
            {0, 0, 0},
            {0, 1, 10},
            {0, 100, 1_000},
            {0, 1_000, 100_000}
    };
    // Suites adverses : il joue juste après, donc son 4 (même avec un seul bout libre)
    // est une victoire assurée, et son 3 ouvert devient un 4 ouvert.
    private static final int[][] ENEMY_SCORES = {
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

    /**
     * Choisit le coup à jouer.
     * Plateau vide : on joue au centre, la case qui offre le plus d'alignements.
     * Sinon : on parcourt les SIZE * SIZE cases avec un seul indice i
     * (ligne = i / SIZE, colonne = i % SIZE). À compléter : poser la pierre,
     * évaluer le plateau, retirer la pierre, garder le meilleur coup.
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
            if(board.getCellColor(row, column) == null ){
                board.setCellColor(row, column, playerColor);
                int score = evaluate();
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
     * Évalue le plateau du point de vue de l'IA : mes points - points adverses.
     * Version naïve (référence) : on parcourt toutes les cases et, pour chaque pierre,
     * les 4 directions. Une suite n'est mesurée qu'à partir de sa première pierre
     * (isStarting), pour ne pas la compter plusieurs fois.
     */
    private int evaluate(){
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
                    int score = scoreSequence(lenthcut(row, column, d, color), isMine);
                    if(isMine) myPoints += score;
                    else enemyPoints += score;
                }
            }
        }
        return myPoints - enemyPoints;
    }

    /**
     * Score d'une suite selon sa longueur et ses extrémités libres.
     * 5 pierres ou plus : victoire, quels que soient les bouts.
     * Bloquée des deux côtés : ne pourra jamais faire 5, vaut 0.
     * Un seul bout libre : le score d'une suite ouverte divisé par HALF_OPEN_FACTOR.
     * Avec USE_ASYMMETRIC_SCALE, on lit MY_SCORES ou ENEMY_SCORES selon isMine.
     * @param isMine vrai si la suite est de la couleur de l'IA
     */
    private int scoreSequence(Sequence sequence, boolean isMine) {
        int length = sequence.getLength();
        int openEnds = sequence.getOpenEnds();

        if (length >= WIN_LENGTH) return WIN_SCORE;
        if (USE_ASYMMETRIC_SCALE) {
            return isMine ? MY_SCORES[length][openEnds] : ENEMY_SCORES[length][openEnds];
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