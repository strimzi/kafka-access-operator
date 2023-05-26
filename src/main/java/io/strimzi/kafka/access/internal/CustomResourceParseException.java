/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

/**
 * The class for custom resource parsing exception
 */
public class CustomResourceParseException extends RuntimeException {

    /**
     * Default constructor
     */
    public CustomResourceParseException() {}

    /**
     * Constructor
     *
     * @param message The exception message
     */
    public CustomResourceParseException(final String message) {
        super(message);
    }

    /**
     * Constructor
     *
     * @param message The exception message
     * @param cause The exception cause
     */
    public CustomResourceParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     *
     * @param cause The exception cause
     */
    public CustomResourceParseException(final Throwable cause) {
        super(cause);
    }

}
