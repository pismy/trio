/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.mvc.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Generic error controller handling Rest errors
 * <p>
 * Renders any {@link Exception} into a readable JSON body
 */
@Controller
@ControllerAdvice
public class GlobalErrorHandler extends AbstractGlobalErrorHandler {

	@Override
	protected ModelAndView doRender(HttpServletRequest request, HttpServletResponse response, ErrorDetails details) {
		MediaType type = findFirstAccept(request, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML);
		if(MediaType.APPLICATION_JSON.equals(type)) {
			// render as JSON
			ModelAndView jsonView = new ModelAndView(new MappingJackson2JsonView());
			jsonView.addObject("code", details.getCode().getCode());
			jsonView.addObject("message", details.getCode().name());
			jsonView.addObject("description", details.getDescription());
			return jsonView;
		} else {
			// render as HTML page
			// retrieve error description from annotation
			String description = details.getCode().name();
			try {
				ErrorCode.Doc doc = ErrorCode.class.getField(details.getCode().name()).getAnnotation(ErrorCode.Doc.class);
				if (doc != null) {
					description = doc.value();
				}
			} catch (NoSuchFieldException | SecurityException e) {
				logger.warn("Parsing error while parsing exception", e);
			}

			HttpStatus httpStatus = details.getCode().getStatus();
			response.setStatus(httpStatus.value());

			ModelAndView htmlView = new ModelAndView("error");
			htmlView.addObject("code", details.getCode());
			htmlView.addObject("niceHttpStatus", cc(httpStatus.name()));
			htmlView.addObject("description", description);

			return htmlView;
		}
	}

	/**
	 * Turns an uppercase string with underscore separators into Camel Case
	 */
	private static String cc(String name) {
		return Arrays.stream(name.split("_")).map(GlobalErrorHandler::capitalize).collect(Collectors.joining(" "));
	}

	private static String capitalize(String word) {
		if(word == null || word.isEmpty()) {
			return word;
		}
		if(word.length() == 1) {
			return word.toUpperCase();
		}
		return Character.toUpperCase(word.charAt(0)) + (word.substring(1).toLowerCase());
	}
}
