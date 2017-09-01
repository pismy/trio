package com.orange.oswe.demo.trio.game;

import com.orange.oswe.demo.trio.game.model.Card;

import java.util.*;

public class Shuffler {
    public Queue<Card> shuffle() {
        // --- build unshuffled deck
        List<Card> unshuffled = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    for (int l = 0; l < 3; l++) {
                        unshuffled.add(new Card(i, j, k, l));
                    }
                }
            }
        }

        // --- shuffle
        Queue<Card> deck = new ArrayDeque<>();
        Random random = new Random();
        while(!unshuffled.isEmpty()) {
            int idx = random.nextInt(unshuffled.size());
            deck.add(unshuffled.remove(idx));
        }
        return deck;
    }
}
