/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.kafka.access.model.BindingStatus;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessStatus;
import io.strimzi.kafka.access.model.KafkaReference;
import io.strimzi.kafka.access.model.KafkaUserReference;
import org.apache.kafka.clients.CommonClientConfigs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ClassFanOutComplexity")
@EnableKubernetesMockClient(crud = true)
public class KafkaAccessReconcilerTest {

    private static final String NAME = "kafka-access-name";
    private static final String NAMESPACE = "kafka-access-namespace";
    private static final String LISTENER_1 = "listener-1";
    private static final String LISTENER_2 = "listener-2";
    private static final String BOOTSTRAP_HOST = "my-kafka-name.svc";
    private static final String KAFKA_NAME = "my-kafka-name";
    private static final String KAFKA_NAMESPACE = "kafka-namespace";
    private static final int BOOTSTRAP_PORT_9092 = 9092;
    private static final int BOOTSTRAP_PORT_9093 = 9093;

    KubernetesClient client;

    static class MockContext implements Context {

        Secret existingSecret;

        public MockContext() {}

        public MockContext(Secret existingSecret) {
            this.existingSecret = existingSecret;
        }

        @Override
        public Optional<RetryInfo> getRetryInfo() {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> getSecondaryResource(Class<T> aClass, String s) {
            if (this.existingSecret == null) {
                return Optional.empty();
            } else {
                return Optional.of((T) existingSecret);
            }
        }
    }

