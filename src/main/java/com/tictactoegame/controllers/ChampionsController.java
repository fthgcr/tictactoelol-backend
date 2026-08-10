package com.tictactoegame.controllers;

import com.tictactoegame.models.Champions;
import com.tictactoegame.service.ChampionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes which champions the server actually knows about.
 *
 * The client builds its champion picker from Riot's Data Dragon, which always
 * carries the full live roster. That roster and this game's data set drift apart
 * the moment Riot ships a champion we have not added: the picker would offer a
 * champion whose move the server can never accept, silently costing the player
 * their turn. Intersecting the picker with this list keeps the two in step.
 */
@RestController
@RequestMapping("/champions")
public class ChampionsController {

    private final ChampionsService championsService;

    public ChampionsController(ChampionsService championsService) {
        this.championsService = championsService;
    }

    /** Names only - the client already has portraits and traits from Data Dragon. */
    @GetMapping("/names")
    public ResponseEntity<List<String>> names() {
        List<String> names = championsService.getAllChampions().stream()
                .map(Champions::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new ResponseEntity<>(names, HttpStatus.OK);
    }
}
