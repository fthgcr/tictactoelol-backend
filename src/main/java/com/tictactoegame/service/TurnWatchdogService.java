package com.tictactoegame.service;

import com.tictactoegame.models.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Watches for players who walked away mid-match.
 *
 * The turn countdown runs in the browser of whoever is on the clock, so a player who
 * closes the tab takes the countdown with them: no skip signal is ever sent and the
 * opponent sits on "waiting" indefinitely. This job is the server-side clock that keeps
 * running regardless of who is still connected.
 *
 * Anything it changes is pushed straight back down the same /topic/{gameId} the players
 * already listen on, so the remaining client updates without polling for it.
 */
@Service
public class TurnWatchdogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TurnWatchdogService.class);

    private final SessionService sessionService;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * How long a turn may go unanswered. Must stay comfortably above the client's own
     * 30 second countdown, otherwise the server would end turns the player was still
     * about to play through the normal path.
     */
    private final Duration turnTimeout;

    /** Unanswered turns by the same player before the opponent is handed the win. */
    private final int maxMissedTurns;

    public TurnWatchdogService(SessionService sessionService,
                               SimpMessagingTemplate messagingTemplate,
                               @Value("${game.turn.timeout:PT38S}") Duration turnTimeout,
                               @Value("${game.turn.max-missed:2}") int maxMissedTurns) {
        this.sessionService = sessionService;
        this.messagingTemplate = messagingTemplate;
        this.turnTimeout = turnTimeout;
        this.maxMissedTurns = maxMissedTurns;
    }

    @Scheduled(fixedDelayString = "${game.turn.watchdog-interval-ms:5000}")
    public void sweepAbandonedTurns() {
        List<GameSession> changed =
                sessionService.sweepAbandonedTurns(turnTimeout.toMillis(), maxMissedTurns);
        for (GameSession gameSession : changed) {
            messagingTemplate.convertAndSend("/topic/" + gameSession.getGameId(), gameSession);
            if (gameSession.isFinished()) {
                LOGGER.info("Game {} ended: player {} stopped responding, win awarded to player {}",
                        gameSession.getGameId(),
                        gameSession.getGameStatus() == 0 ? 2 : 1,
                        gameSession.getGameStatus() + 1);
            }
        }
    }
}
