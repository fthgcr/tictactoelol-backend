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

    /**
     * Epoch millis the current turn began. The turn watchdog measures against this to
     * notice a player who walked away: their client is gone, so nobody sends the skip
     * signal that normally ends a turn.
     */
    @JsonIgnore
    private long turnStartedAt;

    /**
     * How many turns in a row the watchdog had to end for each player because they
     * never answered. Counted per player, not per session: an opponent who is still
     * playing normally must not keep resetting the absent player's tally.
     */
    @JsonIgnore
    private int firstPlayerMissedTurns;

    @JsonIgnore
    private int secondPlayerMissedTurns;

    /**
     * Why the game ended, when it was not ended by play. Null for a normal win or draw;
     * {@link #END_REASON_OPPONENT_LEFT} when the watchdog awarded the win because a
     * player stopped responding. The client uses it to word the result honestly.
     */
    private String endReason;

    public static final String END_REASON_OPPONENT_LEFT = "OPPONENT_LEFT";

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

    /**
     * Hands the turn to a player and restarts that turn's clock. Every turn change goes
     * through here so the watchdog can never read a stale deadline.
     */
    public void startTurn(int player) {
        this.turn = player;
        this.turnStartedAt = System.currentTimeMillis();
    }

    @JsonIgnore
    public int getMissedTurns(int player) {
        return player == 0 ? firstPlayerMissedTurns : secondPlayerMissedTurns;
    }

    /** The watchdog had to end this player's turn for them. */
    public void recordMissedTurn(int player) {
        if (player == 0) {
            firstPlayerMissedTurns++;
        } else {
            secondPlayerMissedTurns++;
        }
    }

    /** Anything this player did proves they are still here, so the tally starts over. */
    public void clearMissedTurns(int player) {
        if (player == 0) {
            firstPlayerMissedTurns = 0;
        } else {
            secondPlayerMissedTurns = 0;
        }
    }

}
