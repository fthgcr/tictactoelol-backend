package com.tictactoegame.repositories;

import com.tictactoegame.models.GameSession;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.sql.SQLException;
import java.util.Optional;


public interface SessionRepository extends JpaRepository<GameSession,Integer> {

    GameSession findByGameId(String gameId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT t FROM GameSession t where t.isMatchmaking = true AND t.secondPlayer is null AND t.firstPlayer <> ?1 order by t.date asc limit 1")
    Optional<GameSession> findEmptySession(String username);
}
