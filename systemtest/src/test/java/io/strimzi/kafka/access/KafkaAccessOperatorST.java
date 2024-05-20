/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.KafkaUserBuilder;
import io.strimzi.api.kafka.model.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.KafkaUserTlsClientAuthentication;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class KafkaAccessOperatorST extends AbstractST {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAccessOperatorST.class);

    @Test
    void testAccessToSpecifiedListener() {
        String clusterName = "my-cluster";

        List<GenericKafkaListener> listeners = List.of(
            new GenericKafkaListenerBuilder()
                .withName("internal")
                .withType(KafkaListenerType.INTERNAL)
                .withNewKafkaListenerAuthenticationTlsAuth()
                .endKafkaListenerAuthenticationTlsAuth()
                .withTls()
                .withPort(9097)
                .build(),
            new GenericKafkaListenerBuilder()
                .withName("nodeport")
                .withType(KafkaListenerType.NODEPORT)
                .withNewKafkaListenerAuthenticationTlsAuth()
                .endKafkaListenerAuthenticationTlsAuth()
                .withTls()
                .withPort(9098)
                .build(),
            new GenericKafkaListenerBuilder()
                .withName("loadbal")
                .withType(KafkaListenerType.LOADBALANCER)
                .withNewKafkaListenerAuthenticationScramSha512Auth()
                .endKafkaListenerAuthenticationScramSha512Auth()
                .withTls(false)
                .withPort(9099)
                .build()
        );

        Kafka kafka = defaultKafka(clusterName, listeners).build();

        resourceManager.createResourceWithoutWait(
            kafka,
            kafkaUser("tls", new KafkaUserTlsClientAuthentication()),
            kafkaUser("scram", new KafkaUserScramSha512ClientAuthentication())
        );

    }

    @Test
    void testAccessToUnspecifiedSingleListener() {

    }

    @Test
    void testAccessToUnspecifiedMultipleListeners() {

    }

    @Test
    void testAccessToUnspecifiedMultipleListenersWithSingleInternal() {

    }

    @Test
    void testAccessToUnspecifiedMultipleListenersWithMultipleInternal() {

    }

    private KafkaBuilder defaultKafka(String clusterName, List<GenericKafkaListener> listOfListeners) {
        return new KafkaBuilder()
            .withNewMetadata()
                .withName(clusterName)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withNewKafka()
                    .withReplicas(3)
                    .withListeners(listOfListeners)
                .endKafka()
                .withNewZookeeper()
                    .withReplicas(3)
                    .withNewEphemeralStorage()
                    .endEphemeralStorage()
                .endZookeeper()
            .endSpec();
    }

    private KafkaUser kafkaUser(String userName, KafkaUserAuthentication auth) {
        return new KafkaUserBuilder()
            .withNewMetadata()
                .withName(userName)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withAuthentication(auth)
            .endSpec()
            .build();
    }
}
