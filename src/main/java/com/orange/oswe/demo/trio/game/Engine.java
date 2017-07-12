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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author PiPo
 */
public class Engine {
    public static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final Game game;
    private boolean trioFoundInQueue = false;

    public Engine(String id, User creator, SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        game = new Game(id, creator.getUsername());
        game.addPlayer(new Player(creator));
    }

    public Game getGame() {
        return game;
    }

    private void broadcast(Event event) {
        messagingTemplate.convertAndSend("/down/games/" + game.getId(), event);
    }

    // =====================================================
    // === Game Data
    // =====================================================
    private Card[] deck;
    private Timer selectionTimer;

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
            LOGGER.error("User {} tries to join a game he is already part of. Reject.", player);
            throw new IllegalGameState("You are already part of this game.");
        }
        game.addPlayer(player);
        // --- broadcast "node joins" message to all participating nodes
        broadcast(Event.playerJoined(player));
    }

    private void playerLeaves(Player player) throws IllegalGameState {
        LOGGER.info("User {} leaves", player);
        if (!game.getPlayers().containsKey(player.getId())) {
            LOGGER.error("User {} tries to quit a game he is not part of. Reject.", player);
            throw new IllegalGameState("You are not part of this game.");
        }
        // --- change player state
        game.getPlayers().remove(player);

        // --- broadcast event
        broadcast(Event.playerLeft(player));
    }

    // =====================================================
    // === General Methods
    // =====================================================
    // =====================================================
    // === Game Methods
    // =====================================================
    /*
	 * @see psc.apps.trio.interfaces.IGame#playerAnnoucesTrio(int)
	 */
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
                broadcast(Event.playerDeclaresTrio(player, null, game.getQueue()));
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
                selectionTimer.stop();
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
                selectionTimer.stop();
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
                selectionTimer.stop();
                selectionTimer = null;
            }

            // --- broadcast event
            broadcast(Event.trioSelectionWithdraw(player, null, game.getQueue()));

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
                selectionTimer.stop();
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
            Card.Attribute wrongAttribute = Card.isTrio(card1, card2, card3);
            if (wrongAttribute == null) {
                LOGGER.info("playerSelectsTrio({}): valid trio", player);
                // --- this is a trio: remove the cards and refill playing ground
                game.getBoard()[cardPositions[0]] = null;
                game.getBoard()[cardPositions[1]] = null;
                game.getBoard()[cardPositions[2]] = null;

                // --- update player score and broadcast event
                broadcast(Event.trioSelectionSuccess(player, cardPositions, game.incrScore(player.getId(), 3), game.getQueue()));
                trioFoundInQueue = true;
            } else {
                LOGGER.info("playerSelectsTrio({}): not a trio on attribute {}", player, wrongAttribute);
                // --- update player score and broadcast event
                broadcast(Event.trioSelectionFailure(player, game.incrScore(player.getId(), -1), game.getQueue()));
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
            broadcast(Event.playerSelectsTrio(player, null, game.getQueue()));
            // --- start selection timeout
            selectionTimer = new Timer(player, 5);
            new Thread(selectionTimer).start();
        }
    }

    private void playerStartsGame(Player player) throws ActionException {
        checkOwner(player);

        if (game.getState() != Game.State.preparing) {
            LOGGER.error("Trying to start a non preparing game. Reject.", player);
            throw new IllegalGameState("This game cannot be started.");
        }
        // change state
        game.setState(Game.State.playing);
        broadcast(Event.gameStateChanged(Game.State.playing));

        // --- build deck
        game.setCardsLeft(81);
        deck = new Card[81];
        int n = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    for (int l = 0; l < 3; l++) {
                        deck[n++] = new Card(i, j, k, l);
                    }
                }
            }
        }

        // --- shuffle
        for (int i = 0; i < deck.length; i++) {
            int offset = (int) (Math.random() * deck.length);
            int i2 = (i + offset) % deck.length;
            Card c = deck[i];
            deck[i] = deck[i2];
            deck[i2] = c;
        }

        game.reset();
        game.setState(Game.State.playing);
        trioFoundInQueue = false;

        // --- fill the board
        refillBoard();
    }

    private void playerEndsGame(Player player) throws ActionException {
        checkOwner(player);

        if (game.getState() != Game.State.playing) {
            LOGGER.error("Trying to end a non playing game. Reject.", player);
            throw new IllegalGameState("This game cannot be ended.");
        }
        if (!game.isOver()) {
            LOGGER.error("Trying to finish a game while there are cards left. Reject.");
            throw new IllegalGameState("You cannot finish a game while there are cards left.");
        }

        game.setState(Game.State.finished);
        broadcast(Event.gameStateChanged(Game.State.finished));
    }

    private void playerPreparesGame(Player player) throws ActionException {
        checkOwner(player);

        if (game.getState() != Game.State.finished) {
            LOGGER.error("Trying to prepare a non finished game. Reject.", player);
            throw new IllegalGameState("This game cannot be prepared.");
        }
        game.reset();

        game.setState(Game.State.preparing);
        broadcast(Event.gameStateChanged(Game.State.preparing));
    }

    private void refillBoard() {
        // --- count cards
        int nbCards = 0;
        int nbReorg = 0;
        int[] posReorg = new int[3];
        int nbFree = 0;
        int[] posFree = new int[12];
        for (int i = 0; i < game.getBoard().length; i++) {
            if (game.getBoard()[i] == null) {
                if (i < game.getBoard().length - 3)
                    posFree[nbFree++] = i;
            } else {
                nbCards++;
                if (i >= game.getBoard().length - 3)
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
        if (nbCards < 12) {
            drawCards(12 - nbCards, Event.DrawReason.refill, -1);
        }

        // --- is there a trio?
        List<int[]> trios = findTrios(false);
        if (trios.isEmpty()) {
            if (game.isOver()) {
                // --- this is the end of the game
                game.setState(Game.State.over);
                broadcast(Event.gameStateChanged(Game.State.over));
                return;
            }
            // --- draw 3 extra cards
            LOGGER.info("GameEngine: no trio possible. Draw 3 more cards.");
            drawCards(3, Event.DrawReason.extra, -1);

            // --- draw and replace cards until a Trio is found
            int replaceFromPos = 0;
            while (true) {
                trios = findTrios(false);
                if (!trios.isEmpty()) {
                    break;
                }
                if (game.isOver()) {
                    // --- this is the end of the game
                    game.setState(Game.State.over);
                    broadcast(Event.gameStateChanged(Game.State.over));
                    return;
                }
                drawCards(3, Event.DrawReason.replaced, replaceFromPos);
                replaceFromPos += 3;
            }
        }
    }

    private List<int[]> findTrios(boolean findAll) {
        List<int[]> trios = new ArrayList<>();
        for (int i = 0; i < game.getBoard().length - 2; i++) {
            Card c1 = game.getBoard()[i];
            if (c1 == null)
                continue;
            // --- find trios with card i
            for (int j = i + 1; j < game.getBoard().length - 1; j++) {
                Card c2 = game.getBoard()[j];
                if (c2 == null)
                    continue;
                // --- find trios with card i+j
                for (int k = j + 1; k < game.getBoard().length; k++) {
                    Card c3 = game.getBoard()[k];
                    if (c3 == null)
                        continue;
                    // --- is i+j+k a trio?
                    if (Card.isTrio(c1, c2, c3) == null) {
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

    // --- returns the number of drawn cards
    private int drawCards(int iCards, Event.DrawReason reason, int iReplaceFromPos) {
        if (iCards == 0 || game.getCardsLeft() == 0)
            return 0;
        int nbCardsBeforeDraw = game.getCardsLeft();
        Card[] cards = null;
        int[] pos = new int[iCards];
        if (iReplaceFromPos < 0) {
            // --- find free positions
            Vector cardsList = new Vector();
            int nb = 0;
            for (int i = 0; i < game.getBoard().length; i++) {
                if (game.getBoard()[i] == null) {
                    // --- draw a card
                    game.decrCardsLeft(1);
                    game.getBoard()[i] = deck[game.getCardsLeft()];
                    cardsList.addElement(deck[game.getCardsLeft()]);
                    pos[nb] = i;
                    nb++;
                    if (nb == iCards || game.getCardsLeft() == 0)
                        break;
                }
            }
            // --- fire draw cards event
            cards = new Card[cardsList.size()];
            cardsList.copyInto(cards);
            if (cards.length != iCards) {
                int[] newpos = new int[cards.length];
                System.arraycopy(pos, 0, newpos, 0, cards.length);
                pos = newpos;
            }
        } else {
            // --- replace positions
            cards = new Card[iCards];
            for (int i = 0; i < iCards; i++) {
                game.decrCardsLeft(1);
                pos[i] = iReplaceFromPos;
                game.getBoard()[iReplaceFromPos] = deck[game.getCardsLeft()];
                cards[i] = deck[game.getCardsLeft()];
                iReplaceFromPos++;
            }
        }

        broadcast(Event.cardsDrawn(reason, nbCardsBeforeDraw, cards, pos));
        return cards.length;
    }

    public void handle(User user, Action action) throws ActionException {
        Player player = new Player(user);
        switch (action.getType()) {
            case start_game:
                playerStartsGame(player);
                break;
            case finish_game:
                playerEndsGame(player);
                break;
            case prepare_game:
                playerPreparesGame(player);
                break;
            case player_join:
                playerJoins(player);
                break;
            case player_leave:
                playerJoins(player);
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
    // === CountDown
    // ==================================================================
    private class Timer implements Runnable {
        private boolean stopped = false;
        private int timeout;
        private Player player;

        public Timer(Player player, int timeout) {
            this.player = player;
            this.timeout = timeout;
        }

        public void run() {
            synchronized (this) {
                try {
                    this.wait(timeout * 1000);
                } catch (Exception e) {
                }
            }
            if (!stopped) {
                // --- timeout reached
                LOGGER.warn("Timer: backup timeout reached for player {}.", player);
                try {
                    playerTimeoutsTrioSelection(player);
                } catch (Exception e) {
                    LOGGER.error("Error occurred while triggering selection timeout for {}", player, e);
                }
            }
        }

        public void stop() {
            stopped = true;
            synchronized (this) {
                try {
                    this.notifyAll();
                } catch (Exception e) {
                }
            }
        }
    }
}