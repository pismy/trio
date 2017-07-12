package com.orange.oswe.demo.trio.game.actions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by crhx7117 on 27/06/17.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class GameNotFound extends ActionException {
    public GameNotFound(String message) {
        super(message);
    }
}
