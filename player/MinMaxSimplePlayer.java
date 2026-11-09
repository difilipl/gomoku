package fr.esisar.in450.gomoku.player;

import fr.esisar.in450.gomoku.gamecore.AbstractPlayer;
import fr.esisar.in450.gomoku.gamecore.enums.CellColor;
import fr.esisar.in450.gomoku.gamecore.enums.Direction;
import fr.esisar.in450.gomoku.gamecore.model.Coords;
import fr.esisar.in450.gomoku.player.utils.Sequence;


public class MinMaxSimplePlayer extends AbstractPlayer {
    private int row;
    private int column;

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
        return new Sequence(lenth,2-cut);
    }


    private boolean isColor(int row, int column, CellColor color){
        if(isValid(row) && isValid(column)) return color == board.getCellColor(row,column);
        return false;
    }


    private boolean isStarting(Coords pion, Direction d, CellColor color){
        return !(isColor(pion.row - d.getRow(), pion.col - d.getColumn(), color));
    }


    private boolean isValid(int rowOrCol)
    {
        return rowOrCol>=0 && rowOrCol <board.SIZE;
    }
































}