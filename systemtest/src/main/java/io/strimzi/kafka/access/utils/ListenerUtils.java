/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.utils;

public class ListenerUtils {

    private ListenerUtils() {}

    public static String bootstrapServer(String hostname, int port) {
        return String.join(":", hostname, String.valueOf(port));
    }
}
