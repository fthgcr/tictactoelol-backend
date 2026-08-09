package com.tictactoegame.repositories;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictactoegame.models.Champions;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory champion "table". Behaves like the old Spring Data repository
 * (same method names and return types) but is backed by a static JSON resource
 * instead of a database, so the application needs no datasource at all.
 *
 * The data is read once at startup and then served from immutable collections,
 * which makes every lookup a plain map/list access.
 */
@Repository
public class ChampionsRepository {

    private static final String DATA_PATH = "data/champions.json";

    private final ObjectMapper objectMapper;

    private List<Champions> champions = List.of();

    /** Lower-cased name -> champion, for O(1) exact lookups. */
    private Map<String, Champions> championsByName = Map.of();

    public ChampionsRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        ClassPathResource resource = new ClassPathResource(DATA_PATH);
        if (!resource.exists()) {
            throw new IllegalStateException("Champion data file not found on the classpath: " + DATA_PATH);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            List<Champions> loaded = objectMapper.readValue(inputStream, new TypeReference<List<Champions>>() {});
            if (loaded == null || loaded.isEmpty()) {
                throw new IllegalStateException("Champion data file is empty: " + DATA_PATH);
            }
            Map<String, Champions> byName = new LinkedHashMap<>(loaded.size() * 2);
            for (Champions champion : loaded) {
                byName.put(normalize(champion.getName()), champion);
            }
            this.champions = List.copyOf(loaded);
            this.championsByName = Map.copyOf(byName);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read champion data from " + DATA_PATH, exception);
        }
    }

    /**
     * All champions. The returned list is immutable - callers that need to sort or
     * shuffle must copy it first (as the services already do).
     */
    public List<Champions> findAll() {
        return champions;
    }

    public Optional<Champions> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(championsByName.get(normalize(name)));
    }

    /**
     * Replacement for the old "name LIKE prefix% AND name LIKE %suffix" query, used for
     * names containing an apostrophe (Kai'Sa, Cho'Gath...) where the client may send a
     * differently escaped variant.
     */
    public Champions findByNameSpecialCharacters(String prefix, String suffix) {
        if (prefix == null || suffix == null) {
            return null;
        }
        String lowerPrefix = normalize(prefix);
        String lowerSuffix = normalize(suffix);
        for (Champions champion : champions) {
            String name = normalize(champion.getName());
            if (name.startsWith(lowerPrefix) && name.endsWith(lowerSuffix)) {
                return champion;
            }
        }
        return null;
    }

    public long count() {
        return champions.size();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /** Defensive helper for callers that want a mutable copy. */
    public List<Champions> findAllMutable() {
        return new ArrayList<>(champions);
    }
}
