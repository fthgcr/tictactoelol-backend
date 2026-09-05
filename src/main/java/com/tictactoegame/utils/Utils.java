package com.tictactoegame.utils;

import com.tictactoegame.models.Champions;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class Utils {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Random random = new SecureRandom();
    public static final String[] fillEmptyGameArea(){
        String[] empty = {"0", "0", "0", "0", "0", "0", "0", "0", "0"};
        return empty;
    }

    public static final String[] fillEmptyGameRules(){
        return new String[]{"0", "0", "0", "0", "0", "0"};
    }

    public static final String[] fillEmptyCellOwners(){
        return new String[]{"-1", "-1", "-1", "-1", "-1", "-1", "-1", "-1", "-1"};
    }

    private static final int[][] WINNING_COMBINATIONS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // horizontal
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // vertical
            {0, 4, 8}, {2, 4, 6}             // diagonal
    };

    /**
     * Returns the winning player (0 or 1) based on cell ownership, or -1 if nobody has won.
     */
    public static int findWinner(String[] cellOwners){
        if (cellOwners == null || cellOwners.length < 9) {
            return -1;
        }
        for (int[] combination : WINNING_COMBINATIONS) {
            String first = cellOwners[combination[0]];
            if (!"-1".equals(first)) {
                if (first.equals(cellOwners[combination[1]]) && first.equals(cellOwners[combination[2]])) {
                    return Integer.parseInt(first);
                }
            }
        }
        return -1;
    }

    public static boolean isBoardFull(String[] playArea){
        for (String cell : playArea) {
            if ("0".equals(cell)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Rules are stored as 6 comma separated entries: [0..2] = column rules, [3..5] = row rules.
     * These mirror getVerticalRule/getHorizontalRule on the frontend, but are derived
     * server-side from the persisted rule set so clients cannot substitute easier rules.
     */
    public static String deriveVerticalRule(String[] rules, int index){
        return rules[index % 3];
    }

    public static String deriveHorizontalRule(String[] rules, int index){
        return rules[3 + (index / 3)];
    }

    public static final String[] splitString(String value){
        if(value.contains(",")){
            return value.split(",");
        } else {
            return new String[]{value};
        }
    }

    /**
     * True when the given champion satisfies a rule like "Region : Ionia".
     * Shared by the tic-tac-toe move validation and the Connections puzzle generator.
     */
    public static boolean isRuleCorrect(String rule, Champions champions){
        if (champions == null || rule == null || !rule.contains(" : ")) {
            return false;
        }
        String ruleName = getPureRuleString(rule);
        boolean isItCorrect = false;
        switch (rule.substring(0, rule.indexOf(" : ")).trim()) {
            case "Region":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getRegion());
                break;
            case "Difficulty":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getDifficulty());
                break;
            case "Role":
                String[] roles = splitString(champions.getRole());
                for (String role : roles) {
                    if (role.equalsIgnoreCase(ruleName)) {
                        isItCorrect = true;
                        break;
                    }
                }
                break;
            case "Release Date":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getReleaseDate());
                break;
            case "Ability Resource":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getAbilityResource());
                break;
            case "Melee/Ranged":
                String[] meleeRanged = splitString(champions.getMeleeRanged());
                for (String mr : meleeRanged) {
                    if (mr.equalsIgnoreCase(ruleName)) {
                        isItCorrect = true;
                        break;
                    }
                }
                break;
            case "Gender":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getGender());
                break;
            case "Position":
                String[] positions = splitString(champions.getPosition());
                for (String position : positions) {
                    if (position.equalsIgnoreCase(ruleName)) {
                        isItCorrect = true;
                        break;
                    }
                }
                break;
            case "Species":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getSpecies());
                break;
            case "Skin Count":
                isItCorrect = isInSkinBucket(ruleName, champions.getSkinCount());
                break;
        }

        return isItCorrect;
    }

    /**
     * Matches a skin count against a bucket label ("5-9", "15+").
     *
     * A null count means the Data Dragon fetch has not landed yet. Answering "no" for
     * every champion is the safe direction: RuleCatalog keeps the whole category out of
     * play until the data is there, so an unmatched skin rule should never reach a board
     * in the first place - and if one somehow did, it would be caught by the solvability
     * check rather than silently accepting wrong answers.
     */
    public static boolean isInSkinBucket(String bucket, Integer skinCount) {
        if (bucket == null || skinCount == null) {
            return false;
        }
        try {
            if (bucket.endsWith("+")) {
                return skinCount >= Integer.parseInt(bucket.substring(0, bucket.length() - 1).trim());
            }
            int dashIndex = bucket.indexOf('-');
            if (dashIndex < 1) {
                return false;
            }
            int min = Integer.parseInt(bucket.substring(0, dashIndex).trim());
            int max = Integer.parseInt(bucket.substring(dashIndex + 1).trim());
            return skinCount >= min && skinCount <= max;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static final String getPureRuleString(String rule){
        int indexOfColon = rule.indexOf(" : ");
        if (indexOfColon != -1 && indexOfColon + 3 < rule.length()) {
            return rule.substring(indexOfColon + 3);
        } else {
            return "Error";
        }
    }

    public static final String generateGameId(){
        return generateGameId("FI");
    }

    // Created (non-matchmaking) games use the "CG" prefix so they are never
    // misdetected as matchmaking sessions (which use the "FI" prefix).
    public static final String generateCreatedGameId(){
        return generateGameId("CG");
    }

    private static String generateGameId(String prefix){
        StringBuilder result = new StringBuilder();
        result.append(prefix);
        for(int index =0; index < 6; index++){
            int randomIndex = random.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(randomIndex));
        }
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMHHmmss");
        result.append(now.format(formatter));

        return result.toString();
    }

}
