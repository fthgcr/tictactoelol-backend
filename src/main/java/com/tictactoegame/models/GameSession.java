package com.tictactoegame.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Live game state. Held in memory by
 * {@link com.tictactoegame.repositories.SessionRepository} - there is no database,
 * so sessions do not survive a restart. That is intentional: a match is short lived
 * and abandoned sessions are swept away by the TTL cleanup job.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GameSession {

    private int uid;

    private String firstPlayer;

    private String secondPlayer;

    private String gameId;

    private Integer turn;

    private Date date;

    private String playArea;

    private String[] playAreaArray;

    private String gameRule;

    private int gameStatus;

    private boolean isMatchmaking;

    // Who placed each of the 9 cells: "-1" = empty, "0" = first player, "1" = second player.
    // Kept server-side so win detection cannot be forged by a client.
    private String cellOwners;

    private String[] cellOwnersArray;

    /**
     * Epoch millis of the last write to this session. Only used by the cleanup job,
     * so it is kept out of the JSON sent to the client.
     */
    @JsonIgnore
    private long lastActivity;

    public String[] getCellOwners() {
        if (cellOwnersArray == null && cellOwners != null) {
            cellOwnersArray = cellOwners.split(",", -1);
        }
        return cellOwnersArray;
    }

    public void setCellOwners(String[] cellOwners) {
        this.cellOwnersArray = cellOwners;
        this.cellOwners = cellOwners == null ? null : String.join(",", cellOwners);
    }

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

    /** True once the match is over (win or draw). */
    @JsonIgnore
    public boolean isFinished() {
        return gameStatus != -1;
    }

}
