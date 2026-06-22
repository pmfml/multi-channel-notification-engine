package com.pmfml.mcne.exceptions;

/**
 * Exception thrown when a requested resource cannot be found in the data store.
 * Maps to HTTP 404 Not Found when thrown from a controller context.
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
