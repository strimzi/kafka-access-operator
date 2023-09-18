/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationTls;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.kafka.access.ResourceProvider;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KafkaParserTest {

    private static final String CLUSTER_NAME = "my-cluster";
    private static final String LISTENER_1 = "listener-1";
    private static final String LISTENER_2 = "listener-2";
    private static final String BOOTSTRAP_HOST = "my-cluster.svc";
    private static final int BOOTSTRAP_PORT_9092 = 9092;
    private static final int BOOTSTRAP_PORT_9093 = 9093;
    private static final int BOOTSTRAP_PORT_9094 = 9094;

    @Test
    @DisplayName("When listener is specified in CR, then that listener is chosen")
    void testSpecifiedPlainListener() {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setListener(LISTENER_1);
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, true)
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, null);
        assertThat(listener.getName()).isEqualTo(LISTENER_1);
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isFalse();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaParser.LISTENER_AUTH_NONE);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    @Test
    @DisplayName("When listener is not specified in CR and there are internal and external listeners, then the internal listener is chosen")
    void testInternalListener() {
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(new KafkaReference());

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.ROUTE, false)
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, null);
        assertThat(listener.getName()).isEqualTo(LISTENER_1);
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isTrue();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaParser.LISTENER_AUTH_NONE);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092));
    }

    @Test
    @DisplayName("When listener is not specified in CR and there are two internal listeners, then the internal listener with the first name alphabetically is chosen")
    void testMultipleInternalListeners() {
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(new KafkaReference());

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener("a", KafkaListenerType.ROUTE, true),
                        ResourceProvider.getListener("b", KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512()),
                        ResourceProvider.getListener("c", KafkaListenerType.INTERNAL, false)
                ),
                List.of(
                        ResourceProvider.getListenerStatus("a", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus("b", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093),
                        ResourceProvider.getListenerStatus("c", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9094)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, null);
        assertThat(listener.getName()).isEqualTo("b");
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.INTERNAL);
        assertThat(listener.isTls()).isFalse();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093));
    }

    @Test
    @DisplayName("When listener is not specified in CR and there are two external listeners, then the external listener with the first name alphabetically is chosen")
    void testMultipleExternalListeners() {
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(new KafkaReference());

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener("b", KafkaListenerType.ROUTE, false),
                        ResourceProvider.getListener("a", KafkaListenerType.ROUTE, true, new KafkaListenerAuthenticationTls())
                ),
                List.of(
                        ResourceProvider.getListenerStatus("b", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus("a", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final KafkaListener listener = KafkaParser.getKafkaListener(kafka, spec, null);
        assertThat(listener.getName()).isEqualTo("a");
        assertThat(listener.getType()).isEqualTo(KafkaListenerType.ROUTE);
        assertThat(listener.isTls()).isTrue();
        assertThat(listener.getAuthenticationType()).isEqualTo(KafkaListenerAuthenticationTls.TYPE_TLS);
        assertThat(listener.getBootstrapServer()).isEqualTo(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093));
    }

    @Test
    @DisplayName("When listener is specified in CR and it is not present in Kafka, then the parsing fails")
    void testSpecifiedListenerMissing() {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setListener(LISTENER_1);
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, true)
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final CustomResourceParseException exception = assertThrows(CustomResourceParseException.class, () -> KafkaParser.getKafkaListener(kafka, spec, null));
        assertThat(exception.getMessage()).isEqualTo(String.format("The specified listener %s is missing from the Kafka resource.", LISTENER_1));
    }

    @Test
    @DisplayName("When listener is specified in CR and the matching Kafka status field is missing, then the parsing fails")
    void testSpecifiedListenerMissingFromStatus() {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setListener(LISTENER_1);
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);

        final Kafka kafka = ResourceProvider.getKafka(
                CLUSTER_NAME,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true)
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );

        final CustomResourceParseException exception = assertThrows(CustomResourceParseException.class, () -> KafkaParser.getKafkaListener(kafka, spec, null));
        assertThat(exception.getMessage()).isEqualTo(String.format("The bootstrap server address for the listener %s is missing from the Kafka resource status.", LISTENER_1));
    }


}
