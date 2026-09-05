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

    /** One or more lanes, comma separated (e.g. "Top,Jungle"). */
    private String position;

    /** Lore species, following the official wiki taxonomy (Human, Yordle, Void...). */
    private String species;

    /**
     * Number of released skins, excluding the base skin.
     *
     * Unlike every other field this one goes stale on its own - Riot ships skins
     * continuously - so it is NOT stored in champions.json. It is filled in at
     * runtime by {@link com.tictactoegame.service.ChampionSkinDataService} from
     * Data Dragon and stays null until that first fetch succeeds. Everything that
     * reads it must cope with null (see RuleCatalog: the skin rule category is
     * simply left out of the game while the data is missing).
     */
    private volatile Integer skinCount;
}
