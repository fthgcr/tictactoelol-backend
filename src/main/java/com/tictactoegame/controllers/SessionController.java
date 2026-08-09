package com.tictactoegame.controllers;

import com.tictactoegame.models.GameSession;
import com.tictactoegame.models.requests.GameAreaRequest;
import com.tictactoegame.models.requests.SessionRequest;
import com.tictactoegame.service.SessionService;
import com.tictactoegame.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

// CORS is handled centrally in CorsConfig, so no per-endpoint @CrossOrigin is needed.
@RestController
@RequestMapping("/session")
public class SessionController {

    // Control signals sent over the "index" field of a websocket GameAreaRequest.
    // A value >= 0 is a real board index; these negative values are commands.
    // Note: the old -2 "echo client state" signal was removed on purpose - it let any
    // client broadcast a forged game state (e.g. a fake win). All state now comes from the server.
    private static final int SIGNAL_HEALTH_CHECK = -1; // re-sync current server state
    private static final int SIGNAL_SKIP_TURN = -3;    // current player's timer ran out
    private static final int SIGNAL_REPLAY = -4;       // rematch on the same session

    private final SessionService sessionService;

    @Autowired
    public SessionController(SessionService sessionService){
        this.sessionService = sessionService;
    }

    @GetMapping ("/generateGameId")
    public ResponseEntity<String> generateGameId() {
        return new ResponseEntity<>(Utils.generateCreatedGameId(), HttpStatus.OK);
    }

    @PostMapping ("/createOrJoinGame")
    public ResponseEntity<GameSession> createOrJoinGame(@RequestBody SessionRequest sessionRequest) {
        return new ResponseEntity<>(sessionService.createOrJoinGame(sessionRequest), HttpStatus.OK);
    }

    @PostMapping ("/healthCheckSession")
    public ResponseEntity<GameSession> healthCheckSession(@RequestBody SessionRequest sessionRequest) {
        return new ResponseEntity<>(sessionService.healthCheckSession(sessionRequest.getGameId()), HttpStatus.OK);
    }

    @MessageMapping("/chat/{gameId}")
    @SendTo("/topic/{gameId}")
    public GameSession sendMessageToGroup(@DestinationVariable String gameId, GameAreaRequest gameAreaRequest) {
        Integer index = gameAreaRequest.getIndex();
        if (index != null && index == SIGNAL_HEALTH_CHECK) {
            return sessionService.healthCheckSession(gameAreaRequest.getGameId());
        } else if (index != null && index == SIGNAL_SKIP_TURN) {
            return sessionService.skipTurn(gameAreaRequest.getGameId(), gameAreaRequest.getPlayerId());
        } else if (index != null && index == SIGNAL_REPLAY) {
            // Broadcast so both players enter the rematch together, not just the one who asked.
            // The service checks the sender is a player, so spectators cannot reset the board.
            return sessionService.replayGame(gameAreaRequest.getGameId(), gameAreaRequest.getPlayerId());
        } else {
            return sessionService.setPlayArea(gameAreaRequest);
        }
    }

    // The old GET /replaySession/{gameId} is gone: it took no identity, so anyone who
    // knew a gameId could reset someone else's board. Rematches go through the
    // websocket replay signal, which validates the sender.

    @GetMapping ("/findMatch/{username}")
    public ResponseEntity<GameSession> findMatch(@PathVariable String username) {
        return new ResponseEntity<>(sessionService.findEmptySession(username), HttpStatus.OK);
    }

    @DeleteMapping ("/quitSession/{id}")
    public ResponseEntity<Boolean> quitSession(@PathVariable Integer id,
                                               @RequestParam String playerId) throws IllegalAccessException {
        try {
            sessionService.quitSession(id, playerId);
            return new ResponseEntity<>(true, HttpStatus.OK);
        } catch (Exception e){
            return new ResponseEntity<>(false, HttpStatus.OK);
        }
    }

}
