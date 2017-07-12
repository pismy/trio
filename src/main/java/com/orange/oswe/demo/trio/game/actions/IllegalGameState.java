package com.orange.oswe.demo.trio.game.actions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by crhx7117 on 27/06/17.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IllegalGameState extends ActionException {
    public IllegalGameState(String message) {
        super(message);
    }
}
