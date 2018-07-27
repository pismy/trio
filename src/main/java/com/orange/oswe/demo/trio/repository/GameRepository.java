package com.orange.oswe.demo.trio.repository;

import com.orange.oswe.demo.trio.domain.Result;
import com.orange.oswe.demo.trio.domain.Score;
import com.orange.oswe.demo.trio.domain.User;
import com.orange.oswe.demo.trio.game.Engine;
import com.orange.oswe.demo.trio.game.Shuffler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private ResultRepository resultRepository;

    private final Shuffler shuffler = new Shuffler();

    public Engine createNew(User user) {
        String id = Long.toHexString(System.nanoTime());
        Engine engine = new Engine(id, user, shuffler, messagingTemplate, inactivityTimeout, new EngineInactivityListener());
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

    private class EngineInactivityListener implements Engine.GameLifecycleListener {
        @Override
        public void onInactivityTimeout(Engine engine) {
            id2Game.remove(engine.getGame().getId());
        }

        @Override
        public void onEndOfRound(Engine engine) {
            com.orange.oswe.demo.trio.game.model.Game game = engine.getGame();
            Result resultDb = Result.build(Result.Id.build(game.getId(), game.getRound()), game.getOwner().getUser(), new Date(), null);
            Set<Score> scores = game.getPlayers().values().stream().map(p -> Score.build(null, resultDb, p.getUser(), game.getScore(p.getId()))).collect(Collectors.toSet());
            resultDb.setScores(scores);
            resultRepository.save(resultDb);
        }
    }
}
