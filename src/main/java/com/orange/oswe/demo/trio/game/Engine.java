/*
 * Created on 11 janv. 2005
 */
package com.orange.oswe.demo.trio.game;

import com.orange.oswe.demo.trio.domain.User;
import com.orange.oswe.demo.trio.game.actions.Action;
import com.orange.oswe.demo.trio.game.actions.ActionException;
import com.orange.oswe.demo.trio.game.actions.Forbidden;
import com.orange.oswe.demo.trio.game.actions.IllegalGameState;
import com.orange.oswe.demo.trio.game.events.Event;
import com.orange.oswe.demo.trio.game.model.Card;
import com.orange.oswe.demo.trio.game.model.Game;
import com.orange.oswe.demo.trio.game.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

/**
 * @author PiPo
 */
public class Engine {
    public static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

    private final Shuffler shuffler;
    private final SimpMessagingTemplate messagingTemplate;
    private final Game game;
    private final Timer timer = new Timer();
    private final long inactivityTimeoutDelay;
    private final GameLifecycleListener gameLifecycleListener;
    private SelectionTimeout selectionTimer;
    private InactivityTimeout inactivityTimeout;
    private boolean trioFoundInQueue = false;
    private Queue<Card> deck;

    public Engine(String id, User creator, Shuffler shuffler, SimpMessagingTemplate messagingTemplate, long inactivityTimeout, GameLifecycleListener gameLifecycleListener) {
        this.shuffler = shuffler;
        this.messagingTemplate = messagingTemplate;
        game = new Game(id, creator.getUsername());
        game.add(new Player(creator));
        this.inactivityTimeoutDelay = inactivityTimeout;
        this.gameLifecycleListener = gameLifecycleListener;
        rearmInactivityTimeout();
    }

    public Game getGame() {
        return game;
    }

    private void broadcast(Event event) {
        LOGGER.info(">>> {}", event);
        messagingTemplate.convertAndSend("/down/games/" + game.getId(), event);
    }

    // =====================================================
    // === Connection Methods
    // =====================================================
    private void playerJoins(Player player) throws IllegalGameState {
        LOGGER.info("User {} joins", player);
        if (game.getState() != Game.State.preparing) {
            LOGGER.error("User {} tries to join during the game. Reject.", player);
            throw new IllegalGameState("You cannot join a game that has already started.");
        }
        if (game.getPlayers().containsKey(player.getId())) {
            LOGGER.warn("User {} tries to join a game he is already part of. Ignore.", player);
            return;
        }
        game.add(player);
        // --- broadcast "node joins" message to all participating nodes
        broadcast(Event.playerJoined(player));
    }

    private void playerLeaves(Player player) throws IllegalGameState {
        LOGGER.info("User {} leaves", player);
        if (!game.getPlayers().containsKey(player.getId())) {
            LOGGER.warn("User {} tries to quit a game he is not part of. Ignore.", player);
            return;
        }
        // --- change player state
        game.remove(player);

        // --- broadcast event
        broadcast(Event.playerLeft(player));
    }

    // =====================================================
    // === Game Methods
    // =====================================================
    private void playerDeclaresTrio(Player player) throws ActionException {
        checkPlaying(player);
        checkPlayer(player);

        synchronized (game.getQueue()) {
            if (game.getQueue().contains(player.getId())) {
                LOGGER.error("User {} declares a trio, but was already in the selection queue. Reject.", player);
                throw new IllegalGameState("You already declared a trio.");
            }
            // extra user to selection queue
            game.getQueue().add(player.getId());
            if (game.getQueue().size() == 1) {
                advanceSelectionQueue();
            } else {
                broadcast(Event.playerDeclaresTrio(player, game.getQueue()));
            }
        }
    }

    private void checkOwner(Player player) throws Forbidden {
        if (!game.getOwnerId().equals(player.getId())) {
            LOGGER.error("Non owner {} tries to change game state. Reject.", player);
            throw new Forbidden("You are not allowed to change this game state.");
        }
    }

