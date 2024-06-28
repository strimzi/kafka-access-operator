/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.templates;

import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.ListenerAddress;
import io.strimzi.api.kafka.model.kafka.listener.ListenerAddressBuilder;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatus;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatusBuilder;

import java.util.ArrayList;
import java.util.List;

public class KafkaTemplates {

    private KafkaTemplates() {}

    public static KafkaBuilder kafkaWithListeners(String namespaceName, String clusterName, String host, List<GenericKafkaListener> listOfListeners) {
        List<ListenerStatus> listOfStatuses = new ArrayList<>();

        listOfListeners.forEach(listener -> {
            ListenerAddress address = new ListenerAddressBuilder()
                .withHost(host)
                .withPort(listener.getPort())
                .build();

            listOfStatuses.add(new ListenerStatusBuilder()
                .withName(listener.getName())
                .withAddresses(List.of(address))
                .build());
        });

        return new KafkaBuilder()
            .withNewMetadata()
                .withName(clusterName)
                .withNamespace(namespaceName)
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
            .endSpec()
            .withNewStatus()
                .addAllToListeners(listOfStatuses)
            .endStatus();
    }
}
