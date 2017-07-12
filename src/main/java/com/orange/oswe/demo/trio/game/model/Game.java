package com.orange.oswe.demo.trio.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.*;

/**
 * Created by crhx7117 on 20/06/17.
 */
@Data
@EqualsAndHashCode(of="id")
public class Game {

    public enum State {
        preparing, playing, finished, over;
    }

    private final String id;
    private final String ownerId;
    private final Instant created = Instant.now();
    private State state = State.preparing;
    private Map<String, Player> players = new HashMap<>();
    private Map<String, Integer> scores = new HashMap<>();
    private Queue<String> queue = new ArrayDeque<>();
    private int cardsLeft = 81;
    private Card[] board = new Card[15];

    /**
     * Resets this game to prepare a new match
     */
    public void reset() {
        state = State.preparing;
        scores = new HashMap<>();
        queue = new ArrayDeque<>();
        cardsLeft = 81;
        board = new Card[15];
    }

    @JsonIgnore
    public Player getOwner() {
        return players.get(ownerId);
    }

    @JsonIgnore
    public boolean isOver() {
        return cardsLeft <= 0;
    }

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
    }

    public int setScore(String playerId, int score) {
        scores.put(playerId, score);
        return score;
    }

    public int getScore(String playerId) {
        Integer score = (Integer) scores.get(playerId);
        return score == null ? 0 : score;
    }

    public int incrScore(String playerId, int delta) {
        return setScore(playerId, getScore(playerId) + delta);
    }

    public void decrCardsLeft(int delta) {
        setCardsLeft(getCardsLeft() - delta);
    }

}
