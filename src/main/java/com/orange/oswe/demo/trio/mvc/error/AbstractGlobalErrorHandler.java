package com.orange.oswe.demo.trio.mvc.error;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base global error handler component that manages:
 * <ul>
 * <li>intercepts uncaught Exceptions (with {@link ExceptionHandler}),</li>
 * <li>basic exception rendering,</li>
 * <li>specific treatment on internal errors:
 * <ul>
 * <li>generates a unique error ID,</li>
 * <li>dumps the full error stack trace along with the ID,</li>
 * <li>replaces the textual description with a generic error message with the ID (for traceability).</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * Override it and implement {@link #doRender(HttpServletRequest, HttpServletResponse, ErrorDetails)} to manage final
 * error rendering and possibly content negotiation.
 */
@Controller
@RequestMapping(GlobalErrorHandler.PATH)
public abstract class AbstractGlobalErrorHandler implements ErrorController {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String ERROR_ATTRIBUTE = AbstractGlobalErrorHandler.class.getName() + ".error";

    protected static final String PATH = "/error";

    @Override
    public String getErrorPath() {
        return PATH;
    }

    /**
     * The only {@link ExceptionHandler} method: catches and stores all <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html#mvc-ann-rest-spring-mvc-exceptions">standard Spring MVC Exceptions</a>
     * for later use when the error has to be rendered (as JSON, HTML or else)
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleAnyException(HttpServletRequest request, HttpServletResponse response, Exception error) {
        // stores the exception in the request attributes to be reused at render time
        request.setAttribute(ERROR_ATTRIBUTE, error);
        // then directly render the error
        return render(request, response);
    }

    /**
     * The main error rendering endpoint
     * <p>
     * Can be either called directly by this {@link ExceptionHandler}, or by the JEE container (non-Spring MVC exception)
     */
    @RequestMapping
    public ModelAndView render(HttpServletRequest request, HttpServletResponse response) {
        ErrorDetails details = preRender(request, response);
        return doRender(request, response, details);
    }

    /**
     * Manages error rendering and possibly content negotiation.
     * @param request request
     * @param response response
     * @param details error details
     * @return most suitable view (use {@link #findFirstAccept(HttpServletRequest, MediaType...)} for content negotiation)
     */
    protected abstract ModelAndView doRender(HttpServletRequest request, HttpServletResponse response, ErrorDetails details);

    /**
     * Displayable representation of an Exception
     */
    @Data
    protected static class ErrorDetails {
        final ErrorCode code;
        String description;
        final Exception exception;

        public ErrorDetails(ErrorCode code, String description, Exception exception) {
            this.code = code;
            this.description = description;
            this.exception = exception;
        }

        public ErrorDetails(ErrorCode code, Exception exception) {
            this(code, exception.getMessage(), exception);
        }
    }

    /**
     * This is the global error handling and pre-rendering method.
     * <p>
     *
     * @param request
     * @param response
     * @return error details
     */
    protected ErrorDetails preRender(HttpServletRequest request, HttpServletResponse response) {
        // 1: retrieve exception from request
        Exception error = getErrorFromRequest(request);

        // 2: make error details
        ErrorDetails details = null;
        if (error == null) {
            // this is not a Spring MVC exception and the error controller is being called by the JEE container
            // render error based on other JEE request attributes
            details = makeErrorDetails(request);
        } else {
            details = makeErrorDetails(error);
        }

        // 3: treat internal errors
        if (details.getCode().getStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
            logInternalError(request, response, details);
        }

        // 4: set response status
        response.reset();
        response.setStatus(details.getCode().getStatus().value());

        return details;
    }

    /**
     * Retrieves the current {@link Throwable error} being handled in the {@link HttpServletRequest request}
     *
     * @param request request
     * @return error, if any
     */
    protected Exception getErrorFromRequest(HttpServletRequest request) {
        // first try to get an exception handled by this @ExceptionHandler
        Exception error = (Exception) request.getAttribute(ERROR_ATTRIBUTE);
        if (error == null) {
            // else try to retrieve exception handled by JEE container
            error = (Exception) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        }
        return error;
    }

    /**
     * Makes error details from JEE request attributes
     * @param request request
     * @return error details
     */
    private ErrorDetails makeErrorDetails(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode != null) {
            ErrorCode code = getDefaultCode(HttpStatus.valueOf(statusCode));
            String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
            return new ErrorDetails(code, message, null);
        } else {
            // no error ?!?
            return new ErrorDetails(ErrorCode.Success, "Hey! looks like there is no error...", null);
        }
    }

    /**
     * Makes error details from a genuine {@link Exception}
     * <p>
     * Manages all <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html#mvc-ann-rest-spring-mvc-exceptions">standard Spring MVC Exceptions</a>
     */
    public ErrorDetails makeErrorDetails(Exception error) {
        logger.info("handleAnyException {}: {}", error.getClass().getName(), error.getMessage());
        if (error instanceof AuthenticationException) {
            // TODO: several Unauthorized codes here (missing/invalid/expired credentials)
            // TODO native message ???
            return new ErrorDetails(ErrorCode.MissingCredentials, error);
        } else if (error instanceof NoSuchRequestHandlingMethodException) {
            // message is ok
            return new ErrorDetails(ErrorCode.ServiceNotFound, "No matching handler found for request.", error);
        } else if (error instanceof HttpRequestMethodNotSupportedException) {
            // message is ok
            return new ErrorDetails(ErrorCode.MethodNotSupported, error);
        } else if (error instanceof HttpMediaTypeNotSupportedException) {
            // message is ok
            return new ErrorDetails(ErrorCode.MediaTypeNotSupported, error);
        } else if (error instanceof HttpMediaTypeNotAcceptableException) {
            // message is ok
            return new ErrorDetails(ErrorCode.MediaTypeNotAcceptable, error);
        } else if (error instanceof MissingServletRequestParameterException) {
            // message is ok
            return new ErrorDetails(ErrorCode.MissingParameter, error);
        } else if (error instanceof TypeMismatchException) {
            // need to rewrite message (too technical)
            return new ErrorDetails(ErrorCode.InvalidParameter, buildDescription((TypeMismatchException) error), error);
        } else if (error instanceof HttpMessageNotReadableException) {
            // need to rewrite message (too technical)
            if (error.getCause() instanceof JsonMappingException) {
                return new ErrorDetails(ErrorCode.InvalidRequestBody, buildDescription((JsonMappingException) error.getCause()), error);
            } else {
                return new ErrorDetails(ErrorCode.InvalidRequestBody, "Request body is invalid and could not be read (see documentation).", error);
            }
        } else if (error instanceof MethodArgumentNotValidException) {
            // need to rewrite message (too technical)
            String name = ((MethodArgumentNotValidException) error).getParameter().getParameterName();
            return new ErrorDetails(ErrorCode.InvalidParameter, "Parameter '" + name + "' is not valid (see documentation).", error);
        } else if (error instanceof MissingServletRequestPartException) {
            // message is ok
            return new ErrorDetails(ErrorCode.MissingRequestPart, error);
        } else if (error instanceof BindException) {
            // need to rewrite message (too technical)
            String name = ((BindException) error).getNestedPath();
            return new ErrorDetails(ErrorCode.InvalidParameter, "Parameter '" + name + "' is not valid (see documentation).", error);
        } else if (error instanceof NoHandlerFoundException) {
            // need to rewrite message (too technical)
            return new ErrorDetails(ErrorCode.ServiceNotFound, "No handler found for " + (((NoHandlerFoundException) error).getHttpMethod()) + " " + (((NoHandlerFoundException) error).getRequestURL()), error);
//        } else if (error instanceof HttpMessageNotWritableException) {
//        } else if (error instanceof ConversionNotSupportedException) {
//        } else if (error instanceof MissingPathVariableException) {
        } else {
            /*
             * default case:
             * HttpMessageNotWritableException, ConversionNotSupportedException, MissingPathVariableException
             * and others are considered as internal errors
             */
            ErrorCode code = getDefaultCode(findStatusByAnnotation(error.getClass()));
            return new ErrorDetails(code, error);
        }
    }

    /**
     * <ol>
     * <li>generated a unique ID,</li>
     * <li>adds this ID as a response header,</li>
     * <li>replaces the {@link ErrorDetails error} description with a generic message,</li>
     * <li>logs the Exception in ERROR with the generated ID (for traceability purpose).</li>
     * </ol>
     */
    void logInternalError(HttpServletRequest request, HttpServletResponse response, ErrorDetails details) {
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

        // change internal exception description (too technical)
        details.setDescription("Internal error occurred in request '" + request.getMethod() + " " + reqUrl + "'");

        // then log error with complete stack for analysis/debugging
        logger.error("Internal error occurred in request '{} {}'", request.getMethod(), reqUrl, details.getException());
    }

    /**
     * Builds a user understandable message of {@link TypeMismatchException}
     */
    String buildDescription(TypeMismatchException tme) {
        StringBuilder msg = new StringBuilder();
        if (tme.getPropertyName() == null) {
            msg.append("A parameter");
        } else {
            msg.append("Parameter '").append(tme.getPropertyName()).append("'");
        }
        msg.append(" with value <").append(String.valueOf(tme.getValue())).append(">");
        if (tme.getRequiredType() == null) {
            msg.append(" is not valid (see documentation).");
        } else {
            msg.append(" is not a valid <").append(tme.getRequiredType().getSimpleName()).append("> (see documentation).");
        }
        return msg.toString();
    }

    /**
     * Builds a user understandable message of {@link JsonMappingException}
     */
    String buildDescription(JsonMappingException jme) {
        StringBuilder locationHint = new StringBuilder();
        JsonLocation location = jme.getLocation();
        if (location != null) {
            locationHint.append(" at line#").append(location.getLineNr()).append(", col#")
                    .append(location.getColumnNr());
            if (location.getCharOffset() >= 0) {
                locationHint.append(" (char#").append(location.getCharOffset()).append(")");
            }
        }
        List<JsonMappingException.Reference> path = jme.getPath();
        if (path != null) {
            locationHint.append(" on <root>");
            for (JsonMappingException.Reference ref : path) {
                if (ref.getFieldName() == null) {
                    locationHint.append("[").append(ref.getIndex()).append("]");
                } else {
                    locationHint.append(".").append(ref.getFieldName());
                }
            }
        }
        if (jme instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException upe = (UnrecognizedPropertyException) jme;
            return "Invalid request body" + locationHint.toString() + ": unrecognized property '" + (upe.getPropertyName()) + "' (see documentation).";
        } else if (jme instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) jme;
            Class<?> type = ife.getTargetType();
            return "Invalid request body" + locationHint.toString() + ": value <" + ife.getValue() + "> " + (type == null ? "is invalid" : "is not a valid <" + type.getSimpleName() + ">") + " (see documentation).";
        } else {
            return "Invalid request body" + locationHint.toString() + " (see documentation).";
        }
    }

    private final Map<Class<? extends Throwable>, HttpStatus> type2Status = new HashMap<>();

    /**
     * Looks for {@link ResponseStatus} annotation in the exception type
     * hierarchy, and determines the {@link HttpStatus}
     * <p>
     * If no such annotation is found, the state defaults to
     * {@link HttpStatus#INTERNAL_SERVER_ERROR}
     *
     * @param errorType error type to determine HttpStatus
     * @return HttpStatus corresponding to the given exception type
     */
    HttpStatus findStatusByAnnotation(Class<? extends Throwable> errorType) {
        HttpStatus cachedStatus = type2Status.get(errorType);
        if (cachedStatus == null) {
            if (errorType.equals(Throwable.class)) {
                // root exceptions type
                cachedStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            } else {
                // find from Spring annotation
                ResponseStatus st = errorType.getAnnotation(ResponseStatus.class);
                if (st != null) {
                    cachedStatus = st.value();
                } else {
                    // recurse on super type
                    cachedStatus = findStatusByAnnotation((Class<? extends Throwable>) errorType.getSuperclass());
                }
            }
            // add to cache
            type2Status.put(errorType, cachedStatus);
        }
        return cachedStatus;
    }

    /**
     * Retrieves the default {@link ErrorCode} from an {@link HttpStatus}
     */
    ErrorCode getDefaultCode(HttpStatus status) {
        if (status == null) {
            return ErrorCode.InternalError;
        }
        switch (status) {
            case UNAUTHORIZED:
                return ErrorCode.MissingCredentials;
            case FORBIDDEN:
                return ErrorCode.ResourceAccessDenied;
            case BAD_REQUEST:
                return ErrorCode.BadRequest;
            case NOT_FOUND:
                return ErrorCode.ResourceNotFound;
            case METHOD_NOT_ALLOWED:
                return ErrorCode.MethodNotSupported;
            case NOT_ACCEPTABLE:
                return ErrorCode.MediaTypeNotAcceptable;
            case UNSUPPORTED_MEDIA_TYPE:
                return ErrorCode.MediaTypeNotSupported;
            case CONFLICT:
                return ErrorCode.ResourceConflict;

            case INTERNAL_SERVER_ERROR:
                return ErrorCode.InternalError;
            case NOT_IMPLEMENTED:
                return ErrorCode.NotImplemented;
            default:
                logger.error("Unexpected HTTP error code through Spring annotations: " + status);
                return ErrorCode.InternalError;
        }
    }

    /**
     * Looks for the first matching {@link MediaType} in the {@code accept} request header
     *
     * @param request        request
     * @param supportedTypes types to test against
     * @return first matching type; or {@code null} if no matching was found
     */
    MediaType findFirstAccept(HttpServletRequest request, MediaType... supportedTypes) {
        Enumeration<String> accepts = request.getHeaders("accept");
        while (accepts.hasMoreElements()) {
            List<MediaType> types = MediaType.parseMediaTypes(accepts.nextElement());
            for (MediaType type : types) {
                for (MediaType supported : supportedTypes) {
                    if (type.equals(supported)) {
                        return supported;
                    }
                }
            }
        }
        return null;
    }
}
