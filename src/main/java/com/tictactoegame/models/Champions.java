package com.tictactoegame.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Static reference data - loaded once from resources/data/champions.json
 * by {@link com.tictactoegame.repositories.ChampionsRepository}.
 * Champions never change at runtime, so no database is involved.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Champions {

    private int pid;

    private String name;

    /** One or more roles, comma separated (e.g. "Fighter,Tank"). */
    private String role;

    private String difficulty;

    private String region;

    private String releaseDate;

    private String abilityResource;

    /** "Melee", "Ranged" or "Melee,Ranged". */
    private String meleeRanged;

    private String gender;
}
