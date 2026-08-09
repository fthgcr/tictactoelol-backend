package com.tictactoegame.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A full Connections puzzle: 4 groups x 4 champions = 16 unique champions.
 * By construction every champion matches exactly one of the four group rules,
 * so the puzzle always has a unique solution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionsPuzzle {

    // "2026-07-18" for the daily puzzle, null for unlimited/random puzzles.
    // The client uses it to persist "already played today".
    private String puzzleId;

    private List<ConnectionsGroup> groups;
}