    @Test
    @DisplayName("When reconcile is called with a KafkaAccess resource, then a secret is created with the " +
            "default information and the bootstrapServer from the Kafka status and the KafkaAccess status is updated")
    void testReconcile() {
        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false)),
                List.of(ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092))
        );
        Crds.kafkaOperation(client).inNamespace(KAFKA_NAMESPACE).withName(KAFKA_NAME).create(kafka);

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference);

        final UpdateControl<KafkaAccess> updateControl = new KafkaAccessReconciler(client).reconcile(kafkaAccess, new MockContext());

        assertThat(updateControl.isUpdateStatus()).isTrue();
        final Optional<String> bindingName = Optional.ofNullable(kafkaAccess.getStatus())
                .map(KafkaAccessStatus::getBinding)
                .map(BindingStatus::getName);
        assertThat(bindingName).isPresent();
        assertThat(bindingName.get()).isEqualTo(NAME);

        final Secret secret = client.secrets().inNamespace(NAMESPACE).withName(NAME).get();
        assertThat(secret).isNotNull();
        assertThat(secret.getType()).isEqualTo("servicebinding.io/kafka");
        final List<OwnerReference> ownerReferences = Optional.ofNullable(secret.getMetadata())
                .map(ObjectMeta::getOwnerReferences)
                .orElse(Collections.emptyList());
        final OwnerReference ownerReference = new OwnerReferenceBuilder()
                .withApiVersion(kafkaAccess.getApiVersion())
                .withName(NAME)
                .withKind(kafkaAccess.getKind())
                .withUid(kafkaAccess.getMetadata().getUid())
                .withBlockOwnerDeletion(false)
                .withController(false)
                .build();
        assertThat(ownerReferences).containsExactly(ownerReference);

        final Base64.Encoder encoder = Base64.getEncoder();
        final Map<String, String> expectedDataEntries = new HashMap<>();
        expectedDataEntries.put("type", encoder.encodeToString("kafka".getBytes(StandardCharsets.UTF_8)));
        expectedDataEntries.put("provider", encoder.encodeToString("strimzi".getBytes(StandardCharsets.UTF_8)));
        expectedDataEntries.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                encoder.encodeToString(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092).getBytes(StandardCharsets.UTF_8)));
        assertThat(secret.getData()).containsAllEntriesOf(expectedDataEntries);
    }

    @Test
    @DisplayName("When reconcile is called with a KafkaAccess resource that references a KafkaUser, then a secret is created with the " +
            "bootstrapServer for the correct listener and the KafkaAccess status is updated")
    void testReconcileWithKafkaUser() {
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
        Crds.kafkaOperation(client).inNamespace(KAFKA_NAMESPACE).withName(KAFKA_NAME).create(kafka);

        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithoutStatus(KAFKA_NAME, KAFKA_NAMESPACE, new KafkaUserScramSha512ClientAuthentication());
        Crds.kafkaUserOperation(client).inNamespace(KAFKA_NAMESPACE).withName(KAFKA_NAME).create(kafkaUser);

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaUserReference kafkaUserReference = ResourceProvider.getKafkaUserReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference, kafkaUserReference);

        final UpdateControl<KafkaAccess> updateControl = new KafkaAccessReconciler(client).reconcile(kafkaAccess, new MockContext());

        assertThat(updateControl.isUpdateStatus()).isTrue();

        final Secret secret = client.secrets().inNamespace(NAMESPACE).withName(NAME).get();
        assertThat(secret).isNotNull();

        final Base64.Encoder encoder = Base64.getEncoder();
        assertThat(secret.getData()).containsEntry(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                encoder.encodeToString(String.format("%s:%s", BOOTSTRAP_HOST, BOOTSTRAP_PORT_9093).getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    @DisplayName("When reconcile is called with a KafkaAccess resource and the referenced Kafka resource is missing, " +
            "then the reconcile loop fails")
    void testReconcileMissingKafka() {
        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference);

        assertThrows(IllegalStateException.class, () -> new KafkaAccessReconciler(client).reconcile(kafkaAccess, new MockContext()));
    }

    @Test
    @DisplayName("When reconcile is called with a KafkaAccess resource and the referenced KafkaUser resource is missing, " +
            "then the reconcile loop fails")
    void testReconcileMissingKafkaUser() {
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
        Crds.kafkaOperation(client).inNamespace(KAFKA_NAMESPACE).withName(KAFKA_NAME).create(kafka);

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaUserReference kafkaUserReference = ResourceProvider.getKafkaUserReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference, kafkaUserReference);

        assertThrows(IllegalStateException.class, () -> new KafkaAccessReconciler(client).reconcile(kafkaAccess, new MockContext()));
    }

    private static Stream<KafkaUserReference> userReferences() {
        return Stream.of(
                ResourceProvider.getUserReference("SpecialUser", KafkaUser.RESOURCE_GROUP, KAFKA_NAME, KAFKA_NAMESPACE),
                ResourceProvider.getUserReference(KafkaUser.RESOURCE_KIND, "special.user.group", KAFKA_NAME, KAFKA_NAMESPACE),
                ResourceProvider.getUserReference("SpecialUser", "special.user.group", KAFKA_NAME, KAFKA_NAMESPACE));
    }

    @ParameterizedTest
    @MethodSource("userReferences")
    @DisplayName("When reconcile is called with a KafkaAccess resource that references a user that has an invalid kind or apiGroup, " +
            "then the reconcile loop fails")
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
        Crds.kafkaOperation(client).inNamespace(KAFKA_NAMESPACE).withName(KAFKA_NAME).create(kafka);

        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithoutStatus(KAFKA_NAME, KAFKA_NAMESPACE, new KafkaUserScramSha512ClientAuthentication());
        Crds.kafkaUserOperation(client).inNamespace(KAFKA_NAMESPACE).withName(KAFKA_NAME).create(kafkaUser);

        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference, userReference);

        assertThrows(IllegalStateException.class, () -> new KafkaAccessReconciler(client).reconcile(kafkaAccess, new MockContext()));
    }

    @Test
    @DisplayName("When reconcile is called with a KafkaAccess resource that already has the secret name in the status, then " +
            "the KafkaAccess status is not updated")
    void testReconcileWithExistingStatusBinding() {
        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference);
        kafkaAccess.setStatus(ResourceProvider.getKafkaAccessStatus(NAME));
        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false)),
                List.of(ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092))
        );
        Crds.kafkaOperation(client).inNamespace(KAFKA_NAMESPACE).withName(KAFKA_NAME).create(kafka);
        final Secret secret = ResourceProvider.getEmptyKafkaAccessSecret(NAME, NAMESPACE, NAME);
        client.secrets().inNamespace(NAMESPACE).withName(NAME).create(secret);

        final KafkaAccessReconciler reconciler = new KafkaAccessReconciler(client);
        final UpdateControl<KafkaAccess> updateControl = reconciler.reconcile(kafkaAccess,
                new MockContext(secret)
        );

        assertThat(updateControl.isUpdateStatus()).isFalse();
    }

    @Test
    @DisplayName("When reconcile is called with a KafkaAccess resource that already has the secret created, then " +
            "the secret is only updated and not completely replaced")
    void testReconcileWithExistingSecret() {
        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME, KAFKA_NAMESPACE);
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(NAME, NAMESPACE, kafkaReference);
        kafkaAccess.setStatus(ResourceProvider.getKafkaAccessStatus(NAME));
        final Kafka kafka = ResourceProvider.getKafka(
                KAFKA_NAME,
                KAFKA_NAMESPACE,
                List.of(ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false)),
                List.of(ResourceProvider.getListenerStatus(LISTENER_1, BOOTSTRAP_HOST, BOOTSTRAP_PORT_9092))
        );
        Crds.kafkaOperation(client).inNamespace(KAFKA_NAMESPACE).withName(KAFKA_NAME).create(kafka);
        final Secret secret = ResourceProvider.getEmptyKafkaAccessSecret(NAME, NAMESPACE, NAME);
        final Map<String, String> customAnnotation = new HashMap<>();
        customAnnotation.put("my-custom", "annotation");
        secret.setMetadata(new ObjectMetaBuilder(secret.getMetadata()).addToAnnotations(customAnnotation).build());
        client.secrets().inNamespace(NAMESPACE).withName(NAME).create(secret);

        final KafkaAccessReconciler reconciler = new KafkaAccessReconciler(client);
        reconciler.reconcile(kafkaAccess, new MockContext(secret));

        final Secret updatedSecret = client.secrets().inNamespace(NAMESPACE).withName(NAME).get();
        final Base64.Encoder encoder = Base64.getEncoder();
        assertThat(updatedSecret.getData()).contains(entry("type", encoder.encodeToString("kafka".getBytes(StandardCharsets.UTF_8))),
                entry("provider", encoder.encodeToString("strimzi".getBytes(StandardCharsets.UTF_8))));
        assertThat(updatedSecret.getMetadata().getAnnotations()).containsAllEntriesOf(customAnnotation);
    }

}
