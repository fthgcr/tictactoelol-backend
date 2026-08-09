package com.tictactoegame.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One of the four groups of a Connections puzzle:
 * the shared rule (e.g. "Region : Ionia") and the 4 champion names that satisfy it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionsGroup {

    private String rule;

    private List<String> champions;
}
