package com.tictactoegame.repositories;

import com.tictactoegame.models.Champions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChampionsRepository extends JpaRepository<Champions,Integer> {

    Optional<Champions> findByName(String name);
}
