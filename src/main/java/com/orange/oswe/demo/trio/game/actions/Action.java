package com.orange.oswe.demo.trio.game.actions;

import lombok.Data;

import java.util.List;

/**
 * Created by crhx7117 on 22/06/17.
 */
@Data
public class Action {
    public enum Type {
        start_game, finish_game, prepare_game, declare_trio, select_trio, player_join, player_leave, cancel_trio
    }
    Type type;
    int[] selection;
}
