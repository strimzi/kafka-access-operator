/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Utils {

    private Base64Utils() {}

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    public static byte[] decodeFromBase64(String encodedData) {
        return DECODER.decode(encodedData);
    }

    public static String decodeFromBase64ToString(String encodedData) {
        return new String(decodeFromBase64(encodedData), StandardCharsets.US_ASCII);
    }

    public static String encodeToBase64(String data) {
        return ENCODER.encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
}
