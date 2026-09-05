package com.tictactoegame.utils;

import java.util.List;

public class Consts {

    public static final String[] regions = {
            "Region : The Void",
            "Region : Bilgewater",
            "Region : Demacia",
            "Region : Ionia",
            "Region : The Freljord",
            "Region : Runeterra",
            "Region : Bandle City",
            "Region : Shurima",
            "Region : Zaun",
            "Region : Targon",
            "Region : Noxus",
            "Region : Piltover",
            "Region : Ixtal",
            "Region : Shadow Isles" };

    public static final String[] difficulties = {
            "Difficulty : 1",
            "Difficulty : 2",
            "Difficulty : 3"};

    public static final String[] roles = {
            "Role : Tank",
            "Role : Support",
            "Role : Fighter",
            "Role : Mages",
            "Role : Assassin",
            "Role : Marksman"};

    public static final String[] releaseDates = {
            "Release Date : 2009",
            "Release Date : 2010",
            "Release Date : 2011",
            "Release Date : 2012",
            "Release Date : 2013",
            "Release Date : 2014",
            "Release Date : 2015",
            "Release Date : 2016",
            "Release Date : 2017",
            "Release Date : 2018",
            "Release Date : 2019",
            "Release Date : 2020",
            "Release Date : 2021",
            "Release Date : 2022",
            "Release Date : 2023",
            // 2024 and 2025 hold three champions each, so - like 2018 - they can never
            // fill a four-champion Connections group. The generator just retries past
            // them; they still work fine as tic-tac-toe rules.
            "Release Date : 2024",
            "Release Date : 2025"};

    public static final String[] abilityResource = {
            "Ability Resource : None",
            "Ability Resource : Health",
            "Ability Resource : Mana",
            "Ability Resource : Energy"};

    public static final String[] meleeRanged = {
            "Melee/Ranged : Melee",
            "Melee/Ranged : Ranged"};

    public static final String[] gender = {
            "Gender : Male",
            "Gender : Female"};

    /** Lane. Multi-valued per champion, so these overlap on purpose. */
    public static final String[] positions = {
            "Position : Top",
            "Position : Jungle",
            "Position : Middle",
            "Position : Bottom",
            "Position : Support"};

    /**
     * Lore species, taken from the official wiki's species categories. Darkin wins over
     * Ascended (every Darkin is a fallen Ascended) and Yordle over Spirit, so each
     * champion carries exactly one value. The smallest groups (Darkin, Ascended) hold
     * four champions, which is the minimum a Connections group needs.
     */
    public static final String[] species = {
            "Species : Human",
            "Species : Yordle",
            "Species : Vastaya",
            "Species : Void",
            "Species : Darkin",
            "Species : Ascended",
            "Species : Celestial",
            "Species : Undead",
            "Species : Demon",
            "Species : Construct",
            "Species : Spirit",
            "Species : Beast"};

    /**
     * Skin count buckets, matched against Champions#skinCount.
     *
     * Buckets rather than exact numbers: "Skin Count : 17" would fit a single champion,
     * which is useless as a rule and impossible as a Connections group.
     *
     * This category is only in play when the skin data has been fetched - see RuleCatalog.
     */
    public static final String[] skinCounts = {
            "Skin Count : 0-4",
            "Skin Count : 5-9",
            "Skin Count : 10-14",
            "Skin Count : 15+"};

    /**
     * Rule categories backed by champions.json alone; these are always available.
     * Categories that depend on data fetched at runtime are added by
     * {@link com.tictactoegame.utils.RuleCatalog}, which is what the game
     * generators actually read - do not use this list directly.
     */
    public static final List<String[]> RULE_CATEGORIES = List.of(
            regions,
            difficulties,
            roles,
            releaseDates,
            abilityResource,
            meleeRanged,
            gender,
            positions,
            species);

}
