package com.orange.oswe.demo.trio.game.model;

import com.orange.oswe.demo.trio.domain.User;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Created by crhx7117 on 20/06/17.
 */
@Value
@EqualsAndHashCode(of="id")
public class Player {
    private final String id;
    private final String name;

    public Player(User user) {
        this.id = user.getUsername();
        this.name = user.getFullname();
    }
}
