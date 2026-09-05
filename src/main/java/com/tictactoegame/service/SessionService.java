package com.tictactoegame.service;

import com.tictactoegame.models.Champions;
import com.tictactoegame.models.GameSession;
import com.tictactoegame.models.requests.GameAreaRequest;
import com.tictactoegame.models.requests.SessionRequest;
import com.tictactoegame.repositories.SessionRepository;
import com.tictactoegame.utils.RuleCatalog;
import com.tictactoegame.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    private final ChampionsService championsService;

    private final RuleCatalog ruleCatalog;

    @Autowired
    SessionService(SessionRepository sessionRepository, ChampionsService championsService, RuleCatalog ruleCatalog){
        this.sessionRepository = sessionRepository;
        this.championsService = championsService;
        this.ruleCatalog = ruleCatalog;
    }

    /**
     * Create the session for a shared gameId, or take the free seat in it.
     * The repository performs the whole check-and-claim atomically, which replaces
     * the database transaction that used to guard this.
     */
    public GameSession createOrJoinGame(SessionRequest sessionRequest) {
        return sessionRepository.joinOrCreateGame(
                sessionRequest.getGameId(),
                sessionRequest.getPlayerIp(),
                () -> newSession(sessionRequest.getPlayerIp(), sessionRequest.getGameId(), false),
                this::createRules);
    }

    public GameSession healthCheckSession(String gameId){
        return sessionRepository.findByGameId(gameId);
    }

    /**
     * Server-authoritative move handling. The session state (board, turn, rules, winner)
     * always comes from the server-side store; the client only supplies "who", "where" and
     * "which champion". Invalid or out-of-turn requests simply get the current server
     * state broadcast back, so a tampered client cannot corrupt the game.
     *
     * The session object is locked for the duration of the move: without a database
     * transaction this is what keeps two simultaneous moves from interleaving.
     */
    public GameSession setPlayArea(GameAreaRequest gameAreaRequest) {
        GameSession gameSession = sessionRepository.findByGameId(gameAreaRequest.getGameId());
        if (gameSession == null) {
            return null;
        }

        synchronized (gameSession) {
            int player = playerIndexOf(gameSession, gameAreaRequest.getPlayerId());
            Integer index = gameAreaRequest.getIndex();
            boolean gameActive = gameSession.getGameStatus() == -1 && gameSession.getSecondPlayer() != null;
            boolean validMoveShape = index != null && index >= 0 && index < 9
                    && gameAreaRequest.getValue() != null && !gameAreaRequest.getValue().isBlank();

            // Not this player's turn, unknown player, finished game or malformed request:
            // re-broadcast the authoritative state untouched.
            if (!gameActive || !validMoveShape || player == -1 || !Objects.equals(gameSession.getTurn(), player)) {
                return gameSession;
            }

            String champion = gameAreaRequest.getValue().replace("'", "''");
            String[] playArea = gameSession.getPlayArea();

            // Occupied cell or champion already used elsewhere on the board:
            // reject without consuming the player's turn (protects against double-click races).
            if (!"0".equals(playArea[index]) || Arrays.asList(playArea).contains(champion)) {
                return gameSession;
            }

            // Rules come from the stored rule set, never from the request.
            if (gameSession.getGameRule() == null || gameSession.getGameRule().split(",").length < 6) {
                return gameSession;
            }
            String[] rules = gameSession.getGameRule().split(",");
            Champions champ = championsService.getChampionByName(champion);
            if (isRuleCorrect(Utils.deriveHorizontalRule(rules, index), champ)
                    && isRuleCorrect(Utils.deriveVerticalRule(rules, index), champ)) {
                playArea[index] = champion;
                gameSession.setPlayArea(playArea);

                String[] cellOwners = gameSession.getCellOwners();
                if (cellOwners == null || cellOwners.length < 9) {
                    cellOwners = Utils.fillEmptyCellOwners();
                }
                cellOwners[index] = String.valueOf(player);
                gameSession.setCellOwners(cellOwners);

                // Server-side win / draw detection.
                int winner = Utils.findWinner(cellOwners);
                if (winner != -1) {
                    gameSession.setGameStatus(winner);
                } else if (Utils.isBoardFull(playArea)) {
                    gameSession.setGameStatus(2); // draw
                }
            }
            // A wrong answer still costs the turn - that is part of the game design.
            // Answering at all proves the player is here, so their missed-turn tally resets.
            gameSession.clearMissedTurns(player);
            gameSession.startTurn(player == 0 ? 1 : 0);
            return sessionRepository.save(gameSession);
        }
    }

    /**
     * Skips the current turn on timeout. Only the player whose turn is running out
     * may skip it, so an opponent cannot steal turns.
     */
    public GameSession skipTurn(String gameId, String playerId) {
        GameSession gameSession = sessionRepository.findByGameId(gameId);
        if (gameSession == null) {
            return null;
        }
        synchronized (gameSession) {
            int player = playerIndexOf(gameSession, playerId);
            boolean gameActive = gameSession.getGameStatus() == -1 && gameSession.getSecondPlayer() != null;
            if (!gameActive || player == -1 || !Objects.equals(gameSession.getTurn(), player)) {
                return gameSession;
            }
            // The signal came from their own client, so they are still at the keyboard.
            gameSession.clearMissedTurns(player);
            gameSession.startTurn(player == 0 ? 1 : 0);
            return sessionRepository.save(gameSession);
        }
    }

    private int playerIndexOf(GameSession gameSession, String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return -1;
        }
        if (playerId.equalsIgnoreCase(gameSession.getFirstPlayer())) {
            return 0;
        }
        if (playerId.equalsIgnoreCase(gameSession.getSecondPlayer())) {
            return 1;
        }
        return -1;
    }

    private static final int MAX_RULE_GENERATION_ATTEMPTS = 500;

    private static final Random RANDOM = new Random();

    /** Fresh, empty session waiting for an opponent. */
    private GameSession newSession(String firstPlayer, String gameId, boolean matchmaking) {
        GameSession gameSession = new GameSession();
        gameSession.setFirstPlayer(firstPlayer);
        gameSession.setSecondPlayer(null);
        gameSession.setGameId(gameId);
        gameSession.setDate(new Date());
        gameSession.setTurn(0);
        gameSession.setGameStatus(-1);
        gameSession.setMatchmaking(matchmaking);
        gameSession.setPlayArea(Utils.fillEmptyGameArea());
        gameSession.setCellOwners(Utils.fillEmptyCellOwners());
        return gameSession;
    }

    private String createRules() {
        for (int attempt = 0; attempt < MAX_RULE_GENERATION_ATTEMPTS; attempt++) {
            String rules = createRule();
            if (checkRules(rules)) {
                return rules;
            }
        }
        throw new IllegalStateException(
                "Could not generate a solvable rule set after " + MAX_RULE_GENERATION_ATTEMPTS + " attempts");
    }

    // Picks 6 distinct categories in random order, then one random rule from each.
    // The catalog decides how many categories there are - it drops any whose data is
    // not loaded - so this must never fall below the 6 the board needs.
    private String createRule() {
        List<String[]> categories = ruleCatalog.categories();
        Collections.shuffle(categories, RANDOM);
        StringBuilder rules = new StringBuilder();
        for (int index = 0; index < 6; index++) {
            String[] category = categories.get(index);
            if (index > 0) {
                rules.append(",");
            }
            rules.append(category[RANDOM.nextInt(category.length)]);
        }
        return rules.toString();
    }

    public boolean isRuleCorrect(String rule, Champions champions){
        return Utils.isRuleCorrect(rule, champions);
    }

    public boolean checkRules(String rules){
        String[] splitArray = rules.split(",");
        // Defensive copy: getAllChampions() is @Cacheable and returns a shared, immutable
        // list, shuffling it in place would mutate the cache for every caller.
        List<Champions> allChampions = new ArrayList<>(championsService.getAllChampions());
        Collections.shuffle(allChampions);
        Set<Integer> championsSet = new HashSet<>();
        for (int index : List.of(0,1,2)) {
            for (int index2 : List.of(3,4,5)){
                boolean found = false;
                for (Champions champion : allChampions) {
                    if(championsSet.contains(champion.getPid()))
                        continue;
                    if(isRuleCorrect(splitArray[index], champion) && isRuleCorrect(splitArray[index2], champion)){
                        found = true;
                        championsSet.add(champion.getPid());
                        break;
                    }
                }
                if(!found){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Starts a rematch on the existing session instead of throwing it away.
     *
     * Deleting the session (as this used to do) meant the player who pressed Replay
     * recreated it as first player while the opponent knew nothing about it - and if
     * both pressed Replay, the second request deleted the session the first one had
     * just created. Resetting in place keeps one session, both players and the score,
     * and the result is broadcast so the two clients start the new round together.
     *
     * The loser of the previous round moves first, and pressing Replay twice is
     * harmless: an already reset (still active) session is returned untouched.
     *
     * Only the two players may start a rematch. Anyone else subscribed to the topic
     * is a spectator, and a spectator must not be able to wipe a finished board.
     *
     * @return the session both clients should switch to, or null if it is gone
     */
    public GameSession replayGame(String gameId, String playerId) {
        GameSession gameSession = sessionRepository.findByGameId(gameId);
        if (gameSession == null || gameSession.getSecondPlayer() == null) {
            return gameSession;
        }
        synchronized (gameSession) {
            if (!gameSession.isFinished() || playerIndexOf(gameSession, playerId) == -1) {
                return gameSession;
            }
            int previousWinner = gameSession.getGameStatus();
            gameSession.setPlayArea(Utils.fillEmptyGameArea());
            gameSession.setCellOwners(Utils.fillEmptyCellOwners());
            gameSession.setGameRule(createRules());
            gameSession.setGameStatus(-1);
            gameSession.setDate(new Date());
            gameSession.setEndReason(null);
            // Both players are demonstrably here - one asked for the rematch, the other
            // is about to see it - so neither carries a missed turn into the new round.
            gameSession.clearMissedTurns(0);
            gameSession.clearMissedTurns(1);
            // The loser opens the next round; after a draw (status 2) the first player does.
            boolean hadWinner = previousWinner == 0 || previousWinner == 1;
            gameSession.startTurn(hadWinner ? (previousWinner == 0 ? 1 : 0) : 0);
            return sessionRepository.save(gameSession);
        }
    }

    /**
     * Matchmaking queue: take the seat in the longest waiting session, or open a new one.
     * Claiming and rule generation happen atomically inside the repository.
     */
    public GameSession findEmptySession(String username) {
        return sessionRepository.joinOrCreateMatchmaking(
                username,
                () -> newSession(username, Utils.generateGameId(), true),
                this::createRules);
    }

    /**
     * Ends turns that nobody answered, and eventually the game itself.
     *
     * The normal turn timer lives in the browser of whoever is on the clock, so when that
     * player closes the tab the skip signal never arrives and the opponent waits forever.
     * This is the server-side backstop: it measures each turn against its own clock, so it
     * does not need either client to be present.
     *
     * The first expiry is treated as an ordinary skipped turn - a reload or a tunnel costs
     * you a turn, not the match. Only after maxMissedTurns unanswered turns *by the same
     * player* is the game handed to the opponent. The tally is per player and any action
     * clears it, so someone who is still playing can never be timed out by their own pace.
     *
     * @return the sessions whose state changed, for the caller to broadcast
     */
    public List<GameSession> sweepAbandonedTurns(long turnTimeoutMillis, int maxMissedTurns) {
        List<GameSession> changed = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (GameSession gameSession : sessionRepository.findAll()) {
            synchronized (gameSession) {
                if (gameSession.isFinished() || gameSession.getSecondPlayer() == null) {
                    continue;
                }
                Integer turn = gameSession.getTurn();
                if (turn == null || (turn != 0 && turn != 1)) {
                    continue;
                }
                // Fall back to the last write for sessions that predate the turn clock.
                long startedAt = gameSession.getTurnStartedAt() > 0
                        ? gameSession.getTurnStartedAt()
                        : gameSession.getLastActivity();
                if (startedAt <= 0 || now - startedAt < turnTimeoutMillis) {
                    continue;
                }

                int absent = turn;
                int present = absent == 0 ? 1 : 0;
                gameSession.recordMissedTurn(absent);
                if (gameSession.getMissedTurns(absent) >= maxMissedTurns) {
                    gameSession.setGameStatus(present);
                    gameSession.setEndReason(GameSession.END_REASON_OPPONENT_LEFT);
                } else {
                    gameSession.startTurn(present);
                }
                changed.add(sessionRepository.save(gameSession));
            }
        }
        return changed;
    }

    /**
     * Leaves an unstarted matchmaking seat. Only the player sitting in it may drop it,
     * and only while nobody has joined: without that check any caller who knew a uid
     * could delete a game in progress.
     */
    public void quitSession(Integer id, String playerId){
        if (id == null || playerId == null || playerId.isBlank()) {
            return;
        }
        sessionRepository.findAll().stream()
                .filter(session -> session.getUid() == id)
                .filter(session -> session.getSecondPlayer() == null)
                .filter(session -> playerId.equalsIgnoreCase(session.getFirstPlayer()))
                .findFirst()
                .ifPresent(session -> sessionRepository.deleteByGameId(session.getGameId()));
    }

}
