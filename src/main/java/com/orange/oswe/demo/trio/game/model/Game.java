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

    /**
     * Total number of cards in a deck
     */
    public static final int TOTAL_NUMBER_OF_CARDS = 81;
    /**
     * Normal board size: number of cards usually displayed (if at least one trio is present)
     */
    public static final int NORMAL_BOARD_SIZE = 12;
    /**
     *Max board size: number of cars displayed in case no trio was present in the normal board
     */
    public static final int FULL_BOARD_SIZE = 15;

    public enum State {
        /**
         * initial game state
         * players are authorized to join and leave until the owner changes to {@link #playing} state
         */
        preparing,
        /**
         * playing state
         * players are authorized to send game actions
         * lasts until there are no more cards in the deck and no more trio on the board
         * then automatically moves to {@link #over} state
         */
        playing,
        /**
         * when the game has ended
         * lasts until the owner moves to {@link #preparing} state
         */
        over;
    }

    private final String id;
    private final String ownerId;
    private final Instant created = Instant.now();
    private State state = State.preparing;
    private Map<String, Player> players = new HashMap<>();
    private Map<String, Integer> scores = new HashMap<>();
    private Queue<String> queue = new ArrayDeque<>();
    private int cardsLeft = TOTAL_NUMBER_OF_CARDS;
    private Card[] board = new Card[FULL_BOARD_SIZE];

    /**
     * Resets this game to prepare a new match
     */
    public void reset() {
        state = State.preparing;
        scores = new HashMap<>();
        queue = new ArrayDeque<>();
        cardsLeft = TOTAL_NUMBER_OF_CARDS;
        board = new Card[FULL_BOARD_SIZE];
    }

    @JsonIgnore
    public boolean hasCardsLeft() {
        return cardsLeft > 0;
    }

    @JsonIgnore
    public Player getOwner() {
        return players.get(ownerId);
    }

    public void add(Player player) {
        players.put(player.getId(), player);
    }

    public void remove(Player player) {
        players.remove(player.getId());
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
