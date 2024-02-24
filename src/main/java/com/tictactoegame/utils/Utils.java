package com.tictactoegame.utils;

import com.tictactoegame.models.GameSession;
import com.tictactoegame.models.requests.GameAreaRequest;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class Utils {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Random random = new SecureRandom();
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

    public static final String generateGameId(){
        StringBuilder result = new StringBuilder();
        result.append("FI");
        for(int index =0; index < 6; index++){
            int randomIndex = random.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(randomIndex));
        }
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMHHmmss");
        result.append(now.format(formatter));

        return result.toString();
    }

}
