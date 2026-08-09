package com.tictactoegame.service;

import com.tictactoegame.models.Champions;
import com.tictactoegame.models.responses.ConnectionsGroup;
import com.tictactoegame.models.responses.ConnectionsPuzzle;
import com.tictactoegame.utils.Consts;
import com.tictactoegame.utils.Utils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Generates LoL "Connections" puzzles (NYT Connections style):
 * 16 champions that must be split into 4 groups of 4 by a hidden shared attribute.
 *
 * Uniqueness guarantee: a champion is only eligible for a group when it matches that
 * group's rule and does NOT match any of the other three rules. The four candidate
 * pools are therefore disjoint and the intended partition is the only valid solution.
 *
 * Two modes:
 * - unlimited: a fresh random puzzle on every request
 * - daily: seeded with the UTC date, so every player gets the same puzzle all day
 */
@Service
public class ConnectionsService {

    private static final int GROUP_COUNT = 4;
    private static final int GROUP_SIZE = 4;
    private static final int MAX_GENERATION_ATTEMPTS = 1000;

    private static final Random RANDOM = new Random();

    private final ChampionsService championsService;

    ConnectionsService(ChampionsService championsService) {
        this.championsService = championsService;
    }

    public ConnectionsPuzzle generateRandomPuzzle() {
        return generatePuzzle(RANDOM, null);
    }

    /**
     * Same puzzle for everyone during a given UTC day. Determinism comes from a
     * date-derived seed plus a stable (pid-sorted) champion ordering, so no state
     * or cache is needed and every instance produces the identical puzzle.
     */
    public ConnectionsPuzzle generateDailyPuzzle() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Random seededRandom = new Random(today.toEpochDay());
        return generatePuzzle(seededRandom, today.toString());
    }

    private ConnectionsPuzzle generatePuzzle(Random random, String puzzleId) {
        // Stable ordering is required for the daily puzzle to be deterministic;
        // the DB does not guarantee row order.
        List<Champions> allChampions = new ArrayList<>(championsService.getAllChampions());
        allChampions.sort(Comparator.comparingInt(Champions::getPid));

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            ConnectionsPuzzle puzzle = tryGeneratePuzzle(allChampions, random, puzzleId);
            if (puzzle != null) {
                return puzzle;
            }
        }
        throw new IllegalStateException(
                "Could not generate a Connections puzzle after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    private ConnectionsPuzzle tryGeneratePuzzle(List<Champions> allChampions, Random random, String puzzleId) {
        List<String> rules = pickRules(random);

        List<ConnectionsGroup> groups = new ArrayList<>(GROUP_COUNT);
        for (int groupIndex = 0; groupIndex < GROUP_COUNT; groupIndex++) {
            List<String> candidates = exclusiveCandidates(allChampions, rules, groupIndex);
            if (candidates.size() < GROUP_SIZE) {
                return null; // not enough exclusive champions for this rule combination
            }
            Collections.shuffle(candidates, random);
            groups.add(new ConnectionsGroup(rules.get(groupIndex), candidates.subList(0, GROUP_SIZE)));
        }
        return new ConnectionsPuzzle(puzzleId, groups);
    }

    // One random rule from each of 4 distinct categories.
    private List<String> pickRules(Random random) {
        List<String[]> categories = new ArrayList<>(Consts.RULE_CATEGORIES);
        Collections.shuffle(categories, random);
        List<String> rules = new ArrayList<>(GROUP_COUNT);
        for (int index = 0; index < GROUP_COUNT; index++) {
            String[] category = categories.get(index);
            rules.add(category[random.nextInt(category.length)]);
        }
        return rules;
    }

    // Champions matching ONLY the rule at ruleIndex (and none of the other three).
    private List<String> exclusiveCandidates(List<Champions> allChampions, List<String> rules, int ruleIndex) {
        List<String> candidates = new ArrayList<>();
        for (Champions champion : allChampions) {
            if (!Utils.isRuleCorrect(rules.get(ruleIndex), champion)) {
                continue;
            }
            boolean matchesAnother = false;
            for (int otherIndex = 0; otherIndex < rules.size(); otherIndex++) {
                if (otherIndex != ruleIndex && Utils.isRuleCorrect(rules.get(otherIndex), champion)) {
                    matchesAnother = true;
                    break;
                }
            }
            if (!matchesAnother) {
                candidates.add(champion.getName());
            }
        }
        return candidates;
    }
}
