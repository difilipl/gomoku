package fr.esisar.in450.gomoku.player;

import fr.esisar.in450.gomoku.gamecore.AbstractPlayer;
import fr.esisar.in450.gomoku.gamecore.enums.CellColor;
import fr.esisar.in450.gomoku.gamecore.enums.Direction;
import fr.esisar.in450.gomoku.gamecore.model.Coords;

import java.awt.*;
import java.util.ArrayList;

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


        for (int i = 0; i < board.SIZE; i++){
            row= i / board.SIZE;
            column = i % board.SIZE;
            if( )
        }
    return new Coords();

    }

    private player.Sequence lenthcut(int row, int column, Direction d, CellColor color){
        int lenth = 0;
        int cut = 0;
        int r = row;
        int c = column;
        ArrayList<Integer> res = new ArrayList<>();
        while(isColor(r, c,color)) {
            lenth++;
            r += d.getRow();
            c += d.getColumn();

        }
        if(!(isColor(r,c,null))) cut += 1;
        if(!(isColor(row - d.getRow(), column - d.getColumn(), null))) cut += 1;
        return new player.Sequence(lenth,cut);
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