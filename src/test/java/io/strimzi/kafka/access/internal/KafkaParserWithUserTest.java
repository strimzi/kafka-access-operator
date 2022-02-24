/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.KafkaUserTlsClientAuthentication;
import io.strimzi.api.kafka.model.KafkaUserTlsExternalClientAuthentication;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationOAuth;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationTls;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.api.kafka.model.status.ListenerStatus;
import io.strimzi.kafka.access.ResourceProvider;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class KafkaParserWithUserTest {

    private static final String CLUSTER_NAME = "my-cluster";
    private static final String BOOTSTRAP_HOST = "my-cluster.svc";
    private static final int BOOTSTRAP_PORT_9092 = 9092;
    private static final int BOOTSTRAP_PORT_9093 = 9093;

    private static final GenericKafkaListener PLAIN_LISTENER = ResourceProvider.getListener("plain-listener", KafkaListenerType.INTERNAL, false);
    private static final GenericKafkaListener PLAIN_LISTENER_WITH_TLS = ResourceProvider.getListener("plain-listener-with-tls", KafkaListenerType.INTERNAL, true);
    private static final GenericKafkaListener SCRAM_LISTENER = ResourceProvider.getListener("scram-listener", KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512());
    private static final GenericKafkaListener SCRAM_LISTENER_WITH_TLS = ResourceProvider.getListener("scram-listener-with-tls", KafkaListenerType.INTERNAL, true, new KafkaListenerAuthenticationScramSha512());
    private static final GenericKafkaListener TLS_LISTENER = ResourceProvider.getListener("tls-listener", KafkaListenerType.INTERNAL, true, new KafkaListenerAuthenticationTls());
    private static final GenericKafkaListener OAUTH_LISTENER = ResourceProvider.getListener("oauth-listener", KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationOAuth());
    private static final GenericKafkaListener OAUTH_LISTENER_WITH_TLS = ResourceProvider.getListener("oauth-listener-with-tls", KafkaListenerType.INTERNAL, true, new KafkaListenerAuthenticationOAuth());

    static Stream<Arguments> listenerSpecifiedWithMatchingAuthProvider() {
        return Stream.of(
                arguments(named(SCRAM_LISTENER.getName(), SCRAM_LISTENER), KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512),
                arguments(named(SCRAM_LISTENER.getName(), SCRAM_LISTENER), KafkaParser.USER_AUTH_UNDEFINED),
                arguments(named(SCRAM_LISTENER_WITH_TLS.getName(), SCRAM_LISTENER_WITH_TLS), KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512),
                arguments(named(SCRAM_LISTENER_WITH_TLS.getName(), SCRAM_LISTENER_WITH_TLS), KafkaParser.USER_AUTH_UNDEFINED),

                arguments(named(TLS_LISTENER.getName(), TLS_LISTENER), KafkaUserTlsClientAuthentication.TYPE_TLS),
                arguments(named(TLS_LISTENER.getName(), TLS_LISTENER), KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL),
                arguments(named(TLS_LISTENER.getName(), TLS_LISTENER), KafkaParser.USER_AUTH_UNDEFINED),

                arguments(named(OAUTH_LISTENER.getName(), OAUTH_LISTENER), KafkaParser.USER_AUTH_UNDEFINED),
                arguments(named(OAUTH_LISTENER_WITH_TLS.getName(), OAUTH_LISTENER), KafkaParser.USER_AUTH_UNDEFINED)
        );
    }

    @ParameterizedTest
    @MethodSource("listenerSpecifiedWithMatchingAuthProvider")
    @DisplayName("When listener is specified in CR and it has a specific auth and KafkaUser is specified with the same auth, then the listener is selected")
    void testListenerSpecifiedWithMatchingAuth(final GenericKafkaListener selectedListener, final String kafkaUserAuthType) {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setListener(selectedListener.getName());
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final List<GenericKafkaListener> listeners = List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS,
                SCRAM_LISTENER, SCRAM_LISTENER_WITH_TLS,
                TLS_LISTENER,
                OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS
        );
        final List<ListenerStatus> listenerStatuses = new ArrayList<>();
        listeners.forEach(listener -> {
            if (listener.getName().equals(selectedListener.getName())) {
                listenerStatuses.add(ResourceProvider.getListenerStatus(selectedListener.getName(), BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
            } else {
                listenerStatuses.add(ResourceProvider.getListenerStatus(listener.getName(), BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093));
            }
        });
        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                listeners,
                listenerStatuses
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, kafkaUserAuthType);
        assertThat(listener.getName()).isEqualTo(selectedListener.getName());
        assertThat(listener.getType()).isEqualTo(selectedListener.getType());
        assertThat(listener.isTls()).isEqualTo(selectedListener.isTls());
        assertThat(listener.getAuthenticationType()).isEqualTo(selectedListener.getAuth().getType());
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    static Stream<Arguments> listenerWithMatchingAuthProvider() {
        return Stream.of(
                arguments(
                        KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512,
                        named(SCRAM_LISTENER.getName(), SCRAM_LISTENER),
                        List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS, TLS_LISTENER, OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS)
                ),
                arguments(
                        KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512,
                        named(SCRAM_LISTENER_WITH_TLS.getName(), SCRAM_LISTENER_WITH_TLS),
                        List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS, TLS_LISTENER, OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS)
                ),
                arguments(
                        KafkaUserTlsClientAuthentication.TYPE_TLS,
                        named(TLS_LISTENER.getName(), TLS_LISTENER),
                        List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS, SCRAM_LISTENER, SCRAM_LISTENER_WITH_TLS, OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS)
                ),
                arguments(
                        KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL,
                        named(TLS_LISTENER.getName(), TLS_LISTENER),
                        List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS, SCRAM_LISTENER, SCRAM_LISTENER_WITH_TLS, OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("listenerWithMatchingAuthProvider")
    @DisplayName("When a Kafka User with a specific auth is specified in CR, then the listener with matching auth is selected")
    void testListenerWithMatchingAuth(final String kafkaUserAuthType, final GenericKafkaListener expectedListener, final List<GenericKafkaListener> otherListeners) {
        final KafkaReference kafkaReference = new KafkaReference();
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final List<GenericKafkaListener> listeners = new ArrayList<>(otherListeners);
        listeners.add(expectedListener);
        final List<ListenerStatus> listenerStatuses = new ArrayList<>();
        otherListeners.forEach(listener -> listenerStatuses.add(ResourceProvider.getListenerStatus(listener.getName(), BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)));
        listenerStatuses.add(ResourceProvider.getListenerStatus(expectedListener.getName(), BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                listeners,
                listenerStatuses
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, kafkaUserAuthType);
        assertThat(listener.getName()).isEqualTo(expectedListener.getName());
        assertThat(listener.getType()).isEqualTo(expectedListener.getType());
        assertThat(listener.isTls()).isEqualTo(expectedListener.isTls());
        assertThat(listener.getAuthenticationType()).isEqualTo(expectedListener.getAuth().getType());
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    static Stream<Arguments> noListenersMatchingAuthProvider() {
        return Stream.of(
                arguments(
                        KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512,
                        List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS, TLS_LISTENER, OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS)
                ),
                arguments(
                        KafkaUserTlsClientAuthentication.TYPE_TLS,
                        List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS, SCRAM_LISTENER, SCRAM_LISTENER_WITH_TLS, OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS)
                ),
                arguments(
                        KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL,
                        List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS, SCRAM_LISTENER, SCRAM_LISTENER_WITH_TLS, OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("noListenersMatchingAuthProvider")
    @DisplayName("When there is no listener that satisfies the auth type for the chosen Kafka User, then the parsing fails")
    void testNoListenersMatchingAuth(final String kafkaUserAuthType, final List<GenericKafkaListener> listeners) {
        final KafkaReference kafkaReference = new KafkaReference();
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                listeners,
                Collections.emptyList()
        );

        final CustomResourceParseException exception = assertThrows(CustomResourceParseException.class, () -> KafkaParser.getKafkaListener(kafka, spec, kafkaUserAuthType));
        assertThat(exception.getMessage()).isEqualTo("No listeners present in Kafka cluster that match auth requirement.");
    }

    static Stream<Arguments> listenerAndUserIncompatibleProvider() {
        return Stream.of(
                arguments(PLAIN_LISTENER.getName(), KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512),
                arguments(PLAIN_LISTENER.getName(), KafkaUserTlsClientAuthentication.TYPE_TLS),
                arguments(PLAIN_LISTENER.getName(), KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL),
                arguments(PLAIN_LISTENER.getName(), KafkaParser.USER_AUTH_UNDEFINED),
                arguments(PLAIN_LISTENER_WITH_TLS.getName(), KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512),
                arguments(PLAIN_LISTENER_WITH_TLS.getName(), KafkaUserTlsClientAuthentication.TYPE_TLS),
                arguments(PLAIN_LISTENER_WITH_TLS.getName(), KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL),
                arguments(PLAIN_LISTENER_WITH_TLS.getName(), KafkaParser.USER_AUTH_UNDEFINED),

                arguments(SCRAM_LISTENER.getName(), KafkaUserTlsClientAuthentication.TYPE_TLS),
                arguments(SCRAM_LISTENER.getName(), KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL),
                arguments(SCRAM_LISTENER_WITH_TLS.getName(), KafkaUserTlsClientAuthentication.TYPE_TLS),
                arguments(SCRAM_LISTENER_WITH_TLS.getName(), KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL),

                arguments(TLS_LISTENER.getName(), KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512),

                arguments(OAUTH_LISTENER.getName(), KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512),
                arguments(OAUTH_LISTENER.getName(), KafkaUserTlsClientAuthentication.TYPE_TLS),
                arguments(OAUTH_LISTENER.getName(), KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL),
                arguments(OAUTH_LISTENER_WITH_TLS.getName(), KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512),
                arguments(OAUTH_LISTENER_WITH_TLS.getName(), KafkaUserTlsClientAuthentication.TYPE_TLS),
                arguments(OAUTH_LISTENER_WITH_TLS.getName(), KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL)
        );
    }

    @ParameterizedTest
    @MethodSource("listenerAndUserIncompatibleProvider")
    @DisplayName("When the selected listener and the selected KafkaUser have different auth types, then the parsing fails")
    void testListenerAndUserIncompatible(final String selectedListener, final String kafkaUserAuthType) {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setListener(selectedListener);
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS,
                        SCRAM_LISTENER, SCRAM_LISTENER_WITH_TLS,
                        TLS_LISTENER,
                        OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS
                ),
                Collections.emptyList()
        );

        final CustomResourceParseException exception = assertThrows(CustomResourceParseException.class, () -> KafkaParser.getKafkaListener(kafka, spec, kafkaUserAuthType));
        assertThat(exception.getMessage()).isEqualTo(String.format("Provided listener %s and Kafka User do not have compatible authentication configurations.", selectedListener));
    }

    @Test
    @DisplayName("When a Kafka User with no auth is specified in CR and a listener isn't specified, then the parsing fails")
    void testUndefinedUserAuthAndNoListener() {
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(new KafkaReference());

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(PLAIN_LISTENER, PLAIN_LISTENER_WITH_TLS,
                        SCRAM_LISTENER, SCRAM_LISTENER_WITH_TLS,
                        TLS_LISTENER,
                        OAUTH_LISTENER, OAUTH_LISTENER_WITH_TLS
                ),
                Collections.emptyList()
        );

        final CustomResourceParseException exception = assertThrows(CustomResourceParseException.class, () -> KafkaParser.getKafkaListener(kafka, spec, KafkaParser.USER_AUTH_UNDEFINED));
        assertThat(exception.getMessage()).isEqualTo("Cannot match KafkaUser with undefined auth to a Kafka listener, specify the listener in the KafkaAccess CR.");
    }
}
