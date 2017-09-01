package com.orange.oswe.demo.trio.game.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by crhx7117 on 26/06/17.
 */
public class CardTest {
    @Test
    public void card_creator() {
        Card card = new Card(0, 1, 2, 0);
        assertThat(card.getAttribute(Card.Attribute.color)).isEqualTo(0);
        assertThat(card.getAttribute(Card.Attribute.shape)).isEqualTo(1);
        assertThat(card.getAttribute(Card.Attribute.fill)).isEqualTo(2);
        assertThat(card.getAttribute(Card.Attribute.number)).isEqualTo(0);
    }

    @Test
    public void check_not_a_trio() {
        Card card1 = new Card(0, 0, 1, 2);
        Card card2 = new Card(1, 0, 1, 2);
        Card card3 = new Card(2, 0, 1, 0);
        assertThat(Card.isTrio(card1, card2, card3)).containsExactlyInAnyOrder(Card.Attribute.number);
    }

    @Test
    public void check_trio() {
        Card card1 = new Card(0, 0, 1, 2);
        Card card2 = new Card(1, 0, 1, 1);
        Card card3 = new Card(2, 0, 1, 0);
        assertThat(Card.isTrio(card1, card2, card3)).isEmpty();
    }

}