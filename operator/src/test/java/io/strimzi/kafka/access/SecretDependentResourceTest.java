/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.event.EventSourceRetriever;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaResources;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.kafka.access.internal.MissingKubernetesResourceException;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaReference;
import io.strimzi.kafka.access.model.KafkaUserReference;
import org.apache.kafka.clients.CommonClientConfigs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.strimzi.kafka.access.Base64Encoder.encodeUtf8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecretDependentResourceTest {

    private static final String NAME = "kafka-access-name";
    private static final String NAMESPACE = "kafka-access-namespace";
    private static final String LISTENER_1 = "listener-1";
    private static final String LISTENER_2 = "listener-2";
    private static final String BOOTSTRAP_HOST = "my-kafka-name.svc";
    private static final String KAFKA_NAME = "my-kafka-name";
    private static final String KAFKA_USER_NAME = "my-kafka-user";
    private static final String KAFKA_USER_SECRET_NAME = "my-user-secret";
    private static final String KAFKA_NAMESPACE = "kafka-namespace";
    private static final int BOOTSTRAP_PORT_9092 = 9092;
    private static final int BOOTSTRAP_PORT_9093 = 9093;


    @Test
    @DisplayName("When secretData is called with a KafkaAccess resource, then the data returned includes the " +
            "default information and the bootstrapServer")
    void testSecretData() {
        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false)),
                List.of(ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092))
        );
        final Context<KafkaAccess> mockContext = mock(Context.class);
        when(mockContext.getSecondaryResource(Kafka.class)).thenReturn(Optional.of(kafka));

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference);

        Map<String, String> data = new SecretDependentResource().desired(kafkaAccess.getSpec(), NAMESPACE, mockContext);
        final Map<String, String> expectedDataEntries = new HashMap<>();
        expectedDataEntries.put("type", encodeUtf8("kafka"));
        expectedDataEntries.put("provider", encodeUtf8("strimzi"));
        expectedDataEntries.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                encodeUtf8(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092)));
        assertThat(data).containsAllEntriesOf(expectedDataEntries);
    }

    @Test
    @DisplayName("When secretData is called with a KafkaAccess resource that references a tls listener, then the data returned includes the " +
            "CA certificate")
    void testSecretDataWithTls() {
        final String cert = encodeUtf8("-----BEGIN CERTIFICATE-----\nMIIFLTCCAx\n-----END CERTIFICATE-----\n");
        final Map<String, String> certSecretData = new HashMap<>();
        certSecretData.put("ca.crt", cert);
//        final Secret certSecret = ResourceProvider.getStrimziSecret(KafkaResources.clusterCaCertificateSecretName(KAFKA_NAME), KAFKA_NAMESPACE, KAFKA_NAME);
//        certSecret.setData(certSecretData);
        // Use SecretBuilder to create the Secret
        final Secret certSecret = new SecretBuilder()
                .withNewMetadata()
                .withName(KafkaResources.clusterCaCertificateSecretName(KAFKA_NAME))
                .withNamespace(KAFKA_NAMESPACE)
                .endMetadata()
                .addToData(certSecretData)
                .build();

        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true)),
                List.of(ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092))
        );
        final Context mockContext = mock(Context.class);
        when(mockContext.getSecondaryResource(Kafka.class)).thenReturn(Optional.of(kafka));

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference);

        final EventSourceRetriever<KafkaAccess> mockEventSourceRetriever = mock(EventSourceRetriever.class);
        final InformerEventSource<Secret, KafkaAccess> mockInformerEventSource = mock(InformerEventSource.class);
        when(mockContext.eventSourceRetriever()).thenReturn(mockEventSourceRetriever);
        when(mockEventSourceRetriever.getEventSourceFor(Secret.class, KafkaAccessReconciler.STRIMZI_SECRET_EVENT_SOURCE)).thenReturn(mockInformerEventSource);
        when(mockInformerEventSource.get(any(ResourceID.class))).thenReturn(Optional.of(certSecret));

        Map<String, String> data = new SecretDependentResource().desired(kafkaAccess.getSpec(), NAMESPACE, mockContext);
        final Map<String, String> expectedDataEntries = new HashMap<>();
        expectedDataEntries.put("type", encodeUtf8("kafka"));
        expectedDataEntries.put("provider", encodeUtf8("strimzi"));
        expectedDataEntries.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                encodeUtf8(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092)));
        expectedDataEntries.put("ssl.truststore.crt", cert);
        assertThat(data).containsAllEntriesOf(expectedDataEntries);
    }

    @Test
    @DisplayName("When secretData is called with a KafkaAccess resource that references a KafkaUser, then the data returned includes the " +
            "bootstrapServer for the listener")
    void testSecretDataWithKafkaUser() {
        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512())),
                List.of(ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093))
        );
        final Context<KafkaAccess> mockContext = mock(Context.class);
        when(mockContext.getSecondaryResource(Kafka.class)).thenReturn(Optional.of(kafka));

        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(KAFKA_NAME, KAFKA_NAMESPACE, KAFKA_USER_SECRET_NAME, "user", new KafkaUserScramSha512ClientAuthentication());
        when(mockContext.getSecondaryResource(KafkaUser.class)).thenReturn(Optional.of(kafkaUser));

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReferenceWithListener(KAFKA_NAME, LISTENER_2, KAFKA_NAMESPACE);
        final KafkaUserReference kafkaUserReference = ResourceProvider.getKafkaUserReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference, kafkaUserReference);

        final EventSourceRetriever<KafkaAccess> mockEventSourceRetriever = mock(EventSourceRetriever.class);
        final InformerEventSource<Secret, KafkaAccess> mockInformerEventSource = mock(InformerEventSource.class);
        when(mockContext.eventSourceRetriever()).thenReturn(mockEventSourceRetriever);
        when(mockEventSourceRetriever.getEventSourceFor(Secret.class, KafkaAccessReconciler.KAFKA_USER_SECRET_EVENT_SOURCE)).thenReturn(mockInformerEventSource);
        when(mockInformerEventSource.get(any(ResourceID.class))).thenReturn(Optional.of(ResourceProvider.getStrimziUserSecret(KAFKA_USER_SECRET_NAME, KAFKA_NAMESPACE, KAFKA_NAME)));

        Map<String, String> data = new SecretDependentResource().desired(kafkaAccess.getSpec(), NAMESPACE, mockContext);
        assertThat(data).containsEntry(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                encodeUtf8(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093))
        );
    }

    @Test
    @DisplayName("When desired is called with a KafkaAccess resource and the referenced Kafka resource is missing, " +
            "then it throws an exception")
    void testDesiredMissingKafka() {
        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference);

        final Context<KafkaAccess> mockContext = mock(Context.class);
        when(mockContext.getSecondaryResource(Kafka.class)).thenReturn(Optional.empty());

        final MissingKubernetesResourceException exception = assertThrows(MissingKubernetesResourceException.class,
                () -> new SecretDependentResource().desired(kafkaAccess.getSpec(), NAMESPACE, mockContext));
        assertThat(exception).hasMessage(String.format("Kafka %s/%s missing", KAFKA_NAMESPACE, KAFKA_NAME));
    }

    @Test
    @DisplayName("When desired is called with a KafkaAccess resource and the referenced KafkaUser resource is missing, " +
            "then it throws an exception")
    void testDesiredMissingKafkaUser() {
        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512())
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );
        final Context<KafkaAccess> mockContext = mock(Context.class);
        when(mockContext.getSecondaryResource(Kafka.class)).thenReturn(Optional.of(kafka));
        when(mockContext.getSecondaryResource(KafkaUser.class)).thenReturn(Optional.empty());

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaUserReference kafkaUserReference = ResourceProvider.getKafkaUserReference(KAFKA_USER_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference, kafkaUserReference);

        final MissingKubernetesResourceException exception = assertThrows(MissingKubernetesResourceException.class,
                () -> new SecretDependentResource().desired(kafkaAccess.getSpec(), NAMESPACE, mockContext));
        assertThat(exception).hasMessage(String.format("KafkaUser %s/%s missing", KAFKA_NAMESPACE, KAFKA_USER_NAME));
    }

    @Test
    @DisplayName("When desired is called with a KafkaAccess resource and the referenced KafkaUser resource's status is missing the secret name, " +
            "then it throws an exception")
    void testDesiredMissingKafkaUserSecretName() {
        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512())
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );
        final Context<KafkaAccess> mockContext = mock(Context.class);
        when(mockContext.getSecondaryResource(Kafka.class)).thenReturn(Optional.of(kafka));

        final KafkaUser kafkaUser = ResourceProvider.getKafkaUser(KAFKA_USER_NAME, KAFKA_NAMESPACE);
        when(mockContext.getSecondaryResource(KafkaUser.class)).thenReturn(Optional.of(kafkaUser));

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaUserReference kafkaUserReference = ResourceProvider.getKafkaUserReference(KAFKA_USER_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference, kafkaUserReference);

        final MissingKubernetesResourceException exception = assertThrows(MissingKubernetesResourceException.class,
                () -> new SecretDependentResource().desired(kafkaAccess.getSpec(), NAMESPACE, mockContext));
        assertThat(exception).hasMessage(String.format("Secret in KafkaUser status %s/%s missing", KAFKA_NAMESPACE, KAFKA_USER_NAME));
    }

    @Test
    @DisplayName("When desired is called with a KafkaAccess resource and the referenced KafkaUser resource's Secret is missing, " +
            "then it throws an exception")
    void testDesiredMissingKafkaUserSecret() {
        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512())
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );
        final Context<KafkaAccess> mockContext = mock(Context.class);
        final EventSourceRetriever<KafkaAccess> mockEventSourceRetriever = mock(EventSourceRetriever.class);
        final InformerEventSource<Secret, KafkaAccess> mockInformerEventSource = mock(InformerEventSource.class);
        when(mockContext.getSecondaryResource(Kafka.class)).thenReturn(Optional.of(kafka));
        when(mockContext.eventSourceRetriever()).thenReturn(mockEventSourceRetriever);
        when(mockEventSourceRetriever.getEventSourceFor(Secret.class, KafkaAccessReconciler.KAFKA_USER_SECRET_EVENT_SOURCE)).thenReturn(mockInformerEventSource);
        when(mockInformerEventSource.get(any(ResourceID.class))).thenReturn(Optional.empty());

        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(KAFKA_USER_NAME, KAFKA_NAMESPACE, KAFKA_USER_SECRET_NAME, "user", new KafkaUserScramSha512ClientAuthentication());
        when(mockContext.getSecondaryResource(KafkaUser.class)).thenReturn(Optional.of(kafkaUser));

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaUserReference kafkaUserReference = ResourceProvider.getKafkaUserReference(KAFKA_USER_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference, kafkaUserReference);

        final MissingKubernetesResourceException exception = assertThrows(MissingKubernetesResourceException.class,
                () -> new SecretDependentResource().desired(kafkaAccess.getSpec(), NAMESPACE, mockContext));
        assertThat(exception).hasMessage(String.format("Secret %s for KafkaUser %s/%s missing", KAFKA_USER_SECRET_NAME, KAFKA_NAMESPACE, KAFKA_USER_NAME));
    }

    private static Stream<KafkaUserReference> userReferences() {
        return Stream.of(
                ResourceProvider.getUserReference("SpecialUser", KafkaUser.RESOURCE_GROUP, KAFKA_NAME, KAFKA_NAMESPACE),
                ResourceProvider.getUserReference(KafkaUser.RESOURCE_KIND, "special.user.group", KAFKA_NAME, KAFKA_NAMESPACE),
                ResourceProvider.getUserReference("SpecialUser", "special.user.group", KAFKA_NAME, KAFKA_NAMESPACE));
    }

    @ParameterizedTest
    @MethodSource("userReferences")
    @DisplayName("When secretData is called with a KafkaAccess resource that references a user that has an invalid kind or apiGroup, " +
            "then it throws an exception")
    void testInvalidUserReference(KafkaUserReference userReference) {
        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(
                        ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false),
                        ResourceProvider.getListener(LISTENER_2, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512())
                ),
                List.of(
                        ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092),
                        ResourceProvider.getListenerStatus(LISTENER_2, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093)
                )
        );
        final Context<KafkaAccess> mockContext = mock(Context.class);
        when(mockContext.getSecondaryResource(Kafka.class)).thenReturn(Optional.of(kafka));

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference, userReference);
        final IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new SecretDependentResource().desired(kafkaAccess.getSpec(), NAMESPACE, mockContext));
        assertThat(exception).hasMessage("User kind must be KafkaUser and apiGroup must be kafka.strimzi.io");
    }
}
