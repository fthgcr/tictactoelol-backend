package com.tictactoegame.controllers;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.tictactoegame.models.Champions;
import com.tictactoegame.models.GameSession;
import com.tictactoegame.models.requests.GameAreaRequest;
import com.tictactoegame.models.requests.SessionRequest;
import com.tictactoegame.repositories.ChampionsRepository;
import com.tictactoegame.repositories.SessionRepository;
import com.tictactoegame.service.ChampionsService;
import com.tictactoegame.service.SessionService;
import com.tictactoegame.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/session")
public class SessionController {

    private final SessionService sessionService;
    private final ChampionsService championsService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public SessionController(SessionService sessionService, ChampionsService championsService, SimpMessagingTemplate simpMessagingTemplate){
        this.sessionService = sessionService;
        this.championsService = championsService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://tictactoelol.onrender.com/"})
    @PostMapping ("/createOrJoinGame")
    public ResponseEntity<GameSession> createOrJoinGame(@RequestBody SessionRequest sessionRequest) throws IllegalAccessException {
        return new ResponseEntity<>(sessionService.createOrJoinGame(sessionRequest), HttpStatus.OK);
    }

    @MessageMapping("/chat/{gameId}")
    @SendTo("/topic/{gameId}")
    public GameSession sendMessageToGroup(@DestinationVariable String gameId, GameAreaRequest gameAreaRequest) {
        if(gameAreaRequest.getIndex() != null && gameAreaRequest.getIndex() == -1){
            return sessionService.healthCheckSession(gameAreaRequest.getGameId());
        } else if(gameAreaRequest.getIndex() != null && gameAreaRequest.getIndex() == -2){
            return Utils.gameAreaRequestToGameSession(gameAreaRequest);
        } else {
            return sessionService.setPlayArea(gameAreaRequest);
        }
    }

    /*@CrossOrigin(origins = "http://localhost:4200")
    @PostMapping ("/setPlayArea")
    public ResponseEntity<GameSession> setPlayArea(@RequestBody GameAreaRequest gameAreaRequest){

        String gameId = gameAreaRequest.getGameId();
        String dummyObject = "Dummy message";
        simpMessagingTemplate.convertAndSend("/topic/" + gameId, dummyObject);
        return new ResponseEntity<>(null, HttpStatus.OK);
        //return new ResponseEntity<>(sessionService.setPlayArea(gameAreaRequest), HttpStatus.OK);
    } */

    //Champions
    @CrossOrigin(origins = {"http://localhost:4200", "https://tictactoelol.onrender.com/"})
    @GetMapping ("/getAll")
    public ResponseEntity<List<Champions>> getAll(){
        return new ResponseEntity<>(championsService.getAllChampions(), HttpStatus.OK);
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://tictactoelol.onrender.com/"})
    @GetMapping ("/getChampionByName")
    public ResponseEntity<Champions> getChampionByName(){
        return new ResponseEntity<>(championsService.getChampionByName("Quinn").get(), HttpStatus.OK);
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://tictactoelol.onrender.com/"})
    @GetMapping ("/getRules")
    public ResponseEntity<Object> getRules() throws IllegalAccessException {
        return new ResponseEntity<>(sessionService.checkRules("Ability Resource : Health,Difficulty : 1,Gender : Female,Region : The Freljord,Role : Marksman,Release Date : 2022"), HttpStatus.OK);
    }




}
