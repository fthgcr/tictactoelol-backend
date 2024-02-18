package com.tictactoegame.repositories;

import com.tictactoegame.models.Champions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChampionsRepository extends JpaRepository<Champions,Integer> {

    Optional<Champions> findByName(String name);

    @Query("SELECT c FROM Champions c WHERE LOWER(c.name) LIKE LOWER(CONCAT(:prefix, '%')) AND LOWER(c.name) LIKE LOWER(CONCAT('%', :suffix))")
    Champions findByNameSpecialCharacters(String prefix, String suffix);
}
