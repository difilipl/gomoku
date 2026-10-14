package fr.esisar.in450.gomoku.player;

import fr.esisar.in450.gomoku.gamecore.AbstractPlayer;
import fr.esisar.in450.gomoku.gamecore.enums.CellColor;
import fr.esisar.in450.gomoku.gamecore.model.Coords;

public class MinMaxSimplePlayer extends AbstractPlayer {
    private final int profondeur = 1; 

    @Override
    public Coords play()
    {
        int x,y ;

        ///la on est sur une complexité de 1 pour celui la, donc pas vraiment de fonction minmax, mais on va tout betement 
        // calculer la valeur d'un board a chaque coup possible, 
        for (int i = 0; i < board.SIZE; i++){
            x = i / board.SIZE;
            y = i % board.SIZE;
             
            if(isColor(CellColor.WHITE, x, y)){

            }
        }
    return new Coords();

    }

    private boolean isColor(CellColor color, int x, int y){
        if(x >= board.SIZE || x < 0 || y >= board.SIZE || y < 0) return false;
        return color == board.getCellColor(x,y);
    }

    private boolean isSequenceStart(CellColor color, int x, int y,String direction){
        switch direction1 {
            case "SW":
                break;
        }
    }



































}