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
import com.orange.oswe.demo.trio.game.model.Game;
import com.orange.oswe.demo.trio.repository.GameRepository;
import com.orange.oswe.demo.trio.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
public class HomeController {
	
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

	@Autowired
	private GameRepository gameRepository;

	@RequestMapping("/")
	public ModelAndView home(Authentication authentication, Locale userLocale) {
		ModelAndView modelAndView = new ModelAndView("home");
		String meId = authentication != null && authentication.isAuthenticated() ? authentication.getName() : null;
		if (meId != null) {
			Optional<Game> mygame = StreamSupport.stream(gameRepository.findAll().spliterator(), false).map(Engine::getGame).filter(g -> g.getOwnerId() == meId).findFirst();
			if(mygame.isPresent()) {
				modelAndView.addObject("mygame", mygame.get());
			}
			List<Game> gamesiamin = StreamSupport.stream(gameRepository.findAll().spliterator(), false).map(Engine::getGame).filter(g -> g.getOwnerId() != meId && g.getPlayers().get(meId) != null).collect(Collectors.toList());
			modelAndView.addObject("gamesiamin", gamesiamin);
		}
		modelAndView.addObject("ui", new UiTool(TimeZone.getDefault(), userLocale));
		List<Game> othergames = StreamSupport.stream(gameRepository.findAll().spliterator(), false).map(Engine::getGame).filter(g -> meId == null || g.getOwnerId() != meId || g.getPlayers().get(meId) == null).collect(Collectors.toList());
		modelAndView.addObject("othergames", othergames);
		return modelAndView;
	}

	@RequestMapping("/rules")
	public ModelAndView rules() {
		return new ModelAndView("rules");
	}
}