    private void checkPlayer(Player player) throws Forbidden {
        if (!game.getPlayers().containsKey(player.getId())) {
            LOGGER.error("Non player user {} sent game actions. Reject.", player);
            throw new Forbidden("You are not part of this game.");
        }
    }

    private void checkPlaying(Player player) throws IllegalGameState {
        if (game.getState() != Game.State.playing) {
            LOGGER.error("User {} sent a game event, but the game is not started. Reject.", player);
            throw new IllegalGameState("The game is not started.");
        }
    }

    private void playerTimeoutsTrioSelection(Player player) throws ActionException {
        checkPlaying(player);
        checkPlayer(player);
        synchronized (game.getQueue()) {
            if (game.getQueue().isEmpty()) {
                LOGGER.error("playerTimeoutsTrioSelection({}): queue is empty.", player);
                throw new IllegalGameState("Selection queue is empty.");
            }
            if (!game.getQueue().peek().equals(player.getId())) {
                LOGGER.error("playerTimeoutsTrioSelection({}): not queue player.", player);
                throw new IllegalGameState("You're not the queue player.");
            }

            // --- remove player from selection queue
            game.getQueue().remove();
            // --- stop backup timeout
            if (selectionTimer == null) {
                LOGGER.warn("playerTimeoutsTrioSelection({}): timeout not set", player);
            } else {
                selectionTimer.cancel();
                selectionTimer = null;
            }

            // --- update player score and broadcast event
            broadcast(Event.trioSelectionTimeouted(player, game.incrScore(player.getId(), -1), game.getQueue()));

            // --- process selection queue
            advanceSelectionQueue();
        }
    }

    /*
     * @see trio.engine.interfaces.IGame#playerRenouncesToTrioSelection(int)
     */
    private void playerGaveUpTrioSelection(Player player) throws ActionException {
        checkPlaying(player);
        checkPlayer(player);
        synchronized (game.getQueue()) {
            if (game.getQueue().isEmpty()) {
                LOGGER.error("playerGaveUpTrioSelection({}): queue is empty.", player);
                throw new IllegalGameState("Selection queue is empty.");
            }
            if (!game.getQueue().peek().equals(player.getId())) {
                LOGGER.error("playerGaveUpTrioSelection({}): not queue player.", player);
                throw new IllegalGameState("You're not the queue player.");
            }

            // --- remove player from selection queue
            game.getQueue().poll();
            if (selectionTimer == null) {
                LOGGER.warn("playerGaveUpTrioSelection({}): timeout not set", player);
            } else {
                selectionTimer.cancel();
                selectionTimer = null;
            }

            // --- update player score and broadcast event
            broadcast(Event.trioSelectionGiveUp(player, game.incrScore(player.getId(), -1), game.getQueue()));

            // --- process selection queue
            advanceSelectionQueue();
        }
    }

    /*
     * A player may cancel when a player previously selected a trio in the same selection queue
     */
    private void playerWithdrawsTrioSelection(Player player) throws ActionException {
        checkPlaying(player);
        checkPlayer(player);

        synchronized (game.getQueue()) {
            if (game.getQueue().isEmpty()) {
                LOGGER.error("playerWithdrawsTrioSelection({}): queue is empty.", player);
                throw new IllegalGameState("Selection queue is empty.");
            }
            if (!game.getQueue().peek().equals(player.getId())) {
                LOGGER.error("playerWithdrawsTrioSelection({}): not queue player.", player);
                throw new IllegalGameState("You're not the queue player.");
            }

            // --- remove player from selection queue
            game.getQueue().poll();
            if (selectionTimer == null) {
                LOGGER.warn("playerWithdrawsTrioSelection({}): timeout not set", player);
            } else {
                selectionTimer.cancel();
                selectionTimer = null;
            }

            // --- broadcast event
            broadcast(Event.trioSelectionWithdraw(player, game.getQueue()));

            // --- process selection queue
            advanceSelectionQueue();
        }
    }

