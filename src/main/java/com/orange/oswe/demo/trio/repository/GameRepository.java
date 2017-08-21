package com.orange.oswe.demo.trio.repository;

import com.orange.oswe.demo.trio.domain.User;
import com.orange.oswe.demo.trio.game.Engine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by crhx7117 on 22/06/17.
 */
@Repository
public class GameRepository {
    private final Map<String, Engine> id2Game = new HashMap<>();

    @Value("${trio.game.inactivity_timeout}")
    private long inactivityTimeout;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Engine createNew(User user) {
        String id = UUID.randomUUID().toString();
        Engine engine = new Engine(id, user, messagingTemplate, inactivityTimeout, new EngineInactivityListener());
        id2Game.put(id, engine);
        return engine;
    }

    public Iterable<Engine> findAll() {
        return id2Game.values();
    }

    public Optional<Engine> findById(String id) {
        return Optional.ofNullable(id2Game.get(id));
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    private class EngineInactivityListener implements Engine.InactivityTimeoutListener {
        @Override
        public void onInactivityTimeout(Engine engine) {
            id2Game.remove(engine.getGame().getId());
        }
    }



}
