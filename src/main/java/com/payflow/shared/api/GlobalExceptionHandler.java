package com.payflow.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.NoSuchElementException;

import java.util.List;
import org.springframework.web.bind.MissingRequestHeaderException;
/**
 * Converts application and validation exceptions into consistent
 * HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField()
                                + ": "
                                + error.getDefaultMessage()
                )
                .toList();

        return ApiErrorResponse.validation(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "request validation failed",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(
            MissingRequestHeaderException.class
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMissingHeader(
            MissingRequestHeaderException exception,
            HttpServletRequest request
    ) {
        return ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleIllegalState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        return ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(
            NoSuchElementException exception,
            HttpServletRequest request
    ) {
        return ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
    }

}