package com.tictactoegame.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Champion attributes exposed to the Guess Who client so it can render the
 * comparison feedback locally (solo mode, nothing to cheat against).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuessWhoChampion {

    private String name;
    private String role;          // may be multi-valued, comma separated
    private String difficulty;
    private String region;
    private String releaseDate;   // release year, e.g. "2013"
    private String abilityResource;
    private String meleeRanged;   // may be multi-valued, comma separated
    private String gender;
    private String position;      // lane, may be multi-valued, comma separated
    private String species;       // lore species, single valued
    /** Skins excluding the base one; null while the Data Dragon fetch has not landed. */
    private Integer skinCount;
}
