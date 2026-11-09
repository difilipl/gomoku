package fr.esisar.in450.gomoku.gamecore.enums;

public enum Direction {
    W(0,-1),
    SE(1,1),
    S(1,0),
    SW(1,-1);

    private final int row;
    private final int column;

    Direction(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getRow() {return this.row;}
    public int getColumn() {return this.column;}
}
