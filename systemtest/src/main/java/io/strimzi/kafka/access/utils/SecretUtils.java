/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.utils;

public class SecretUtils {

    private SecretUtils() {}

    public static String createUserKey(String key) {
        String data = "-----BEGIN PRIVATE KEY-----\n" + key + "\n-----END PRIVATE KEY-----\n";
        return Base64Utils.encodeToBase64(data);
    }

    public static String createUserCrt(String crt) {
        String data = "-----BEGIN CERTIFICATE-----\n" + crt + "\n-----END CERTIFICATE-----\n";
        return Base64Utils.encodeToBase64(data);
    }

    public static String createUserP12(byte[] p12) {
        return Base64Utils.encodeToBase64(p12);
    }

    public static String createUserPassword(String password) {
        return Base64Utils.encodeToBase64(password);
    }
}
