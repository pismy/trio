package com.orange.oswe.demo.trio.game.actions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by crhx7117 on 22/06/17.
 */
@Data
@NoArgsConstructor
public class Action {
    public enum Type {
        start_game, restart_game, declare_trio, select_trio, player_join, player_leave, cancel_trio
    }
    Type type;
    int[] selection;


    public Action(Type type) {
        this.type = type;
    }
}
