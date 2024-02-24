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

    @Autowired
    public SessionController(SessionService sessionService){
        this.sessionService = sessionService;
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://tictactoelol.onrender.com/"})
    @PostMapping ("/createOrJoinGame")
    public ResponseEntity<GameSession> createOrJoinGame(@RequestBody SessionRequest sessionRequest) throws IllegalAccessException {
        return new ResponseEntity<>(sessionService.createOrJoinGame(sessionRequest), HttpStatus.OK);
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://tictactoelol.onrender.com/"})
    @PostMapping ("/healthCheckSession")
    public ResponseEntity<GameSession> healthCheckSession(@RequestBody SessionRequest sessionRequest) throws IllegalAccessException {
        return new ResponseEntity<>(sessionService.healthCheckSession(sessionRequest.getGameId()), HttpStatus.OK);
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

    @CrossOrigin(origins = {"http://localhost:4200", "https://tictactoelol.onrender.com/"})
    @GetMapping ("/replaySession/{gameId}")
    public ResponseEntity<Boolean> replaySession(@PathVariable String gameId)  {
        return new ResponseEntity<>(sessionService.replaySession(gameId), HttpStatus.OK);
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://tictactoelol.onrender.com/"})
    @GetMapping ("/findMatch/{username}")
    public ResponseEntity<GameSession> findMatch(@PathVariable String username) throws IllegalAccessException {
        return new ResponseEntity<>(sessionService.findEmptySession(username), HttpStatus.OK);
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://tictactoelol.onrender.com/"})
    @DeleteMapping ("/quitSession/{id}")
    public ResponseEntity<Boolean> quitSession(@PathVariable Integer id) throws IllegalAccessException {
        try {
            sessionService.quitSession(id);
            return new ResponseEntity<>(true, HttpStatus.OK);
        } catch (Exception e){
            return new ResponseEntity<>(false, HttpStatus.OK);
        }
    }

}