    /*
     * @see psc.apps.trio.interfaces.IGame#playerSelectsTrio(int, int[])
     */
    private void playerSelectsTrio(Player player, int[] cardPositions) throws ActionException {
        checkPlaying(player);
        checkPlayer(player);

        synchronized (game.getQueue()) {
            if (game.getQueue().isEmpty()) {
                LOGGER.error("playerSelectsTrio({}): queue is empty.", player);
                throw new IllegalGameState("Selection queue is empty.");
            }
            if (!game.getQueue().peek().equals(player.getId())) {
                LOGGER.error("playerSelectsTrio({}): not queue player.", player);
                throw new IllegalGameState("You're not the queue player.");
            }

            // --- remove player from selection queue
            game.getQueue().poll();
            // --- cancel timer
            if (selectionTimer == null) {
                LOGGER.warn("playerSelectsTrio({}): timeout not set", player);
            } else {
                selectionTimer.cancel();
                selectionTimer = null;
            }

            // --- check selection is a trio
            Card card1 = game.getBoard()[cardPositions[0]];
            Card card2 = game.getBoard()[cardPositions[1]];
            Card card3 = game.getBoard()[cardPositions[2]];
            if(card1 == null || card2 == null || card3 == null) {
                LOGGER.error("playerSelectsTrio({}): at least one position not occupied.", player);
                throw new IllegalGameState("You've selected a non occupied slot.");
            }
            List<Card.Attribute> faulty = Card.isTrio(card1, card2, card3);
            if (faulty.isEmpty()) {
                LOGGER.info("playerSelectsTrio({}): valid trio", player);
                // --- this is a trio: remove the cards and refill playing ground
                game.getBoard()[cardPositions[0]] = null;
                game.getBoard()[cardPositions[1]] = null;
                game.getBoard()[cardPositions[2]] = null;

                // --- update player score and broadcast event
                broadcast(Event.trioSelectionSuccess(player, cardPositions, game.incrScore(player.getId(), 3), game.getQueue()));
                trioFoundInQueue = true;
                // wait 1.5s for the trio found animation to occur before advancing the selection queue...
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // --- process selection queue
                        advanceSelectionQueue();
                    }
                }, 1600);
                return;
            } else {
                LOGGER.info("playerSelectsTrio({}): not a trio on attributes {}", player, faulty);
                // --- update player score and broadcast event
                broadcast(Event.trioSelectionFailure(player, faulty, game.incrScore(player.getId(), -1), game.getQueue()));
            }

            // --- process selection queue
            advanceSelectionQueue();
        }
    }

    private void advanceSelectionQueue() {
        // --- process next player in queue
        if (game.getQueue().isEmpty()) {
            // --- stack is now empty: reset and refill playground
            trioFoundInQueue = false;
            refillBoard();
        } else {
            String playerId = game.getQueue().peek();
            Player player = game.getPlayers().get(playerId);
            broadcast(Event.playerSelectsTrio(player, game.getQueue()));
            // --- start selection timeout
            selectionTimer = new SelectionTimeout(player);
            timer.schedule(selectionTimer, 5000);
        }
    }

    private void playerStartsRound(Player player) throws ActionException {
        checkOwner(player);

        if (game.getState() != Game.State.preparing) {
            LOGGER.error("Trying to start a non preparing game. Reject.", player);
            throw new IllegalGameState("This game cannot be started.");
        }
        // change state (start)
        game.start();
        broadcast(Event.gameStateChanged(Game.State.playing));

        // reset deck
        deck = shuffler.shuffle();
        trioFoundInQueue = false;

        // --- fill the board
        refillBoard();
    }

    private void playerPreparesNextRound(Player player) throws ActionException {
        checkOwner(player);
        if (game.getState() != Game.State.over) {
            LOGGER.error("Trying to restart a non finished game. Reject.", player);
            throw new IllegalGameState("This game cannot be restarted.");
        }
        game.next();
        broadcast(Event.gameStateChanged(Game.State.preparing));
    }

    /**
     * TODO: simplify?
     */
    private void refillBoard() {
        // --- count cards, find free slot and slots to reorganize
        int cardsOnBoard = 0;
        int nbReorg = 0;
        int[] posReorg = new int[3];
        int nbFree = 0;
        int[] posFree = new int[12];
        for (int i = 0; i < Game.FULL_BOARD_SIZE; i++) {
            if (game.getBoard()[i] == null) {
                if (i < Game.NORMAL_BOARD_SIZE)
                    posFree[nbFree++] = i;
            } else {
                cardsOnBoard++;
                if (i >= Game.NORMAL_BOARD_SIZE)
                    posReorg[nbReorg++] = i;
            }
        }
        // --- reorganize cards
        if (nbReorg > 0 && nbFree > 0) {
            if (nbReorg < posReorg.length) {
                int[] newPosReorg = new int[nbReorg];
                System.arraycopy(posReorg, 0, newPosReorg, 0, nbReorg);
                posReorg = newPosReorg;
            }
            int[] posDest = new int[nbReorg];
            System.arraycopy(posFree, 0, posDest, 0, nbReorg);
            broadcast(Event.cardsMoved(posReorg, posDest));

            // --- move card on board
            for (int i = 0; i < nbReorg; i++) {
                game.getBoard()[posDest[i]] = game.getBoard()[posReorg[i]];
                game.getBoard()[posReorg[i]] = null;
            }
        }

        // --- adds cards to have 12
        if (cardsOnBoard < Game.NORMAL_BOARD_SIZE) {
            drawCards(Game.NORMAL_BOARD_SIZE - cardsOnBoard, Event.DrawReason.refill, -1);
        }

        // --- is there a trio?
        List<int[]> trios = findTrios(false);
        if (trios.isEmpty()) {
            if (!game.hasCardsLeft()) {
                LOGGER.info("no trio on board and no more cards: end of game");
                triggerEndOfRound();
                return;
            }
            // --- draw 3 extra cards
            LOGGER.info("no trio on board: draw 3 extra cards");
            drawCards(3, Event.DrawReason.extra, -1);

            // --- draw and replace cards until a Trio is found
            int replaceFromPos = 0;
            while (true) {
                trios = findTrios(false);
                if (!trios.isEmpty()) {
                    break;
                }
                if (!game.hasCardsLeft()) {
                    LOGGER.info("still no trio on board and no more cards: end of game");
                    triggerEndOfRound();
                    return;
                }
                LOGGER.info("still no trio on board: replace 3 additional cards");
                drawCards(3, Event.DrawReason.replaced, replaceFromPos);
                replaceFromPos += 3;
            }
        }
    }

    private void triggerEndOfRound() {
        game.end();
        gameLifecycleListener.onEndOfRound(this);
        broadcast(Event.gameStateChanged(Game.State.over));
    }

    private List<int[]> findTrios(boolean findAll) {
        List<int[]> trios = new ArrayList<>();
        for (int i = 0; i < Game.FULL_BOARD_SIZE - 2; i++) {
            Card c1 = game.getBoard()[i];
            if (c1 == null)
                continue;
            // --- find trios with card i
            for (int j = i + 1; j < Game.FULL_BOARD_SIZE - 1; j++) {
                Card c2 = game.getBoard()[j];
                if (c2 == null)
                    continue;
                // --- find trios with card i+j
                for (int k = j + 1; k < Game.FULL_BOARD_SIZE; k++) {
                    Card c3 = game.getBoard()[k];
                    if (c3 == null)
                        continue;
                    // --- is i+j+k a trio?
                    if (Card.isTrio(c1, c2, c3).isEmpty()) {
                        trios.add(new int[]{i, j, k});
                        if (!findAll) {
                            return trios;
                        }
                    }
                }
            }
        }
        return trios;
    }

    /**
     * returns the number of drawn cards
     */
    // TODO: simplify?
    private int drawCards(int cardsToDraw, Event.DrawReason reason, int replaceFromPos) {
        if (cardsToDraw == 0 || deck.isEmpty())
            return 0;
        int cardsLeftBeforeDraw = game.getCardsLeft();
        Card[] drawnCards = null;
        int[] pos = new int[cardsToDraw];
        if (replaceFromPos < 0) {
            // --- find free positions
            List drawnCardsList = new ArrayList();
            int cardsDrawn = 0;
            for (int i = 0; i < Game.FULL_BOARD_SIZE; i++) {
                if (game.getBoard()[i] == null) {
                    // --- draw a card
                    game.decrCardsLeft(1);
                    drawnCardsList.add(game.getBoard()[i] = deck.remove());
                    pos[cardsDrawn] = i;
                    cardsDrawn++;
                    if (cardsDrawn == cardsToDraw || deck.isEmpty())
                        break;
                }
            }
            // --- fire draw cards event
            drawnCards = new Card[drawnCardsList.size()];
            drawnCards = (Card[]) drawnCardsList.toArray(drawnCards);
            if (drawnCards.length != cardsToDraw) {
                int[] newpos = new int[drawnCards.length];
                System.arraycopy(pos, 0, newpos, 0, drawnCards.length);
                pos = newpos;
            }
        } else {
            // --- replace positions
            drawnCards = new Card[cardsToDraw];
            for (int i = 0; i < cardsToDraw; i++) {
                game.decrCardsLeft(1);
                pos[i] = replaceFromPos;
                drawnCards[i] = game.getBoard()[replaceFromPos] = deck.remove();
                replaceFromPos++;
            }
        }

        broadcast(Event.cardsDrawn(reason, cardsLeftBeforeDraw, drawnCards, pos));
        return drawnCards.length;
    }

    public void handle(User user, Action action) throws ActionException {
        LOGGER.info("<<< from {}: {}", user.getUsername(), action);
        rearmInactivityTimeout();
        Player player = new Player(user);
        switch (action.getType()) {
            case restart_game:
                playerPreparesNextRound(player);
                break;
            case start_game:
                playerStartsRound(player);
                break;
            case player_join:
                playerJoins(player);
                break;
            case player_leave:
                playerLeaves(player);
                break;
            case declare_trio:
                playerDeclaresTrio(player);
                break;
            case select_trio:
                playerSelectsTrio(player, action.getSelection());
                break;
            case cancel_trio:
                if (trioFoundInQueue) {
                    playerWithdrawsTrioSelection(player);
                } else {
                    playerGaveUpTrioSelection(player);
                }
                break;
        }
    }

    // ==================================================================
    // === selection timeout
    // ==================================================================
    private class SelectionTimeout extends TimerTask {
        private Player player;

        public SelectionTimeout(Player player) {
            this.player = player;
        }

        public void run() {
            LOGGER.warn("Selection timeout reached for player {}.", player);
            try {
                playerTimeoutsTrioSelection(player);
            } catch (Exception e) {
                LOGGER.error("Error occurred while triggering selection timeout for {}", player, e);
            }
        }
    }

    // ==================================================================
    // === inactivity timeout
    // ==================================================================
    public interface GameLifecycleListener {
        void onInactivityTimeout(Engine engine);
        void onEndOfRound(Engine engine);
    }

    private void rearmInactivityTimeout() {
        if(inactivityTimeout != null) {
            inactivityTimeout.cancel();
            inactivityTimeout = null;
        }
        // in 10 min
        inactivityTimeout = new InactivityTimeout();
        timer.schedule(inactivityTimeout, inactivityTimeoutDelay);
    }

    private class InactivityTimeout extends TimerTask {
        public void run() {
            LOGGER.warn("Inactivity timeout reached for game {}.", game.getId());
            gameLifecycleListener.onInactivityTimeout(Engine.this);
        }
    }

}