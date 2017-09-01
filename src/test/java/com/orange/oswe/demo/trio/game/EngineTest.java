package com.orange.oswe.demo.trio.game;

import com.orange.oswe.demo.trio.domain.User;
import com.orange.oswe.demo.trio.game.actions.Action;
import com.orange.oswe.demo.trio.game.actions.ActionException;
import com.orange.oswe.demo.trio.game.events.Event;
import com.orange.oswe.demo.trio.game.model.Card;
import com.orange.oswe.demo.trio.game.model.Game;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class EngineTest {
    public static final String ID = "game";
    public static final User CREATOR = new User("creator", "fullname", "password");
    @Mock
    Shuffler shuffler;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    Timer timer;

    @Mock
    Engine.InactivityTimeoutListener inactivityTimeoutListener;

    Engine engine;

    @Before
    public void setup() {
        engine = new Engine(ID, CREATOR, shuffler, messagingTemplate, 2000, inactivityTimeoutListener);
    }

    @Test
    public void first_draw_with_trio_should_draw_12_cards() throws ActionException {
        // GIVEN
        Mockito.when(shuffler.shuffle()).thenReturn(deckWithTrioInFirst12());

        // WHEN
        engine.handle(CREATOR, new Action(Action.Type.start_game));

        // THEN
        InOrder inOrder = Mockito.inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSend("/down/games/" + ID, Event.gameStateChanged(Game.State.playing));
        inOrder.verify(messagingTemplate).convertAndSend("/down/games/" + ID, Event.cardsDrawn(Event.DrawReason.refill, 81, Mockito.any(), Mockito.any()));
    }

    @Test
    public void deck_with_trio_at_19_should_draw_21_cards() throws ActionException {
        // GIVEN
        Mockito.when(shuffler.shuffle()).thenReturn(deckWithFirstTrioAt19());

        // WHEN
        engine.handle(CREATOR, new Action(Action.Type.start_game));

        // THEN
        InOrder inOrder = Mockito.inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSend("/down/games/" + ID, Event.gameStateChanged(Game.State.playing));
        ArgumentCaptor<Event.CardsDrawnEvent> drawEventCaptor = ArgumentCaptor.forClass(Event.CardsDrawnEvent.class);
        inOrder.verify(messagingTemplate, Mockito.times(4)).convertAndSend(Matchers.eq("/down/games/" + ID), drawEventCaptor.capture());
        Assert.assertEquals(Event.DrawReason.refill, drawEventCaptor.getAllValues().get(0).getReason());
        Assert.assertEquals(Event.DrawReason.extra, drawEventCaptor.getAllValues().get(1).getReason());
        Assert.assertEquals(Event.DrawReason.replaced, drawEventCaptor.getAllValues().get(2).getReason());
        Assert.assertEquals(Event.DrawReason.replaced, drawEventCaptor.getAllValues().get(3).getReason());
    }

    private Queue<Card> deckWithTrioInFirst12() {
        return new ArrayDeque<>(Arrays.asList(
                // 0-12
                new Card(21),
                new Card(160),
                new Card(66),
                new Card(82),
                new Card(138),
                new Card(32),
                new Card(10),
                new Card(22),
                new Card(73),
                new Card(9),
                new Card(145),
                new Card(69),
                // 13-15
                new Card(68), new Card(41), new Card(148),
                // 16-18
                new Card(132), new Card(137), new Card(70),
                // 19-21
                new Card(25), new Card(153), new Card(90)
        ));
    }
    private Queue<Card> deckWithFirstTrioAt19() {
        return new ArrayDeque<>(Arrays.asList(
                // 0-12
                new Card(73),
                new Card(16),
                new Card(152),
                new Card(37),
                new Card(21),
                new Card(22),
                new Card(38),
                new Card(8),
                new Card(68),
                new Card(146),
                new Card(148),
                new Card(134),
                // 13-15
                new Card(137),
                new Card(132),
                new Card(136),
                // 16-18
                new Card(154),
                new Card(106),
                new Card(89),
                // 19-21
                new Card(25), new Card(153), new Card(90)
        ));
    }
}