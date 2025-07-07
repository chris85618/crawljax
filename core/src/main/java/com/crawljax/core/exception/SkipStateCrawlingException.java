package com.crawljax.core.exception;

/**
 * An exception that can be thrown from a plugin to signal that the current
 * state should not be crawled.
 */
public class SkipStateCrawlingException extends RuntimeException {
    public SkipStateCrawlingException(String message) {
        super(message);
    }
}