package com.name.brief.model;

import com.name.brief.model.games.Brief;
import com.name.brief.model.games.Game;
import com.name.brief.utils.TimeConverter;
import com.name.brief.web.dto.StatsList;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

@Entity
@Data
@ToString(exclude = {"players", "rounds"})
//@ValidateDateRange(startDate = "activeDate", endDate = "endDate")
public class GameSession extends BaseEntity{
    @NotNull
    private String strId;
    @NotNull
    private LocalDate activeDate;
    @NotNull
    private Game game;
    @Transient
    private int numberOfCommands;
    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Player> players;
    private int[] rounds;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private int currentPhaseNumber;   // is used to track current status of game
    private int currentRoundIndex;    // is used to track current status of game

    // if user reloads page this aids in restoring timer
    private LocalTime endOfTimer;

    public GameSession() {
        super();
    }

    public GameSession(String strId,
                       LocalDate activeDate,
                       Game game,
                       int numberOfCommands,
                       User user) {
        this();
        this.strId = strId;
        this.activeDate = activeDate;
        this.game = game;
        players = new ArrayList<>(numberOfCommands);
        IntStream.range(0, numberOfCommands)
                .forEach(i -> players.add(new Player(this, String.valueOf((i + 1)))));
        rounds = new int[game.getNumberOfRounds()];
        this.user = user;
    }

    public StatsList getStatsList() {
        StatsList stats = new StatsList(players.size());
        players.forEach(p -> {
            SortedMap<Integer, Integer> roundScoreMap = new TreeMap<>();
            p.getDecisions().forEach(d -> roundScoreMap.put(d.getRoundNumber(), game.getScore(d)));
            stats.addStatistic(p.getCommandName(), roundScoreMap);
        });
        return stats;
    }

    public static class GameSessionBuilder {
        private String strId;
        private LocalDate activeDate;
        private Game game;
        private int numberOfCommands;
        private User user;

        public GameSessionBuilder(String strId) {
            this.strId = strId;
        }

        public GameSessionBuilder withActiveDate(LocalDate activeDate) {
            this.activeDate = activeDate;
            return this;
        }

        public GameSessionBuilder withGame(Game game) {
            this.game = game;
            return this;
        }

        public GameSessionBuilder withNumberOfCommands(int numberOfCommands) {
            this.numberOfCommands = numberOfCommands;
            return this;
        }

        public GameSessionBuilder withUser(User user) {
            this.user = user;
            return this;
        }

        public GameSession build() {
            return new GameSession(
                    this.strId,
                    this.activeDate == null ? LocalDate.now() : this.activeDate,
                    this.game == null ? new Brief() : this.game,
                    this.numberOfCommands == 0 ? 5 : numberOfCommands,
                    this.user != null ? this.user : getDefaultUser()
            );
        }

        private User getDefaultUser() {
            User principal;
            try {
                principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                principal = null;
            }
            return principal != null ? principal : new User();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GameSession session = (GameSession) o;
        return Objects.equals(strId, session.strId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), strId);
    }

    public String getRemainingTimerTime() {
        if (endOfTimer == null) throw new IllegalArgumentException("endOfTimer is null");
        LocalTime now = LocalTime.now();
        if (now.isAfter(endOfTimer)) return "00:00";
        return TimeConverter.getTimeStrFromDuration(Duration.between(LocalTime.now(), endOfTimer));
    }

    public void activateTimer(Duration seconds) {
        endOfTimer = LocalTime.now().plus(seconds);
    }

    public boolean timerIsRunning() {
        return endOfTimer != null && endOfTimer.isAfter(LocalTime.now());
    }
}