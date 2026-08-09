package com.tictactoegame.service;

import com.tictactoegame.models.Champions;
import com.tictactoegame.models.responses.GuessWhoChampion;
import com.tictactoegame.models.responses.GuessWhoPuzzle;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Generates "Guess Who" (LoLdle-style) puzzles: a secret champion the player
 * must find by guessing champion names; every guess reveals per-attribute
 * feedback (role, region, gender, resource, melee/ranged, release year...).
 *
 * Two modes, mirroring ConnectionsService:
 * - unlimited: a fresh random secret champion on every request
 * - daily: seeded with the UTC date, so every player gets the same champion all day
 */
@Service
public class GuessWhoService {

    private static final Random RANDOM = new Random();

    private final ChampionsService championsService;

    GuessWhoService(ChampionsService championsService) {
        this.championsService = championsService;
    }

    public GuessWhoPuzzle generateRandomPuzzle() {
        return generatePuzzle(RANDOM, null);
    }

    /**
     * Same secret champion for everyone during a given UTC day. Determinism comes
     * from a date-derived seed plus a stable (pid-sorted) champion ordering.
     * The seed is offset so the daily Guess Who champion does not correlate with
     * the daily Connections puzzle of the same date.
     */
    public GuessWhoPuzzle generateDailyPuzzle() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Random seededRandom = new Random(today.toEpochDay() * 31L + 7L);
        return generatePuzzle(seededRandom, today.toString());
    }

    private GuessWhoPuzzle generatePuzzle(Random random, String puzzleId) {
        // Stable ordering is required for the daily puzzle to be deterministic;
        // the DB does not guarantee row order.
        List<Champions> allChampions = new ArrayList<>(championsService.getAllChampions());
        if (allChampions.isEmpty()) {
            throw new IllegalStateException("No champions available for a Guess Who puzzle");
        }
        allChampions.sort(Comparator.comparingInt(Champions::getPid));

        Champions answer = allChampions.get(random.nextInt(allChampions.size()));

        List<GuessWhoChampion> pool = new ArrayList<>(allChampions.size());
        for (Champions champion : allChampions) {
            pool.add(toDto(champion));
        }
        return new GuessWhoPuzzle(puzzleId, answer.getName(), pool);
    }

    private GuessWhoChampion toDto(Champions champion) {
        return new GuessWhoChampion(
                champion.getName(),
                champion.getRole(),
                champion.getDifficulty(),
                champion.getRegion(),
                champion.getReleaseDate(),
                champion.getAbilityResource(),
                champion.getMeleeRanged(),
                champion.getGender());
    }
}
