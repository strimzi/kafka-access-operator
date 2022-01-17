/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

public class ParserException extends RuntimeException {

    public ParserException() {}

    public ParserException(final String message) {
        super(message);
    }

    public ParserException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ParserException(final Throwable cause) {
        super(cause);
    }

}
