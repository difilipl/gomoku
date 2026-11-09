package fr.esisar.in450.gomoku.player;

import fr.esisar.in450.gomoku.gamecore.AbstractPlayer;
import fr.esisar.in450.gomoku.gamecore.enums.CellColor;
import fr.esisar.in450.gomoku.gamecore.enums.Direction;
import fr.esisar.in450.gomoku.gamecore.model.Coords;
import fr.esisar.in450.gomoku.player.utils.Sequence;


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

    // Case en cours d'examen dans play().
    private int row;
    private int column;

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
        if(board.isEmpty()){

            row = (board.SIZE - 1) / 2;
            column = (board.SIZE - 1) / 2;
            return new Coords(row,column);
        }


        for (int i = 0; i < board.SIZE * board.SIZE; i++){
            row = i / board.SIZE;
            column = i % board.SIZE;
        }
    return new Coords();

    }



    /**
     * Score d'une suite selon sa longueur et ses extrémités libres.
     * 5 pierres ou plus : victoire, quels que soient les bouts.
     * Bloquée des deux côtés : ne pourra jamais faire 5, vaut 0.
     * Un seul bout libre : le score d'une suite ouverte divisé par HALF_OPEN_FACTOR.
     */
    private int scoreSequence(Sequence sequence) {
        int length = sequence.getLength();
        int openEnds = sequence.getOpenEnds();

        if (length >= WIN_LENGTH) return WIN_SCORE;
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