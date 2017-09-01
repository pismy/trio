package com.orange.oswe.demo.trio.game.events;

import com.orange.oswe.demo.trio.game.model.Card;
import com.orange.oswe.demo.trio.game.model.Game;
import com.orange.oswe.demo.trio.game.model.Player;
import lombok.Value;

import java.util.List;
import java.util.Queue;

/**
 * Created by crhx7117 on 20/06/17.
 */
public abstract class Event {

    public enum Type {
        game_state_changed, player_joined, player_left, player_selects, player_declares, select_timeout, select_giveup, select_nolonger, select_success, select_failure, cards_moved, cards_drawn
    }

    public enum DrawReason {
        /**
         * refill board up to 12 cards
         */
        refill,
        /**
         * drawn 3 extra cards because no trio on base board
         */
        extra,
        /**
         * replaced 3 extra cards because no trio on extended board
         */
        replaced
    }

    public abstract Type getType();

    @Value
    public static class GameStateChanged extends Event {
        private final Game.State state;

        @Override
        public Type getType() {
            return Type.game_state_changed;
        }
    }

    @Value
    public static class PlayerEvent extends Event {
        private final Type type;
        private final Player player;
    }

    @Value
    public static class GameEvent extends Event {
        private final Type type;
        private final Player player;
        private final Integer newScore;
        private final Queue<String> queue;
    }

    @Value
    public static class TrioFailureEvent extends Event {
        private final Player player;
        private final List<Card.Attribute> faulty;
        private final Integer newScore;
        private final Queue<String> queue;

        @Override
        public Type getType() {
            return Type.select_failure;
        }
    }

    @Value
    public static class TrioFoundEvent extends Event {
        private final Player player;
        private final int[] positions;
        private final Integer newScore;
        private final Queue<String> queue;

        @Override
        public Type getType() {
            return Type.select_success;
        }
    }

    @Value
    public static class CardsDrawnEvent extends Event {
        private final DrawReason reason;
        private final int nbCardsBeforeDraw;
        private final Card[] cards;
        private final int[] positions;

        @Override
        public Type getType() {
            return Type.cards_drawn;
        }
    }

    @Value
    public static class CardsMovedEvent extends Event {
        private final int[] from;
        private final int[] to;

        @Override
        public Type getType() {
            return Type.cards_moved;
        }
    }

    public static Event gameStateChanged(Game.State state) {
        return new GameStateChanged(state);
    }

    public static Event playerJoined(Player player) {
        return new PlayerEvent(Type.player_joined, player);
    }

    public static Event playerLeft(Player player) {
        return new PlayerEvent(Type.player_left, player);
    }

    public static Event playerSelectsTrio(Player player, Integer newScore, Queue<String> queue) {
        return new GameEvent(Type.player_selects, player, newScore, queue);
    }

    public static Event playerDeclaresTrio(Player player, Integer newScore, Queue<String> queue) {
        return new GameEvent(Type.player_declares, player, newScore, queue);
    }

    public static Event trioSelectionTimeouted(Player player, Integer newScore, Queue<String> queue) {
        return new GameEvent(Type.select_timeout, player, newScore, queue);
    }

    public static Event trioSelectionGiveUp(Player player, Integer newScore, Queue<String> queue) {
        return new GameEvent(Type.select_giveup, player, newScore, queue);
    }

    public static Event trioSelectionWithdraw(Player player, Integer newScore, Queue<String> queue) {
        return new GameEvent(Type.select_nolonger, player, newScore, queue);
    }

    public static Event trioSelectionFailure(Player player, List<Card.Attribute> faulty, Integer newScore, Queue<String> queue) {
        return new TrioFailureEvent(player, faulty, newScore, queue);
    }

    public static Event trioSelectionSuccess(Player player, int[] positions, Integer newScore, Queue<String> queue) {
        return new TrioFoundEvent(player, positions, newScore, queue);
    }

    public static Event cardsMoved(int[] fromPositions, int[] toPositions) {
        return new CardsMovedEvent(fromPositions, toPositions);
    }

    public static Event cardsDrawn(DrawReason reason, int nbCardsBeforeDraw, Card[] cards, int[] positions) {
        return new CardsDrawnEvent(reason, nbCardsBeforeDraw, cards, positions);
    }

}
