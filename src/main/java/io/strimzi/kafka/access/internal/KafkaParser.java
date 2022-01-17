/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaClusterSpec;
import io.strimzi.api.kafka.model.KafkaSpec;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.api.kafka.model.status.KafkaStatus;
import io.strimzi.api.kafka.model.status.ListenerStatus;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KafkaParser {
    public static final String NONE_AUTH = "none";

    /**
     * Selects a KafkaListener from the Kafka resource based on the KafkaAccessSpec
     *
     * @param kafka                The Kafka resource
     * @param kafkaAccessSpec      The KafkaAccessSpec resource
     *
     * @return                     A new instance of KafkaListener for the chosen listener
     */
    public static KafkaListener getKafkaListener(final Kafka kafka, final KafkaAccessSpec kafkaAccessSpec) {
        return getKafkaListener(kafka, kafkaAccessSpec, null);
    }

    /**
     * Selects a KafkaListener from the Kafka resource based on the KafkaAccessSpec and the request authentication type
     *
     * @param kafka                The Kafka resource
     * @param kafkaAccessSpec      The KafkaAccessSpec resource
     * @param kafkaUserAuth        The KafkaUser authentication type
     *
     * @return                     A new instance of KafkaListener for the chosen listener
     */
    public static KafkaListener getKafkaListener(final Kafka kafka, final KafkaAccessSpec kafkaAccessSpec, final String kafkaUserAuth) {
        final Optional<String> chosenListener = Optional.ofNullable(kafkaAccessSpec.getKafka())
                .map(KafkaReference::getListener);
        final Optional<String> chosenAuthType = Optional.ofNullable(kafkaUserAuth);
        final List<GenericKafkaListener> genericKafkaListeners = Optional.ofNullable(kafka)
                .map(Kafka::getSpec)
                .map(KafkaSpec::getKafka)
                .map(KafkaClusterSpec::getListeners)
                .orElseGet(ArrayList::new);

        final KafkaListener kafkaListener;
        if (chosenListener.isPresent()) {
            kafkaListener = genericKafkaListeners
                    .stream()
                    .filter(genericKafkaListener -> chosenListener.get().equals(genericKafkaListener.getName()))
                    .findFirst()
                    .map(KafkaListener::new)
                    .orElseThrow(() -> new ParserException(String.format("The specified listener %s is missing from the Kafka resource.",
                            chosenListener.get())
                    ));
            chosenAuthType.ifPresent(authType -> {
                if (!authType.equals(kafkaListener.getAuthenticationType())) {
                    throw new ParserException(String.format("Provided listener %s and Kafka User do not have compatible authentication configurations.", kafkaListener.getName()));
                }
            });
        } else {

            final Map<String, KafkaListener> internalListeners = new HashMap<>();
            final Map<String, KafkaListener> externalListeners = new HashMap<>();
            genericKafkaListeners
                    .stream()
                    .map(KafkaListener::new)
                    .filter(listener -> chosenAuthType.isEmpty() || chosenAuthType.get().equals(listener.getAuthenticationType()))
                    .forEach(possibleListener -> {
                        if (possibleListener.getType() == KafkaListenerType.INTERNAL) {
                            internalListeners.put(possibleListener.getName(), possibleListener);
                        } else {
                            externalListeners.put(possibleListener.getName(), possibleListener);
                        }
                    });
            if (internalListeners.isEmpty() && externalListeners.isEmpty()) {
                throw new ParserException("No listeners present in Kafka cluster that match auth requirement.");
            }
            final Map<String, KafkaListener> possibleListenerMap = internalListeners.isEmpty() ? externalListeners : internalListeners;
            final String listenerName = possibleListenerMap.keySet()
                    .stream()
                    .sorted()
                    .findFirst()
                    .get();
            kafkaListener = possibleListenerMap.get(listenerName);
        }

        final List<ListenerStatus> listenersInStatus = Optional.ofNullable(kafka)
                .map(Kafka::getStatus)
                .map(KafkaStatus::getListeners)
                .orElse(Collections.emptyList());
        final Optional<String> bootstrapServer = listenersInStatus.stream()
                .filter(listener -> kafkaListener.getName().equals(listener.getType()))
                .findFirst()
                .map(ListenerStatus::getBootstrapServers);
        if (bootstrapServer.isEmpty()) {
            throw new ParserException(String.format("The bootstrap server address for the listener %s is missing from the Kafka resource status.",
                    kafkaListener.getName())
            );
        }
        return kafkaListener.withBootstrapServer(bootstrapServer.get());
    }
}
