/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

/**
 *
 */
public class CustomResourceParseException extends RuntimeException {

    /**
     *
     */
    public CustomResourceParseException() {}

    /**
     * @param message
     */
    public CustomResourceParseException(final String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public CustomResourceParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public CustomResourceParseException(final Throwable cause) {
        super(cause);
    }

}
