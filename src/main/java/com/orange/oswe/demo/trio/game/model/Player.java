package com.orange.oswe.demo.trio.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.orange.oswe.demo.trio.domain.User;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Created by crhx7117 on 20/06/17.
 */
@Value
@EqualsAndHashCode(of="id")
public class Player {
    @JsonIgnore
    private final User user;

    public Player(User user) {
        this.user = user;
    }

    @JsonProperty
    public String getId() {
        return user.getUsername();
    }

    @JsonProperty
    public String getName() {
        return user.getFullname();
    }

}
