package com.tictactoegame.controllers;

import com.tictactoegame.models.responses.ConnectionsPuzzle;
import com.tictactoegame.service.ConnectionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CORS is handled centrally in CorsConfig.
@RestController
@RequestMapping("/connections")
public class ConnectionsController {

    private final ConnectionsService connectionsService;

    public ConnectionsController(ConnectionsService connectionsService) {
        this.connectionsService = connectionsService;
    }

    // Solo puzzles: the full solution is returned to the client on purpose;
    // there is no opponent to cheat against.

    // Unlimited mode: a fresh random puzzle on every call.
    @GetMapping("/newPuzzle")
    public ResponseEntity<ConnectionsPuzzle> newPuzzle() {
        return new ResponseEntity<>(connectionsService.generateRandomPuzzle(), HttpStatus.OK);
    }

    // Daily mode: identical puzzle for every player during a UTC day.
    @GetMapping("/dailyPuzzle")
    public ResponseEntity<ConnectionsPuzzle> dailyPuzzle() {
        return new ResponseEntity<>(connectionsService.generateDailyPuzzle(), HttpStatus.OK);
    }
}
