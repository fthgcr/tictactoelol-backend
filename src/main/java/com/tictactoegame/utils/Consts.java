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

    // All rule categories in one place; used by the tic-tac-toe rule generator
    // and the Connections puzzle generator.
    public static final List<String[]> RULE_CATEGORIES = List.of(
            regions,
            difficulties,
            roles,
            releaseDates,
            abilityResource,
            meleeRanged,
            gender);

}
