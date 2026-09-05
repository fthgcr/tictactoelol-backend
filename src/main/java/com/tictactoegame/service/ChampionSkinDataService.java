package com.tictactoegame.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictactoegame.repositories.ChampionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Keeps each champion's skin count up to date from Riot's Data Dragon.
 *
 * Every other champion attribute is lore or design and sits still for years, so it lives
 * in champions.json. Skin counts do not: Riot ships skins every patch, and a hand
 * maintained number would be wrong within weeks and would quietly generate rules nobody
 * can satisfy. So this is the one attribute the server fetches instead of storing.
 *
 * Nothing here blocks startup. The job runs on the scheduler right after boot and then
 * every few hours; until the first successful run champions carry a null skin count and
 * {@link com.tictactoegame.utils.RuleCatalog} keeps the whole skin category out of the
 * game. A failed fetch therefore costs one rule category, never a broken board.
 */
@Service
public class ChampionSkinDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChampionSkinDataService.class);

    private static final String VERSIONS_URL = "https://ddragon.leagueoflegends.com/api/versions.json";

    private static final String CHAMPIONS_URL_TEMPLATE =
            "https://ddragon.leagueoflegends.com/cdn/%s/data/en_US/championFull.json";

    private final ChampionsRepository championsRepository;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final boolean enabled;

    private final Duration requestTimeout;

    /** Flipped once by the first successful refresh; never goes back to false. */
    private volatile boolean skinDataAvailable = false;

    public ChampionSkinDataService(
            ChampionsRepository championsRepository,
            ObjectMapper objectMapper,
            @Value("${game.skins.enabled:true}") boolean enabled,
            @Value("${game.skins.request-timeout:PT10S}") Duration requestTimeout) {
        this.championsRepository = championsRepository;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** True once champions carry a skin count, i.e. once the skin rules may be used. */
    public boolean isSkinDataAvailable() {
        return skinDataAvailable;
    }

    /**
     * Refreshes the counts. Runs shortly after startup and then on the configured
     * interval, which also picks up skins released while the server was up.
     */
    @Scheduled(
            initialDelayString = "${game.skins.initial-delay-ms:2000}",
            fixedDelayString = "${game.skins.refresh-interval-ms:21600000}")
    public void refreshSkinCounts() {
        if (!enabled) {
            return;
        }
        try {
            String version = fetchLatestVersion();
            Map<String, Integer> countsByName = fetchSkinCounts(version);
            int applied = championsRepository.applySkinCounts(countsByName);
            int total = (int) championsRepository.count();
            if (applied == 0) {
                LOGGER.warn("Data Dragon {} matched none of the {} champions; skin rules stay disabled",
                        version, total);
                return;
            }
            skinDataAvailable = true;
            if (applied < total) {
                // A champion we know that Data Dragon does not, or a name that stopped
                // matching. Not fatal: the rules still work, that champion just never
                // satisfies a skin rule - worth a log line so it gets noticed.
                LOGGER.warn("Skin counts refreshed from Data Dragon {} for {}/{} champions; "
                        + "unmatched: {}", version, applied, total,
                        championsRepository.namesWithoutSkinCount());
            } else {
                LOGGER.info("Skin counts refreshed from Data Dragon {} for all {} champions",
                        version, total);
            }
        } catch (Exception exception) {
            // Deliberately swallowed: the game is fully playable without this category,
            // and a scheduled job that throws would just be retried on the next tick anyway.
            LOGGER.warn("Could not refresh skin counts from Data Dragon ({}); "
                    + "skin rules stay {}", exception.toString(),
                    skinDataAvailable ? "on the previous data" : "disabled");
        }
    }

    private String fetchLatestVersion() throws Exception {
        JsonNode versions = objectMapper.readTree(get(VERSIONS_URL));
        if (!versions.isArray() || versions.isEmpty()) {
            throw new IllegalStateException("Data Dragon returned no versions");
        }
        return versions.get(0).asText();
    }

    private Map<String, Integer> fetchSkinCounts(String version) throws Exception {
        JsonNode data = objectMapper.readTree(get(String.format(CHAMPIONS_URL_TEMPLATE, version))).path("data");
        Map<String, Integer> counts = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> entries = data.fields();
        while (entries.hasNext()) {
            JsonNode champion = entries.next().getValue();
            String name = champion.path("name").asText(null);
            if (name == null) {
                continue;
            }
            // Data Dragon counts the base skin ("default") as a skin; players do not.
            int skins = Math.max(0, champion.path("skins").size() - 1);
            counts.put(normalizeName(name), skins);
        }
        if (counts.isEmpty()) {
            throw new IllegalStateException("Data Dragon " + version + " returned no champions");
        }
        return counts;
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("GET " + url + " returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * "Nunu &amp; Willump", "Dr. Mundo" and "Kai'Sa" are spelled the same on both sides but
     * only if punctuation and spacing are ignored, which is all this does.
     */
    public static String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
