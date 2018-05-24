package com.name.brief.service;

import com.name.brief.exception.GameSessionAlreadyExistsException;
import com.name.brief.exception.GameSessionNotFoundException;
import com.name.brief.model.GameSession;
import com.name.brief.model.Player;
import com.name.brief.model.games.Game;
import com.name.brief.model.games.roleplay.RolePlay;
import com.name.brief.repository.GameSessionRepository;
import com.name.brief.repository.PlayerRepository;
import com.name.brief.utils.TimeConverter;
import com.name.brief.web.dto.GameSessionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class GameSessionServiceImpl implements GameSessionService {
    private final GameSessionRepository gameSessionRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public GameSessionServiceImpl(GameSessionRepository gameSessionRepository,
                                  PlayerRepository playerRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public void save(GameSession gameSession) throws GameSessionAlreadyExistsException {
        GameSession saved = getSession(gameSession.getStrId(), gameSession.getActiveDate());
        if (saved != null) throw new GameSessionAlreadyExistsException();

        // is done here because game need to be saved before referencing from unsaved gameSession
        gameSession.getGame().setGameSession(gameSession);
        gameSessionRepository.save(gameSession);
    }

    @Override
    public GameSession getSession(String strId, LocalDate activeDate) {
        return gameSessionRepository.findByStrIdAndActiveDate(strId, activeDate);
    }

    @Override
    public List<GameSession> getFutureSessions() {
        return gameSessionRepository.findSessionsAfter(LocalDate.now());
    }

    @Override
    public List<GameSession> getPastSessions() {
        return gameSessionRepository.findSessionsBefore(LocalDate.now());
    }

    @Override
    public GameSession getSession(Long gameSessionId) {
        return gameSessionRepository.findOne(gameSessionId);
    }

    @Override
    @Transactional
    public void changePhase(Long gameSessionId, int phaseNumber) {
        gameSessionRepository.changePhase(gameSessionId, phaseNumber);
    }

    @Override
    @Transactional
    public void activateTimer(Long gameSessionId, String durationStr) {
        gameSessionRepository.setEndOfTimer(gameSessionId, LocalTime.now()
                .plus(TimeConverter.getDurationFromTimeStr(durationStr)));
    }

    @Override
    public void changeRound(Long gameSessionId, int nextRoundIndex) {
        GameSession gameSession = gameSessionRepository.findOne(gameSessionId);
        if (gameSession == null) throw new GameSessionNotFoundException();
        gameSession.setCurrentRoundIndex(nextRoundIndex);
        gameSession.setCurrentPhaseNumber(0);
        gameSession.setEndOfTimer(null);
        gameSessionRepository.save(gameSession);
    }

    @Override
    public void update(GameSessionDto dto) {
        GameSession current = gameSessionRepository.findOne(dto.getGameSessionId());
        if (current != null) {
            // update gameSession fields
            current.setStrId(dto.getNewStrId());
            current.setActiveDate(dto.getActiveDate());

            // update players usernames
            List<Player> players = current.getPlayers();
            players.forEach(player -> player.setUsername(Player.constructUsername(
                    current.getStrId(),
                    current.getActiveDate(),
                    player.getCommandName())
            ));

            gameSessionRepository.save(current);
        }
    }

    @Override
    public void delete(Long gameSessionId) {
        GameSession gameSession = gameSessionRepository.findOne(gameSessionId);
        if (gameSession == null) throw new GameSessionNotFoundException();
//        gameSession.getPlayers().forEach(playerAuthenticationService::logout);
        gameSessionRepository.delete(gameSessionId);

    }

    @Override
    public void nullPlayersAnswers(Long gameSessionId) {
        GameSession session = getSession(gameSessionId);
        session.getPlayers().forEach(p -> p.getDecisions().forEach(d -> d.setAnswer(null)));
        gameSessionRepository.save(session);
    }

    @Override
    public Player addPlayer(Player player, GameSession session) {
        session = gameSessionRepository.findOne(session.getId());

        if (session == null) throw new GameSessionNotFoundException();

        player.setGameSession(session);
        playerRepository.save(player);

        player.setUsername("player" + player.getId());
        playerRepository.save(player);

        return player;
    }

    @Override
    public void removePlayer(Player player) {
        GameSession gameSession = gameSessionRepository.findOne(player.getGameSession().getId());

        // if gameSession is null it means that it was deleted and thus player was already deleted too
        if (gameSession == null) return;

        gameSession.getPlayers().removeIf(p -> p.getId().equals(player.getId()));
        Game game = gameSession.getGame();
        if (game instanceof RolePlay) {
            ((RolePlay) game).getPlayersData().removeIf(data ->
                    data.getPlayer().getId().equals(player.getId()));
        }
        gameSessionRepository.save(gameSession);
    }
}
