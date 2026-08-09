package com.tictactoegame.service;

import com.tictactoegame.models.Champions;
import com.tictactoegame.repositories.ChampionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ChampionsService {

    private final ChampionsRepository repository;

    @Autowired
    ChampionsService(ChampionsRepository repository){
        this.repository = repository;
    }

    @Cacheable("allChampions")
    public List<Champions> getAllChampions() {
        return repository.findAll();
    }

    @Cacheable(value = "championByName", key = "#name")
    public Champions getChampionByName(String name) {
        if (name == null) {
            return null;
        }
        String[] parts = name.split("\\'");
        if (parts.length > 2) {
            return repository.findByNameSpecialCharacters(parts[0], parts[parts.length - 1]);
        }
        return repository.findByName(name).orElse(null);
    }
}
