/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.templates;

import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;

public class ListenerTemplates {

    private ListenerTemplates() {}

    public static GenericKafkaListener tlsListener(String name, KafkaListenerType type, KafkaListenerAuthentication auth, int port) {
        return defaultListener(name, type, auth, port)
            .withTls()
            .build();
    }

    public static GenericKafkaListener listener(String name, KafkaListenerType type, KafkaListenerAuthentication auth, int port) {
        return defaultListener(name, type, auth, port)
            .withTls(false)
            .build();
    }

    private static GenericKafkaListenerBuilder defaultListener(String name, KafkaListenerType type, KafkaListenerAuthentication auth, int port) {
        return new GenericKafkaListenerBuilder()
            .withName(name)
            .withType(type)
            .withAuth(auth)
            .withPort(port);
    }
}
