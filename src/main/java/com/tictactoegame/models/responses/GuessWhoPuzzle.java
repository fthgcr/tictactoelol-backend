package com.tictactoegame.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A Guess Who (LoLdle-style) puzzle: one secret champion plus the full champion
 * pool with attributes. The client computes per-attribute feedback for each guess.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuessWhoPuzzle {

    // "2026-07-18" for the daily puzzle, null for unlimited/random puzzles.
    // The client uses it to persist "already played today".
    private String puzzleId;

    // Name of the secret champion (solo puzzle: returned on purpose).
    private String answer;

    private List<GuessWhoChampion> champions;
}
