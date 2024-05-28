/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

/**
 * The class for exception when Kuberentes resources are missing
 */
public class MissingKubernetesResourceException extends RuntimeException {
    /**
     * Default constructor
     */
    public MissingKubernetesResourceException() {}

    /**
     * Constructor
     *
     * @param message The exception message
     */
    public MissingKubernetesResourceException(final String message) {
        super(message);
    }

    /**
     * Constructor
     *
     * @param message The exception message
     * @param cause The exception cause
     */
    public MissingKubernetesResourceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     *
     * @param cause The exception cause
     */
    public MissingKubernetesResourceException(final Throwable cause) {
        super(cause);
    }

}
