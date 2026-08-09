package com.tictactoegame.controllers;

import com.tictactoegame.models.responses.GuessWhoPuzzle;
import com.tictactoegame.service.GuessWhoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CORS is handled centrally in CorsConfig.
@RestController
@RequestMapping("/guesswho")
public class GuessWhoController {

    private final GuessWhoService guessWhoService;

    public GuessWhoController(GuessWhoService guessWhoService) {
        this.guessWhoService = guessWhoService;
    }

    // Solo puzzles: the answer is returned to the client on purpose;
    // there is no opponent to cheat against.

    // Unlimited mode: a fresh random secret champion on every call.
    @GetMapping("/newPuzzle")
    public ResponseEntity<GuessWhoPuzzle> newPuzzle() {
        return new ResponseEntity<>(guessWhoService.generateRandomPuzzle(), HttpStatus.OK);
    }

    // Daily mode: identical secret champion for every player during a UTC day.
    @GetMapping("/dailyPuzzle")
    public ResponseEntity<GuessWhoPuzzle> dailyPuzzle() {
        return new ResponseEntity<>(guessWhoService.generateDailyPuzzle(), HttpStatus.OK);
    }
}
