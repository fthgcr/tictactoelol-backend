package com.tictactoegame.service;

import com.tictactoegame.models.Champions;
import com.tictactoegame.models.GameSession;
import com.tictactoegame.models.requests.GameAreaRequest;
import com.tictactoegame.models.requests.SessionRequest;
import com.tictactoegame.repositories.SessionRepository;
import com.tictactoegame.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

import com.tictactoegame.utils.Consts;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    private final ChampionsService championsService;

    @Autowired
    SessionService(SessionRepository sessionRepository, ChampionsService championsService){
        this.sessionRepository = sessionRepository;
        this.championsService = championsService;
    }

    public GameSession createOrJoinGame(SessionRequest sessionRequest) throws IllegalAccessException {
        GameSession gameSession = sessionRepository.findByGameId(sessionRequest.getGameId());
        if(gameSession == null){
            gameSession = new GameSession();
            gameSession.setFirstPlayer(sessionRequest.getPlayerIp());
            gameSession.setGameId(sessionRequest.getGameId());
            gameSession.setDate(new Date());
            gameSession.setTurn(0);
            gameSession.setGameStatus(-1);
            gameSession.setPlayArea(Utils.fillEmptyGameArea());
        } else if (gameSession.getSecondPlayer() == null && !gameSession.getFirstPlayer().equalsIgnoreCase(sessionRequest.getPlayerIp())){
            gameSession.setGameRule(createRules().toString());
            gameSession.setSecondPlayer(sessionRequest.getPlayerIp());
        }

        return sessionRepository.save(gameSession);
    }

    public GameSession healthCheckSession(String gameId){
        return sessionRepository.findByGameId(gameId);
    }

    public GameSession setPlayArea(GameAreaRequest gameAreaRequest) {
        //If the rules match the champion (gameAreaRequest.getValue())
        GameSession gameSession = Utils.gameAreaRequestToGameSession(gameAreaRequest);
        gameAreaRequest.setValue(gameAreaRequest.getValue().replace("'", "''"));
        if(isRuleCorrect(gameAreaRequest.getHorizontalRule(),championsService.getChampionByName(gameAreaRequest.getValue())) &&
                isRuleCorrect(gameAreaRequest.getVerticalRule(),championsService.getChampionByName(gameAreaRequest.getValue()))){

            gameSession.getPlayArea()[gameAreaRequest.getIndex()] = gameAreaRequest.getValue();
            gameSession.setPlayArea(gameSession.getPlayArea());
        }
        gameSession.setTurn(gameAreaRequest.getTurn() == 0 ? 1 : 0);
        return sessionRepository.save(gameSession);
    }

    private StringBuilder createRules() throws IllegalAccessException {
        while(true){
            StringBuilder rules = createRule();
            if(checkRules(rules.toString()))
                return rules;
        }
    }

    private StringBuilder createRule() throws IllegalAccessException {
        StringBuilder rules = new StringBuilder();
        List<Field> fieldList = Arrays.asList(Consts.class.getDeclaredFields());
        for(int index = 0; index <6 ; index++){
            Map<Field, List<Field>> fieldMap = getRandomField(fieldList);
            for (Map.Entry<Field, List<Field>> entry : fieldMap.entrySet()) {
                Field keyField = entry.getKey();
                fieldList = entry.getValue();
                keyField.setAccessible(true);
                rules.append(getRandomElementFromArray((String[]) keyField.get(null))).append(",");
            }
        }
        fieldList.clear();
        rules.deleteCharAt(rules.length() - 1);
        return rules;
    }

    private Map<Field, List<Field>> getRandomField(List<Field> fields) {
        Map<Field, List<Field>> fieldMap = new HashMap<>();

        List<Field> mutableFields = new ArrayList<>(fields);

        Random random = new Random();
        int randomIndex = random.nextInt(mutableFields.size());

        Field randomField = mutableFields.get(randomIndex);
        mutableFields.remove(randomIndex);

        fieldMap.put(randomField, mutableFields);
        return fieldMap;
    }

    private String getRandomElementFromArray(String[] keyArray){
        Random random = new Random();
        int randomIndex = random.nextInt(keyArray.length);
        return keyArray[randomIndex];
    }

    public boolean isRuleCorrect(String rule, Champions champions){
        String ruleName = Utils.getPureRuleString(rule);
        boolean isItCorrect = false;
        switch (rule.substring(0, rule.indexOf(" : ")).trim()) {
            case "Region":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getRegion());
                break;
            case "Difficulty":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getDifficulty());
                break;
            case "Role":
                String[] roles = Utils.splitString(champions.getRole());
                for (String role : roles) {
                    if (role.equalsIgnoreCase(ruleName)) {
                        isItCorrect = true;
                        break;
                    }
                }
                break;
            case "Release Date":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getReleaseDate());
                break;
            case "Ability Resource":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getAbilityResource());
                break;
            case "Melee/Ranged":
                String[] meleeRanged = Utils.splitString(champions.getMeleeRanged());
                for (String mr : meleeRanged) {
                    if (mr.equalsIgnoreCase(ruleName)) {
                        isItCorrect = true;
                        break;
                    }
                }
                break;
            case "Gender":
                isItCorrect = ruleName.equalsIgnoreCase(champions.getGender());
                break;
        }

        return isItCorrect;
    }

    public boolean checkRules(String rules){
        String[] splitArray = rules.split(",");
        List<Champions> allChampions = championsService.getAllChampions();
        Collections.shuffle(allChampions);
        for (int index : List.of(0,1,2)) {
            for (int index2 : List.of(3,4,5)){
                boolean found = false;
                for (Champions champion : allChampions) {
                    if(isRuleCorrect(splitArray[index], champion) && isRuleCorrect(splitArray[index2], champion)){
                        found = true;
                        allChampions.removeIf(obj -> obj.getPid() == champion.getPid());
                        break;
                    }
                }
                if(!found){
                    System.out.println(splitArray[index] + " - " + splitArray[index2]);
                    return false;
                }
            }
        }
        return true;
    }

    public Boolean replaySession (String gameId){
        GameSession gameSession = sessionRepository.findByGameId(gameId);
        if(gameSession != null && gameSession.getSecondPlayer() != null){
            sessionRepository.deleteById(gameSession.getUid());
        }
        return true;
    }


}
