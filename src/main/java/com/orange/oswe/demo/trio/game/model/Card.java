package com.orange.oswe.demo.trio.game.model;

import com.google.common.base.Preconditions;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by crhx7117 on 20/06/17.
 */
@Value
public class Card {

    public enum Attribute {
        color, shape, fill, number
    }

    public Card(int value) {
        this.value = value;
    }

    public Card(int color, int shape, int fill, int number) {
        check(Attribute.color, color);
        check(Attribute.shape, shape);
        check(Attribute.fill, fill);
        check(Attribute.number, number);
        value = (number << 6) + (fill << 4) + (shape << 2) + color;
    }

    private int value;

    private static int check(Attribute attr, int val) {
        Preconditions.checkArgument(val >= 0 && val <= 2, "Attribute '"+attr+"' out of range [0-2] ("+val+")");
        return val;
    }

    public int getAttribute(Attribute attribute) {
        return (value >> (attribute.ordinal() * 2)) & 0x03;
    }
    /**
     * Determines whether the 3 given cards are a Trio
     * @return an empty list if they are a trio; list of faulty attributes if not
     */
    public static List<Attribute> isTrio(Card card1, Card card2, Card card3) {
        List<Attribute> faulty = new ArrayList<>();
        for (Card.Attribute attribute : Card.Attribute.values()) {
            if (card1.getAttribute(attribute) == card2.getAttribute(attribute)) {
                // --- check they are all equal
                if (card1.getAttribute(attribute) != card3.getAttribute(attribute)) {
                    // --- not a trio
                    faulty.add(attribute);
                }
            } else {
                // --- check they are all different
                if (card1.getAttribute(attribute) == card3.getAttribute(attribute)) {
                    // --- not a trio
                    faulty.add(attribute);
                } else if (card2.getAttribute(attribute) == card3.getAttribute(attribute)) {
                    // --- not a trio
                    faulty.add(attribute);
                }
            }
        }
        return faulty;
    }

}
