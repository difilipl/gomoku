package fr.esisar.in450.gomoku.player.utils;

/**
 * Une suite de pierres alignées : sa longueur et son nombre d'extrémités libres (0, 1 ou 2).
 * Objet immuable, renvoyé par le comptage et lu par le barème.
 */
public class Sequence {
    private final int length;
    private final int openEnds;

    public Sequence(int length, int openEnds) {
        this.length = length;
        this.openEnds = openEnds;
    }

    public int getLength() {
        return length;
    }

    public int getOpenEnds() {
        return openEnds;
    }
}
