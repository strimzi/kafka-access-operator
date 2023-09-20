/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Encoder {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    public static String encodeUtf8(String data) {
        return ENCODER.encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
}
