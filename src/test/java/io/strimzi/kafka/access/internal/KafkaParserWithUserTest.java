/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.KafkaUserTlsClientAuthentication;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationTls;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.kafka.access.ResourceProvider;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class KafkaParserWithUserTest {

    private static final String CLUSTER_NAME = "my-cluster";
    private static final String LISTENER_1 = "listener-1";
    private static final String LISTENER_2 = "listener-2";
    private static final String LISTENER_3 = "listener-3";
    private static final String BOOTSTRAP_HOST = "my-cluster.svc";
    private static final int BOOTSTRAP_PORT_9092 = 9092;
    private static final int BOOTSTRAP_PORT_9093 = 9093;
    private static final int BOOTSTRAP_PORT_9094 = 9094;

    @Test
    @DisplayName("When listener is specified in CR and it is plain and KafkaUser is specified with no auth, then the listener with no auth is selected")
    void testSpecifiedPlainListenerAndPlainUser() {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setListener(LISTENER_1);
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512())
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, KafkaParser.NONE_AUTH);
        assertThat(listener.getName()).isEqualTo(LISTENER_1);
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isFalse();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaParser.NONE_AUTH);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    @Test
    @DisplayName("When listener is specified in CR and it has SASL auth and KafkaUser is specified with SASL auth, then the listener with SASL auth is selected")
    void testSpecifiedSASLListenerAndSASLUser() {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setListener(LISTENER_1);
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512()),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false)
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512);
        assertThat(listener.getName()).isEqualTo(LISTENER_1);
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isFalse();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    @Test
    @DisplayName("When listener is specified in CR and it has SSL auth and KafkaUser is specified with SSL auth, then the listener with SSL auth is selected")
    void testSpecifiedSSLListenerAndSSLUser() {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setListener(LISTENER_1);
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true, new KafkaListenerAuthenticationTls()),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, true)
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, KafkaUserTlsClientAuthentication.TYPE_TLS);
        assertThat(listener.getName()).isEqualTo(LISTENER_1);
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isTrue();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaListenerAuthenticationTls.TYPE_TLS);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    @Test
    @DisplayName("When a Kafka User with no auth is specified in CR, then the listener with no auth is selected")
    void testKafkaUserWithNoAuth() {
        final KafkaReference kafkaReference = new KafkaReference();
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512())
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, KafkaParser.NONE_AUTH);
        assertThat(listener.getName()).isEqualTo(LISTENER_1);
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isFalse();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaParser.NONE_AUTH);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    @Test
    @DisplayName("When a Kafka User with SASL auth is specified in CR, then the listener with SASL auth is selected")
    void testKafkaUserWithSaslAuth() {
        final KafkaReference kafkaReference = new KafkaReference();
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512()),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false)
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512);
        assertThat(listener.getName()).isEqualTo(LISTENER_1);
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isFalse();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    @Test
    @DisplayName("When a Kafka User with SASL auth is specified in CR and the matching listener has tls enabled, then the listener with SASL auth is selected")
    void testKafkaUserWithSaslAuthAndTls() {
        final KafkaReference kafkaReference = new KafkaReference();
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true, new KafkaListenerAuthenticationScramSha512()),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false)
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512);
        assertThat(listener.getName()).isEqualTo(LISTENER_1);
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isTrue();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    @Test
    @DisplayName("When a Kafka User with TLS auth is specified in CR, then the listener with TLS auth is selected")
    void testKafkaUserWithSSLAuth() {
        final KafkaReference kafkaReference = new KafkaReference();
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true, new KafkaListenerAuthenticationTls()),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, true)
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, KafkaUserTlsClientAuthentication.TYPE_TLS);
        assertThat(listener.getName()).isEqualTo(LISTENER_1);
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isTrue();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaListenerAuthenticationTls.TYPE_TLS);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    static Stream<Arguments> noListenersMatchingAuthProvider() {
        return Stream.of(
                arguments(
                        KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512,
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false)
                ),
                arguments(
                        KafkaUserTlsClientAuthentication.TYPE_TLS,
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false)
                ),
                arguments(
                        KafkaParser.NONE_AUTH,
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true, new KafkaListenerAuthenticationTls()),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512())
                )
        );
    }

    @ParameterizedTest
    @MethodSource("noListenersMatchingAuthProvider")
    @DisplayName("When there is no listener that satisfies the auth type for the chosen Kafka User, then the parsing fails")
    void testNoListenersMatchingAuth(final String kafkaUserAuthType, final GenericKafkaListener listener1, final GenericKafkaListener listener2) {
        final KafkaReference kafkaReference = new KafkaReference();
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(listener1, listener2),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final ParserException exception = assertThrows(ParserException.class, () -> KafkaParser.getKafkaListener(kafka, spec, kafkaUserAuthType));
        assertThat(exception.getMessage()).isEqualTo("No listeners present in Kafka cluster that match auth requirement.");
    }

    static Stream<Arguments> listenerAndUserIncompatibleProvider() {
        return Stream.of(
                arguments(KafkaParser.NONE_AUTH, LISTENER_2),
                arguments(KafkaParser.NONE_AUTH, LISTENER_3),
                arguments(KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512, LISTENER_1),
                arguments(KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512, LISTENER_3),
                arguments(KafkaUserTlsClientAuthentication.TYPE_TLS, LISTENER_1),
                arguments(KafkaUserTlsClientAuthentication.TYPE_TLS, LISTENER_2)
        );
    }

    @ParameterizedTest
    @MethodSource("listenerAndUserIncompatibleProvider")
    @DisplayName("When the selected listener and the selected KafkaUser have different auth types, then the parsing fails")
    void testListenerAndUserIncompatible(final String kafkaUserAuthType, final String selectedListener) {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setListener(selectedListener);
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512()),
                        ResourceProvider.getListener(LISTENER_3, KafkaListenerType.INTERNAL, true, new KafkaListenerAuthenticationTls())
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093),
                        ResourceProvider.getListenerStatus(LISTENER_3, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9094)
                )
        );

        final ParserException exception = assertThrows(ParserException.class, () -> KafkaParser.getKafkaListener(kafka, spec, kafkaUserAuthType));
        assertThat(exception.getMessage()).isEqualTo(String.format("Provided listener %s and Kafka User do not have compatible authentication configurations.", selectedListener));
    }
}
