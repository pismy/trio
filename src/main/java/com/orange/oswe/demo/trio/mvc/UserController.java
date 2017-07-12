/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.mvc;

import com.orange.oswe.demo.trio.domain.User;
import com.orange.oswe.demo.trio.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
 @RequestMapping(value = "/users")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private UserRepository userRepository;

	@RequestMapping(value = "", method = RequestMethod.GET)
	public ModelAndView allUsers(Authentication authentication, @RequestParam(name="dir", defaultValue="ASC") Direction direction) {
		ModelAndView modelAndView = new ModelAndView("users");
		Iterable<User> users = userRepository.findAll();
		modelAndView.addObject("users", users);
		modelAndView.addObject("direction", direction);
		
		// me
		if(authentication != null && authentication.isAuthenticated()) {
			String meId = authentication.getName();
			User me = userRepository.findByUsername(meId);
			modelAndView.addObject("me", me);
		}

		return modelAndView;
	}


	@RequestMapping(value = "/{username}", method = RequestMethod.GET)
	public ModelAndView user(Authentication authentication, @PathVariable("username") String username, @RequestParam(name="pageNumber", required=false, defaultValue="0") int pageNumber) {
		logger.debug("user {}", username);
		ModelAndView modelAndView = new ModelAndView("user");
		User user = userRepository.findByUsername(username);
		modelAndView.addObject("user", user);

		// me
		if(authentication != null && authentication.isAuthenticated()) {
			String meId = authentication.getName();
			User me = userRepository.findByUsername(meId);
			modelAndView.addObject("me", me);
		}
		return modelAndView;
	}
}
