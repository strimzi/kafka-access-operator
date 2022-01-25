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
import io.strimzi.kafka.access.model.BindingStatus;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@EnableKubernetesMockClient(crud = true)
public class KafkaAccessReconcilerTest {

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
            "default information and the KafkaAccess status is updated")
    void testReconcile() {
        final String name = "kafka-access-name";
        final String namespace = "kafka-access-namespace";
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccess(name, namespace);

        final KafkaAccessReconciler reconciler = new KafkaAccessReconciler(client);
        final UpdateControl<KafkaAccess> updateControl = reconciler.reconcile(kafkaAccess, new MockContext());

        assertThat(updateControl.isUpdateStatus()).isTrue();
        final Optional<String> bindingName = Optional.ofNullable(kafkaAccess.getStatus())
                .map(KafkaAccessStatus::getBinding)
                .map(BindingStatus::getName);
        assertThat(bindingName).isPresent();
        assertThat(bindingName.get()).isEqualTo(name);

        final Secret secret = client.secrets().inNamespace(namespace).withName(name).get();
        assertThat(secret).isNotNull();
        assertThat(secret.getType()).isEqualTo("servicebinding.io/kafka");
        final Base64.Encoder encoder = Base64.getEncoder();
        assertThat(secret.getData()).contains(entry("type", encoder.encodeToString("kafka".getBytes(StandardCharsets.UTF_8))),
                entry("provider", encoder.encodeToString("strimzi".getBytes(StandardCharsets.UTF_8))));

        final List<OwnerReference> ownerReferences = Optional.ofNullable(secret.getMetadata())
                .map(ObjectMeta::getOwnerReferences)
                .orElse(Collections.emptyList());
        final OwnerReference ownerReference = new OwnerReferenceBuilder()
                .withApiVersion(kafkaAccess.getApiVersion())
                .withName(name)
                .withKind(kafkaAccess.getKind())
                .withUid(kafkaAccess.getMetadata().getUid())
                .withBlockOwnerDeletion(false)
                .withController(false)
                .build();
        assertThat(ownerReferences).containsExactly(ownerReference);
    }

    @Test
    @DisplayName("When reconcile is called with a KafkaAccess resource that already has the secret name in the status, then " +
            "the KafkaAccess status is not updated")
    void testReconcileWithExistingStatusBinding() {
        final String name = "kafka-access-name";
        final String namespace = "kafka-access-namespace";
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccessWithStatus(name, namespace, name);
        final Secret secret = ResourceProvider.getEmptyKafkaAccessSecret(name, namespace, name);
        client.secrets().inNamespace(namespace).withName(name).create(secret);

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
        final String name = "kafka-access-name";
        final String namespace = "kafka-access-namespace";
        final KafkaAccess kafkaAccess = ResourceProvider.getKafkaAccessWithStatus(name, namespace, name);
        final Secret secret = ResourceProvider.getEmptyKafkaAccessSecret(name, namespace, name);
        final Map<String, String> customAnnotation = new HashMap<>();
        customAnnotation.put("my-custom", "annotation");
        secret.setMetadata(new ObjectMetaBuilder(secret.getMetadata()).addToAnnotations(customAnnotation).build());
        client.secrets().inNamespace(namespace).withName(name).create(secret);

        final KafkaAccessReconciler reconciler = new KafkaAccessReconciler(client);
        reconciler.reconcile(kafkaAccess, new MockContext(secret));

        final Secret updatedSecret = client.secrets().inNamespace(namespace).withName(name).get();
        final Base64.Encoder encoder = Base64.getEncoder();
        assertThat(updatedSecret.getData()).contains(entry("type", encoder.encodeToString("kafka".getBytes(StandardCharsets.UTF_8))),
                entry("provider", encoder.encodeToString("strimzi".getBytes(StandardCharsets.UTF_8))));
        assertThat(updatedSecret.getMetadata().getAnnotations()).containsAllEntriesOf(customAnnotation);
    }

}
