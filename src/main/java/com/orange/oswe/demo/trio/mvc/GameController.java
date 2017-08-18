/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.mvc;

import com.orange.oswe.demo.trio.domain.User;
import com.orange.oswe.demo.trio.game.Engine;
import com.orange.oswe.demo.trio.game.actions.Action;
import com.orange.oswe.demo.trio.game.actions.ActionException;
import com.orange.oswe.demo.trio.game.actions.GameNotFound;
import com.orange.oswe.demo.trio.game.actions.Unauthorized;
import com.orange.oswe.demo.trio.game.model.Game;
import com.orange.oswe.demo.trio.repository.GameRepository;
import com.orange.oswe.demo.trio.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@RequestMapping("/games")
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private CurrentUserService currentUser;

    // ================================================================================================================
    // === HTML pages
    // ================================================================================================================
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_XHTML_XML_VALUE})
    public ModelAndView viewGame(Authentication authentication, @PathVariable("id") String id) throws GameNotFound {
        logger.debug("(HTML) view game {}", id);
        Optional<Engine> engine = gameRepository.findById(id);
        if (!engine.isPresent()) {
            throw new GameNotFound("Game " + id + " not found");
        }
        ModelAndView modelAndView = new ModelAndView("game");
        modelAndView.addObject("gameId", id);

        // me
        if (authentication != null && authentication.isAuthenticated()) {
            String meId = authentication.getName();
            modelAndView.addObject("userId", meId);
        }

        return modelAndView;
    }

    @RequestMapping(method = RequestMethod.POST, produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_XHTML_XML_VALUE})
    public ModelAndView createGameAndRedirect(Authentication authentication) throws Unauthorized {
        logger.debug("(HTML) create new game");
        Game game = createGame(authentication);
        return new ModelAndView("redirect:/games/" + game.getId());
    }

    @RequestMapping(value = "/{id}/players", method = RequestMethod.POST, produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_XHTML_XML_VALUE})
    public ModelAndView joinGameAndRedirect(Authentication authentication, @PathVariable("id") String id) throws ActionException {
        logger.debug("(HTML) join game");
        handleGameAction(authentication, id, new Action(Action.Type.player_join));
        return new ModelAndView("redirect:/games/" + id);
    }

    // ================================================================================================================
    // === JSON REST endpoints
    // ================================================================================================================
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Game> getAllGames() {
        logger.debug("(API) get all games");
        return StreamSupport.stream(gameRepository.findAll().spliterator(), false).map(Engine::getGame).collect(Collectors.toList());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Game getGame(@PathVariable("id") String id) throws GameNotFound {
        logger.debug("(API) get game {}", id);
        Optional<Engine> engine = gameRepository.findById(id);
        if (!engine.isPresent()) {
            throw new GameNotFound("Game " + id + " not found");
        } else {
            return engine.get().getGame();
        }
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public Game createGame(Authentication authentication) throws Unauthorized {
        logger.debug("(API) create new game");
        if (authentication != null && authentication.isAuthenticated()) {
            User me = currentUser.getCurrentUser();
            return gameRepository.createNew(me).getGame();
        } else {
            // unauthorized
            throw new Unauthorized("You must be authenticated to create a game");
        }
    }

    @RequestMapping(value = "/{id}/actions", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void handleGameAction(Authentication authentication, @PathVariable("id") String id, @RequestBody Action action) throws ActionException {
        if (authentication != null && authentication.isAuthenticated()) {
            User me = currentUser.getCurrentUser();
            logger.info("handleGameAction {} from {}", action, me);
            Optional<Engine> engine = gameRepository.findById(id);
            if (!engine.isPresent()) {
                throw new GameNotFound("Game " + id + " not found");
            } else {
                engine.get().handle(me, action);
            }
        } else {
            // unauthorized
            throw new Unauthorized("You must be authenticated to send game actions");
        }
    }

    @ExceptionHandler(GameNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ModelAndView handleGameNotFound() {
        return new ModelAndView("game_not_found");
    }

}
