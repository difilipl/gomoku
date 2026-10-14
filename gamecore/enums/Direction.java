package fr.esisar.in450.gomoku.gamecore.enums;

public enum Direction {
    N(0,-1),
    NE(-1,-1),
    E(-1,0),
    SE(-1,1),
    S(0,1),
    SW(1,1),
    W(1,0),
    NW(1,-1),
    ;

    private final int raw;
    private final int column;

    Direction(int x, int y) {
        this.raw = x;
        this.column = y;
    }

    public int getRaw() {return this.raw;}
    public int getcolumn() {return this.column;}
}
