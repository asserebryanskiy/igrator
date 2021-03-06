package com.name.brief.web.dto;

import com.name.brief.model.GameSession;
import com.name.brief.model.games.Brief;
import com.name.brief.model.games.Game;
import com.name.brief.model.games.RiskMap;
import com.name.brief.model.games.riskmap.RiskMapType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class GameSessionDto {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final String[] gameTypes = new String[]{"Бриф", "Карта рисков"};
    private final RiskMapType[] riskMapTypes = RiskMapType.values();
    private RiskMapType riskMapType = RiskMapType.OFFICE;
    private String oldStrId;
    private String newStrId;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate activeDate = LocalDate.now();
    private String gameType = gameTypes[0];
    private Long gameSessionId;
    private int numberOfCommands = 5;

    public GameSession createGameSession() {
        GameSession session = new GameSession.GameSessionBuilder(getNewStrId())
                .withActiveDate(getActiveDate())
                .withNumberOfCommands(numberOfCommands)
                .withGame(createGame())
                .build();
        if (gameSessionId != null) session.setId(gameSessionId);
        return session;
    }

    private Game createGame() {
        switch (gameType) {
            case "Бриф":
                return new Brief();
            case "Карта рисков": {
                RiskMap riskMap = new RiskMap();
                riskMap.setType(riskMapType);
                return riskMap;
            }
        }
        return new Brief();
    }

    public String getNewStrId() {
        return newStrId == null || newStrId.isEmpty() ?
                (oldStrId == null || oldStrId.isEmpty() ?
                createGame().getEnglishName() + activeDate.format(DATE_FORMATTER) : oldStrId) : newStrId;
    }

    public static Map<String, GameSessionDto> getDtosMap(List<GameSession> sessions) {
        Map<String, GameSessionDto> dtos = new HashMap<>(sessions.size());
        sessions.forEach(s -> dtos.put("dto_" + s.getStrId(), createFrom(s)));
        return dtos;
    }

    public static GameSessionDto createFrom(GameSession session) {
        GameSessionDto dto = new GameSessionDto();
        dto.setGameSessionId(session.getId());
        dto.setOldStrId(session.getStrId());
        dto.setActiveDate(session.getActiveDate());
        dto.setGameType(session.getGame().getEnglishName());
        dto.setNumberOfCommands(session.getPlayers().size());
        return dto;
    }
}
