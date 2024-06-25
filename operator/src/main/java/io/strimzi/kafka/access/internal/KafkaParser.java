/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaClusterSpec;
import io.strimzi.api.kafka.model.kafka.KafkaSpec;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserTlsClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserTlsExternalClientAuthentication;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationTls;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatus;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Representation of a Kafka parser that gets the relevant Kafka listener from different sources
 */
public class KafkaParser {

    /**
     *  The constant for listener authentication type
     */
    public static final String LISTENER_AUTH_NONE = "listener-auth-none";

    /**
     * The constant for no authentication type
     */
    public static final String USER_AUTH_NONE = "user-auth-none";

    /**
     *  The constant for undefined authentication type
     */
    public static final String USER_AUTH_UNDEFINED = "user-auth-undefined";

    /**
     * Selects a KafkaListener from the Kafka resource based on the KafkaAccessSpec and the request authentication type
     *
     * @param kafka                The Kafka resource
     * @param kafkaAccessSpec      The KafkaAccessSpec resource
     * @param kafkaUserAuth        The KafkaUser authentication type, can be null
     *
     * @return                     A new instance of KafkaListener for the chosen listener
     */
    public static KafkaListener getKafkaListener(final Kafka kafka, final KafkaAccessSpec kafkaAccessSpec, final String kafkaUserAuth) {
        final Optional<String> chosenListener = Optional.ofNullable(kafkaAccessSpec.getKafka())
                .map(KafkaReference::getListener);
        final String chosenUserAuthType = Optional.ofNullable(kafkaUserAuth).orElse(USER_AUTH_NONE);
        final List<GenericKafkaListener> genericKafkaListeners = Optional.ofNullable(kafka)
                .map(Kafka::getSpec)
                .map(KafkaSpec::getKafka)
                .map(KafkaClusterSpec::getListeners)
                .orElseGet(ArrayList::new);

        final KafkaListener kafkaListener;
        if (chosenListener.isPresent()) {
            kafkaListener = getChosenListener(chosenListener.get(), chosenUserAuthType, genericKafkaListeners);
        } else {
            if (USER_AUTH_UNDEFINED.equals(chosenUserAuthType)) {
                throw new CustomResourceParseException("Cannot match KafkaUser with undefined auth to a Kafka listener, specify the listener in the KafkaAccess CR.");
            }
            final Stream<KafkaListener> possibleListeners = genericKafkaListeners
                    .stream()
                    .map(KafkaListener::new)
                    .filter(listener -> listenerAndUserAuthCompatible(listener.getAuthenticationType(), chosenUserAuthType));
            kafkaListener = pickListener(possibleListeners);
        }

        final List<ListenerStatus> listenersInStatus = Optional.ofNullable(kafka)
                .map(Kafka::getStatus)
                .map(KafkaStatus::getListeners)
                .orElse(Collections.emptyList());
        final Optional<String> bootstrapServer = listenersInStatus.stream()
                .filter(listener -> kafkaListener.getName().equals(listener.getName()))
                .findFirst()
                .map(ListenerStatus::getBootstrapServers);
        if (bootstrapServer.isEmpty()) {
            throw new CustomResourceParseException(String.format("The bootstrap server address for the listener %s is missing from the Kafka resource status.",
                    kafkaListener.getName())
            );
        }
        return kafkaListener.withBootstrapServer(bootstrapServer.get());
    }

    private static KafkaListener getChosenListener(final String chosenListener, final String chosenUserAuthType, final List<GenericKafkaListener> genericKafkaListeners) {
        final KafkaListener kafkaListener = genericKafkaListeners
                .stream()
                .filter(genericKafkaListener -> chosenListener.equals(genericKafkaListener.getName()))
                .findFirst()
                .map(KafkaListener::new)
                .orElseThrow(() -> new CustomResourceParseException(String.format("The specified listener %s is missing from the Kafka resource.",
                        chosenListener)
                ));
        if (!listenerAndUserAuthCompatible(kafkaListener.getAuthenticationType(), chosenUserAuthType)) {
            throw new CustomResourceParseException(String.format("Provided listener %s and Kafka User do not have compatible authentication configurations.", kafkaListener.getName()));
        }
        return kafkaListener;
    }

    private static KafkaListener pickListener(final Stream<KafkaListener> listeners) {
        final Map<String, KafkaListener> internalListeners = new HashMap<>();
        final Map<String, KafkaListener> externalListeners = new HashMap<>();
        listeners.forEach(possibleListener -> {
            if (possibleListener.getType() == KafkaListenerType.INTERNAL) {
                internalListeners.put(possibleListener.getName(), possibleListener);
            } else {
                externalListeners.put(possibleListener.getName(), possibleListener);
            }
        });
        if (internalListeners.isEmpty() && externalListeners.isEmpty()) {
            throw new CustomResourceParseException("No listeners present in Kafka cluster that match auth requirement.");
        }
        final Map<String, KafkaListener> possibleListenerMap = internalListeners.isEmpty() ? externalListeners : internalListeners;
        final String listenerName = possibleListenerMap.keySet()
                .stream()
                .sorted()
                .findFirst()
                .get();
        return possibleListenerMap.get(listenerName);
    }

    private static boolean listenerAndUserAuthCompatible(final String listenerAuthType, final String userAuthType) {
        if (LISTENER_AUTH_NONE.equals(listenerAuthType)) { // If the listener has no auth we don't expect a KafkaUser
            return USER_AUTH_NONE.equals(userAuthType);
        }
        if (USER_AUTH_NONE.equals(userAuthType)) { // If no KafkaUser is specified any listener valid
            return true;
        }
        if (USER_AUTH_UNDEFINED.equals(userAuthType)) { // If KafkaUser is defined but has no auth any listener (other than a no auth listener) is fine
            return true;
        }
        if (KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512.equals(listenerAuthType)) {
            return KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512.equals(userAuthType);
        }
        if (KafkaListenerAuthenticationTls.TYPE_TLS.equals(listenerAuthType)) {
            return KafkaUserTlsClientAuthentication.TYPE_TLS.equals(userAuthType)
                    || KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL.equals(userAuthType);
        }
        return false;
    }
}
