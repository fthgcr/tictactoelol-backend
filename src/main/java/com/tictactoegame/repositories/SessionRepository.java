package com.tictactoegame.repositories;

import com.tictactoegame.models.GameSession;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * In-memory replacement for the old JPA session repository.
 *
 * Sessions live in a {@link ConcurrentHashMap} keyed by gameId, which means:
 * - no database, no datasource, no transactions;
 * - state is lost on restart (acceptable: a match only lasts a few minutes);
 * - the two "join" operations below take the place of the old pessimistic row lock,
 *   so two players can never claim the same waiting session.
 *
 * Everything a caller receives is the live object, so a mutation followed by
 * {@link #save(GameSession)} is enough to make the change visible - save() mainly
 * refreshes the activity timestamp used by the TTL cleanup job.
 */
@Repository
public class SessionRepository {

    private final Map<String, GameSession> sessionsByGameId = new ConcurrentHashMap<>();

    private final AtomicInteger uidSequence = new AtomicInteger();

    /** Guards the read-then-write "claim a seat" sequences. */
    private final Object joinLock = new Object();

    public GameSession findByGameId(String gameId) {
        return gameId == null ? null : sessionsByGameId.get(gameId);
    }

    public GameSession save(GameSession gameSession) {
        if (gameSession == null) {
            return null;
        }
        if (gameSession.getUid() == 0) {
            gameSession.setUid(uidSequence.incrementAndGet());
        }
        touch(gameSession);
        sessionsByGameId.put(gameSession.getGameId(), gameSession);
        return gameSession;
    }

    public void deleteById(int uid) {
        sessionsByGameId.values().removeIf(session -> session.getUid() == uid);
    }

    public void deleteByGameId(String gameId) {
        if (gameId != null) {
            sessionsByGameId.remove(gameId);
        }
    }

    public List<GameSession> findAll() {
        return new ArrayList<>(sessionsByGameId.values());
    }

    public int count() {
        return sessionsByGameId.size();
    }

    /**
     * Create-or-join for a known gameId (invite link flow).
     *
     * The whole read-modify-write runs under the join lock and the rules are produced
     * inside it, so the second player and the rule set become visible together - exactly
     * like the single database write it replaces. A client can therefore never observe a
     * session that has an opponent but no rules.
     *
     * @param newSessionFactory builds a fresh session when the gameId is unknown
     * @param gameRuleFactory   produces the rule set when a second player joins
     */
    public GameSession joinOrCreateGame(String gameId,
                                        String playerId,
                                        Supplier<GameSession> newSessionFactory,
                                        Supplier<String> gameRuleFactory) {
        synchronized (joinLock) {
            GameSession gameSession = findByGameId(gameId);
            if (gameSession == null) {
                return save(newSessionFactory.get());
            }
            if (gameSession.getSecondPlayer() == null
                    && !gameSession.getFirstPlayer().equalsIgnoreCase(playerId)) {
                gameSession.setGameRule(gameRuleFactory.get());
                gameSession.setSecondPlayer(playerId);
                // The match starts here, so the first turn's clock starts here too.
                gameSession.startTurn(0);
                touch(gameSession);
            }
            return gameSession;
        }
    }

    /**
     * Matchmaking: claim the longest waiting open session, or open a new one.
     * Replaces the old "SELECT ... FOR UPDATE ... LIMIT 1" query.
     *
     * @return the joined session (secondPlayer == username) or the newly created
     *         session the caller now waits in
     */
    public GameSession joinOrCreateMatchmaking(String username,
                                               Supplier<GameSession> newSessionFactory,
                                               Supplier<String> gameRuleFactory) {
        synchronized (joinLock) {
            GameSession waiting = findLongestWaiting(username);
            if (waiting == null) {
                return save(newSessionFactory.get());
            }
            waiting.setGameRule(gameRuleFactory.get());
            waiting.setSecondPlayer(username);
            // The match starts here, so the first turn's clock starts here too.
            waiting.startTurn(0);
            touch(waiting);
            return waiting;
        }
    }

    private GameSession findLongestWaiting(String username) {
        return sessionsByGameId.values().stream()
                .filter(GameSession::isMatchmaking)
                .filter(session -> session.getSecondPlayer() == null)
                .filter(session -> session.getFirstPlayer() != null
                        && !session.getFirstPlayer().equalsIgnoreCase(username))
                .min(Comparator.comparing(GameSession::getDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    /**
     * Drops sessions nobody is going to come back to.
     *
     * @param finishedTtlMillis how long a finished match is kept so both clients can
     *                          still read the final board
     * @param idleTtlMillis     how long an untouched session is kept (covers players who
     *                          simply closed the tab, which is the main leak source)
     * @return number of removed sessions
     */
    public int removeExpired(long finishedTtlMillis, long idleTtlMillis) {
        long now = System.currentTimeMillis();
        Collection<GameSession> sessions = sessionsByGameId.values();
        int before = sessions.size();
        sessions.removeIf(session -> {
            long age = now - lastActivityOf(session);
            return session.isFinished() ? age > finishedTtlMillis : age > idleTtlMillis;
        });
        return before - sessions.size();
    }

    private static long lastActivityOf(GameSession session) {
        if (session.getLastActivity() > 0) {
            return session.getLastActivity();
        }
        Date date = session.getDate();
        return date == null ? 0L : date.getTime();
    }

    private static void touch(GameSession gameSession) {
        gameSession.setLastActivity(System.currentTimeMillis());
    }
}
