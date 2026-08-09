package com.tictactoegame.service;

import com.tictactoegame.repositories.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * TTL sweep for the in-memory session store.
 *
 * Without a database nothing ever deletes rows for players who just close the tab,
 * so those sessions would pile up for as long as the server runs. This job removes:
 * - finished matches, shortly after both clients have had time to read the final board;
 * - any session that has not been written to for a longer idle period (abandoned games
 *   and matchmaking seats nobody ever took).
 *
 * All three values are configurable in application.properties.
 */
@Service
public class SessionCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCleanupService.class);

    private final SessionRepository sessionRepository;

    private final Duration finishedTtl;

    private final Duration idleTtl;

    public SessionCleanupService(SessionRepository sessionRepository,
                                 @Value("${game.session.finished-ttl:PT30M}") Duration finishedTtl,
                                 @Value("${game.session.idle-ttl:PT2H}") Duration idleTtl) {
        this.sessionRepository = sessionRepository;
        this.finishedTtl = finishedTtl;
        this.idleTtl = idleTtl;
    }

    @Scheduled(fixedDelayString = "${game.session.cleanup-interval-ms:600000}",
            initialDelayString = "${game.session.cleanup-interval-ms:600000}")
    public void removeExpiredSessions() {
        int removed = sessionRepository.removeExpired(finishedTtl.toMillis(), idleTtl.toMillis());
        if (removed > 0) {
            LOGGER.info("Session cleanup removed {} expired session(s), {} still active",
                    removed, sessionRepository.count());
        }
    }
}
