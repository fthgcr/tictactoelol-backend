package com.tictactoegame.repositories;

import com.tictactoegame.models.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<GameSession,Integer> {

    GameSession findByGameId(String gameId);

}
