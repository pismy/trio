/*
 * Copyright (C) 2017 Orange
 *
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE.txt' in this package distribution
 * or at 'http://www.apache.org/licenses/LICENSE-2.0'.
 */
package com.orange.oswe.demo.trio.mvc.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
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
@RequestMapping(HtmlAndRestErrorController.PATH)
@ControllerAdvice
public class HtmlAndRestErrorController implements ErrorController {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String ERROR_ATTRIBUTE = HtmlAndRestErrorController.class.getName()+".error";

	protected static final String PATH = "/error";

	@Override
	public String getErrorPath() {
		return PATH;
	}

	/**
	 * JSON error handling (default)
	 */
	@RequestMapping
	public ResponseEntity<JsonError[]> errorAsJson(HttpServletRequest request, HttpServletResponse response) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		JsonError error = buildError(request);
		handleInternalError(request, response, error);
		return new ResponseEntity<>(new JsonError[] { error }, headers, error.getErrorCode().getStatus());
	}

	/**
	 * Retrieves the current {@link Throwable error} being handled in the {@link HttpServletRequest request}
	 * @param request request
	 * @return error, if any
	 */
	protected Throwable getError(HttpServletRequest request) {
		// first try to get an exception handled by this
		Throwable throwable = (Throwable) request.getAttribute(ERROR_ATTRIBUTE);
		if(throwable == null) {
			// else try to retrieve exception handled by JEE
			throwable = (Throwable)request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		}
		return throwable;
	}

    /**
     * Builds a {@link JsonError} object representing the error being currently handled in the {@link HttpServletRequest request}
     * @param request request
     * @return error
     */
	protected JsonError buildError(HttpServletRequest request) {
		// either an exception or a state code
		Throwable throwable = getError(request);
		if (throwable != null) {
			return ErrorTranslator.build(throwable);
		}
		// no exception in the request: build JsonError from other JEE attributes (state, message, ...)
		Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		if (statusCode != null) {
			ErrorCode status = ErrorTranslator.getDefaultCode(HttpStatus.valueOf(statusCode));
			String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
			return new JsonError(status, message);
		} else {
			// no error ?!?
			return new JsonError(ErrorCode.Success, "Hey! looks like there is no error...");
		}
	}

	/**
	 * If the given error is internal ({@code 5XX}), then this method:
	 * <ol>
	 *     <li>generated a unique ID,</li>
	 *     <li>adds this ID as a response header,</li>
	 *     <li>replaces the {@link JsonError error} description with a generic message,</li>
	 *     <li>logs the Exception in ERROR with the generated ID (for traceability purpose).</li>
	 * </ol>
	 */
	protected void handleInternalError(HttpServletRequest request, HttpServletResponse response, JsonError error) {
		HttpStatus status = error.getErrorCode().getStatus();
		if(!status.is5xxServerError()) {
			return;
		}
		Throwable throwable = getError(request);
		if(throwable == null) {
			return;
		}

		// dump exception
		// compute request url
		String reqUrl = request.getRequestURI() + (request.getQueryString() == null ? "" : request.getQueryString());
		String fwdUri = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		String fwdQuery = (String) request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING);
		if (fwdUri != null) {
			// request was forwarded
			reqUrl = fwdUri + (fwdQuery == null ? "" : "?" + fwdQuery);
		}
		String errUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		if (errUri != null) {
			// JEE error
			reqUrl = errUri;
		}
		// do not return internal exception message (too technical)
		error.setDescription("Internal error occurred in request '" + request.getMethod() + " " + reqUrl + "'");

		// then log error with complete stack for analysis/debugging
		logger.error("Internal error occurred in request '{} {}'", request.getMethod(), reqUrl, throwable);
	}

	/*
	 * HandlerExceptionResolver impl: stores the exception in the request for later use, and forwards to this error controller
	 */
	@ExceptionHandler(Exception.class)
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Exception ex) {
		// stores the exception in the request attributes
		request.setAttribute(ERROR_ATTRIBUTE, ex);
		// forward to this
		return new ModelAndView("forward:"+PATH);
	}


	/**
	 * Html error handling
	 */
	@RequestMapping(produces = { MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_XHTML_XML_VALUE })
	public ModelAndView errorAsHtml(HttpServletRequest request, HttpServletResponse response) {
		JsonError error = buildError(request);
		handleInternalError(request, response, error);

		// retrieve error description from annotation
		String description = error.getErrorCode().name();
		try {
			ErrorCode.Doc doc = ErrorCode.class.getField(error.getErrorCode().name()).getAnnotation(ErrorCode.Doc.class);
			if (doc != null) {
				description = doc.value();
			}
		} catch (NoSuchFieldException | SecurityException e) {
			logger.warn("Parsing error while parsing exception", e);
		}

		HttpStatus httpStatus = error.getErrorCode().getStatus();
		response.setStatus(httpStatus.value());

		ModelAndView errorView = new ModelAndView("error");
		errorView.addObject("error", error);
		errorView.addObject("niceHttpStatus", cc(httpStatus.name()));
		errorView.addObject("description", description);

		return errorView;
	}

	/**
	 * Turns an uppercase string with underscore separators into Camel Case
	 */
	private static String cc(String name) {
		return Arrays.stream(name.split("_")).map(HtmlAndRestErrorController::capitalize).collect(Collectors.joining(" "));
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
