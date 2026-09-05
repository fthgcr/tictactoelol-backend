package com.tictactoegame.utils;

import com.tictactoegame.service.ChampionSkinDataService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The set of rule categories a puzzle may be built from, right now.
 *
 * Most categories are backed by champions.json and are simply always there. Skin counts
 * are not: they arrive from Data Dragon after startup and may never arrive at all if the
 * network is down. Handing the generators a category whose data is missing would produce
 * rules no champion satisfies - the tic-tac-toe generator would burn its 500 attempts and
 * throw, and Connections would do the same - so the category is only offered once the
 * data behind it exists.
 *
 * This is the list the generators must read; Consts.RULE_CATEGORIES is only the static
 * half of it.
 */
@Component
public class RuleCatalog {

    private final ChampionSkinDataService skinDataService;

    public RuleCatalog(ChampionSkinDataService skinDataService) {
        this.skinDataService = skinDataService;
    }

    /** A fresh, mutable list - callers shuffle it. */
    public List<String[]> categories() {
        List<String[]> categories = new ArrayList<>(Consts.RULE_CATEGORIES);
        if (skinDataService.isSkinDataAvailable()) {
            categories.add(Consts.skinCounts);
        }
        return categories;
    }

    /**
     * The categories that are the same on every instance at every moment.
     *
     * The daily puzzle is generated on demand from a date seed rather than stored, so
     * "everyone gets the same puzzle" only holds while the generator's inputs are
     * identical everywhere. categories() is not: it grows the moment the Data Dragon
     * fetch lands, which would hand a player who loaded the page during the first
     * seconds after a restart a different puzzle than everyone else. The daily puzzle
     * therefore draws from the static half only - the price is that skin rules appear
     * in unlimited mode and on tic-tac-toe boards, but not in the daily Connections.
     */
    public List<String[]> stableCategories() {
        return new ArrayList<>(Consts.RULE_CATEGORIES);
    }
}
