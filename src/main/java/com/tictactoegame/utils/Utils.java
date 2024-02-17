package com.tictactoegame.utils;

import com.tictactoegame.models.GameSession;
import com.tictactoegame.models.requests.GameAreaRequest;

public class Utils {

    public static final String[] fillEmptyGameArea(){
        String[] empty = {"0", "0", "0", "0", "0", "0", "0", "0", "0"};
        return empty;
    }

    public static final String[] fillEmptyGameRules(){
        return new String[]{"0", "0", "0", "0", "0", "0"};
    }

    public static final GameSession gameAreaRequestToGameSession(GameAreaRequest gameAreaRequest){
        return GameSession.builder()
                .gameId(gameAreaRequest.getGameId())
                .playArea(gameAreaRequest.getPlayArea())
                .firstPlayer(gameAreaRequest.getFirstPlayer())
                .uid(gameAreaRequest.getUid())
                .date(gameAreaRequest.getDate())
                .turn(gameAreaRequest.getTurn())
                .secondPlayer(gameAreaRequest.getSecondPlayer())
                .gameStatus(gameAreaRequest.getGameStatus())
                .build();
    }

    public static final String[] splitString(String value){
        if(value.contains(",")){
            return value.split(",");
        } else {
            return new String[]{value};
        }
    }

    public static final String getPureRuleString(String rule){
        int indexOfColon = rule.indexOf(" : ");
        if (indexOfColon != -1 && indexOfColon + 3 < rule.length()) {
            return rule.substring(indexOfColon + 3);
        } else {
            return "Error";
        }


    }
}
