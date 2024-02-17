package com.tictactoegame.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Entity
@Table(name="game_session")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int uid;

    @Column(name = "first_player")
    private String firstPlayer;

    @Column(name = "second_player")
    private String secondPlayer;

    @Column(name = "game_id")
    private String gameId;

    @Column(name = "turn")
    private Integer turn;

    @Column(name = "start_date")
    private Date date;

    @Column(name = "play_area")
    private String playArea;

    @Transient
    private String[] playAreaArray;

    @Column(name = "game_rule")
    private String gameRule;

    @Column(name = "game_status")
    private int gameStatus;

    public String[] getPlayArea() {
        if (playAreaArray == null && playArea != null) {
            playAreaArray = playArea.split(",", -1);
        }
        return playAreaArray;
    }

    public void setPlayArea(String[] playArea) {
        this.playAreaArray = playArea;
        this.playArea = String.join(",", playArea);
    }

    public void setPlayAreaAsString(String playAreaAsString) {
        this.playArea = playAreaAsString;
        // Update playArea array when playAreaAsString is set
        if (playAreaAsString != null) {
            this.playAreaArray = playAreaAsString.split(",", -1);
        }
    }

    public String[] getPlayAreaAsArray() {
        if (playArea != null) {
            return playArea.split(",", -1);
        }
        return null;
    }

}
