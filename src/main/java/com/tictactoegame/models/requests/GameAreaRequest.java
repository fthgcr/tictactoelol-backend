package com.tictactoegame.models.requests;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameAreaRequest {

    private int uid;

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

}
