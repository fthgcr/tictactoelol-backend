package com.tictactoegame.models.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameAreaRequest {

    private int uid;

    // Identity of the sender (persistent player id / matchmaking username).
    // Used server-side to validate that the move belongs to the player whose turn it is.
    private String playerId;

    private String firstPlayer;

    private String secondPlayer;

    private String gameId;

    private Integer turn;

    private Date date;

    private String playArea;

    private Integer index;

    private String value;

    private String horizontalRule;

    private String verticalRule;

    private int gameStatus;

}
