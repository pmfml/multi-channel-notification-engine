package com.pmfml.mcne.exceptions;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles Bean Validation failures (@Valid on @RequestBody), returning a structured
   * 400 response with per-field error messages.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
            (existing, duplicate) -> existing));

    Map<String, Object> body = Map.of(
        "timestamp", Instant.now(),
        "status", HttpStatus.BAD_REQUEST.value(),
        "error", "Validation Failed",
        "fields", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  /**
   * Handles domain-level illegal argument exceptions (e.g. unsupported notification channel),
   * returning a 400 Bad Request with a descriptive message.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
    Map<String, Object> errorResponse = Map.of(
        "timestamp", Instant.now(),
        "status", HttpStatus.BAD_REQUEST.value(),
        "error", "Bad Request",
        "message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /**
   * Handles resource not found exceptions, returning a 404 Not Found.
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
    Map<String, Object> body = Map.of(
        "timestamp", Instant.now(),
        "status", HttpStatus.NOT_FOUND.value(),
        "error", "Not Found",
        "message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  /**
   * Catch-all handler for any unexpected exception, returning a generic 500 response
   * without leaking internal stack trace details to the client.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    log.error("Unexpected error occurred", ex);
    Map<String, Object> body = Map.of(
        "timestamp", Instant.now(),
        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "error", "Internal Server Error",
        "message", "An unexpected error occurred. Please try again later.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}