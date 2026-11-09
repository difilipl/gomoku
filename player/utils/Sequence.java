package fr.esisar.in450.gomoku.player.utils;

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
