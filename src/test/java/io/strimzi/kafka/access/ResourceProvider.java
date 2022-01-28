/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.KafkaUserBuilder;
import io.strimzi.api.kafka.model.KafkaUserSpec;
import io.strimzi.api.kafka.model.KafkaUserSpecBuilder;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.api.kafka.model.status.KafkaStatus;
import io.strimzi.api.kafka.model.status.KafkaStatusBuilder;
import io.strimzi.api.kafka.model.status.KafkaUserStatus;
import io.strimzi.api.kafka.model.status.ListenerAddress;
import io.strimzi.api.kafka.model.status.ListenerAddressBuilder;
import io.strimzi.api.kafka.model.status.ListenerStatus;
import io.strimzi.api.kafka.model.status.ListenerStatusBuilder;
import io.strimzi.kafka.access.internal.KafkaAccessParser;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaReference;
import io.strimzi.kafka.access.model.KafkaUserReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"ClassDataAbstractionCoupling", "ClassFanOutComplexity"})
public class ResourceProvider {

    public static KafkaAccess getKafkaAccess(final String kafkaAccessName, final String kafkaAccessNamespace) {
        final ObjectMeta metadata = new ObjectMeta();
        metadata.setName(kafkaAccessName);
        metadata.setNamespace(kafkaAccessNamespace);
        final KafkaAccess kafkaAccess = new KafkaAccess();
        kafkaAccess.setMetadata(metadata);
        return kafkaAccess;
    }

    public static KafkaAccess getKafkaAccess(final String kafkaAccessName, final String kafkaAccessNamespace, final KafkaReference kafkaReference) {
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);
        final KafkaAccess kafkaAccess = getKafkaAccess(kafkaAccessName, kafkaAccessNamespace);
        kafkaAccess.setSpec(spec);
        return kafkaAccess;
    }

    public static KafkaAccess getKafkaAccess(final String kafkaAccessName, final String kafkaAccessNamespace, final KafkaReference kafkaReference, final KafkaUserReference kafkaUserReference) {
        final KafkaAccessSpec spec = new KafkaAccessSpec();
        spec.setKafka(kafkaReference);
        spec.setUser(kafkaUserReference);
        final KafkaAccess kafkaAccess = getKafkaAccess(kafkaAccessName, kafkaAccessNamespace);
        kafkaAccess.setSpec(spec);
        return kafkaAccess;
    }

    public static Secret getEmptyKafkaAccessSecret(String secretName, String secretNamespace, String kafkaAccessName) {
        final Map<String, String> labels = new HashMap<>();
        labels.put(KafkaAccessParser.MANAGED_BY_LABEL_KEY, KafkaAccessParser.KAFKA_ACCESS_LABEL_VALUE);
        final OwnerReference ownerReference = new OwnerReference();
        ownerReference.setName(kafkaAccessName);
        ownerReference.setKind(KafkaAccess.KIND);
        return new SecretBuilder()
                .withNewMetadata()
                    .withName(secretName)
                    .withNamespace(secretNamespace)
                    .withLabels(labels)
                    .withOwnerReferences(ownerReference)
                .endMetadata()
                .build();
    }

    public static Secret getStrimziSecret(final String secretName, final String secretNamespace, final String kafkaInstanceName) {
        final Map<String, String> labels = new HashMap<>();
        labels.put(KafkaAccessParser.MANAGED_BY_LABEL_KEY, KafkaAccessParser.STRIMZI_CLUSTER_LABEL_VALUE);
        labels.put(KafkaAccessParser.INSTANCE_LABEL_KEY, kafkaInstanceName);
        return new SecretBuilder()
                .withNewMetadata()
                    .withName(secretName)
                    .withNamespace(secretNamespace)
                    .withLabels(labels)
                .endMetadata()
                .build();
    }

    public static KafkaReference getKafkaReference(final String kafkaName, final String kafkaNamespace) {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setName(kafkaName);
        Optional.ofNullable(kafkaNamespace).ifPresent(kafkaReference::setNamespace);
        return kafkaReference;
    }

    public static KafkaReference getKafkaReferenceWithListener(final String kafkaName, final String listenerName, final String kafkaNamespace) {
        final KafkaReference kafkaReference = new KafkaReference();
        kafkaReference.setName(kafkaName);
        kafkaReference.setListener(listenerName);
        Optional.ofNullable(kafkaNamespace).ifPresent(kafkaReference::setNamespace);
        return kafkaReference;
    }

    public static KafkaUserReference getKafkaUserReference(final String kafkaUserName, final String kafkaUserNamespace) {
        final KafkaUserReference kafkaUserReference = new KafkaUserReference();
        kafkaUserReference.setKind(KafkaUser.RESOURCE_KIND);
        kafkaUserReference.setApiGroup(KafkaUser.RESOURCE_GROUP);
        kafkaUserReference.setName(kafkaUserName);
        Optional.ofNullable(kafkaUserNamespace).ifPresent(kafkaUserReference::setNamespace);
        return kafkaUserReference;
    }

    public static KafkaUserReference getUserReference(final String kind, final String apiGroup, final String kafkaUserName, final String kafkaUserNamespace) {
        final KafkaUserReference userReference = new KafkaUserReference();
        userReference.setKind(kind);
        userReference.setApiGroup(apiGroup);
        userReference.setName(kafkaUserName);
        Optional.ofNullable(kafkaUserNamespace).ifPresent(userReference::setNamespace);
        return userReference;
    }

    public static Kafka getKafka(final String name, final String namespace) {
        return new KafkaBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace)
                .endMetadata()
                .build();
    }

    public static Kafka getKafka(final String clusterName, final List<GenericKafkaListener> listeners, List<ListenerStatus> listenerStatuses) {
        final ObjectMeta metadata = new ObjectMetaBuilder()
                .withName(clusterName)
                .build();
        return getKafka(metadata, listeners, listenerStatuses);
    }

    public static Kafka getKafka(final String clusterName, final String namespace, final List<GenericKafkaListener> listeners, List<ListenerStatus> listenerStatuses) {
        final ObjectMeta metadata = new ObjectMetaBuilder()
                .withName(clusterName)
                .withNamespace(namespace)
                .build();
        return getKafka(metadata, listeners, listenerStatuses);
    }

    private static Kafka getKafka(final ObjectMeta metadata, final List<GenericKafkaListener> listeners, List<ListenerStatus> listenerStatuses) {
        final KafkaStatus kafkaStatus = new KafkaStatusBuilder()
                .withListeners(listenerStatuses)
                .build();
        return new KafkaBuilder()
                .withMetadata(metadata)
                .withNewSpec()
                .withNewKafka()
                .withListeners(listeners)
                .endKafka()
                .endSpec()
                .withStatus(kafkaStatus)
                .build();
    }

    public static GenericKafkaListener getListener(final String name, final KafkaListenerType type, final boolean tls) {
        return new GenericKafkaListenerBuilder()
                .withName(name)
                .withType(type)
                .withTls(tls)
                .build();
    }

    public static GenericKafkaListener getListener(final String name, final KafkaListenerType type, final boolean tls, final KafkaListenerAuthentication authentication) {
        return new GenericKafkaListenerBuilder()
                .withName(name)
                .withType(type)
                .withTls(tls)
                .withAuth(authentication)
                .build();
    }

    public static ListenerStatus getListenerStatus(final String name, final String bootstrapHost, final int bootstrapPort) {
        final ListenerAddress listenerAddress = new ListenerAddressBuilder()
                .withHost(bootstrapHost)
                .withPort(bootstrapPort)
                .build();
        return new ListenerStatusBuilder()
                .withName(name)
                .withAddresses(List.of(listenerAddress))
                .build();
    }

    public static KafkaUser getKafkaUser(final String name, final String namespace) {
        return new KafkaUserBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .build();
    }

    public static KafkaUser getKafkaUser(final String name, final String namespace, final KafkaUserAuthentication authentication) {
        final KafkaUserSpec spec = Optional.ofNullable(authentication)
                .map(auth -> new KafkaUserSpecBuilder().withAuthentication(authentication).build())
                .orElse(new KafkaUserSpecBuilder().build());
        return new KafkaUserBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withSpec(spec)
                .build();
    }

    public static KafkaUser getKafkaUserWithStatus(final String secretName, final String username, final KafkaUserAuthentication authentication) {
        final KafkaUserSpec spec = Optional.ofNullable(authentication)
                .map(auth -> new KafkaUserSpecBuilder().withAuthentication(authentication).build())
                .orElse(new KafkaUserSpecBuilder().build());
        final KafkaUserStatus status = new KafkaUserStatus();
        Optional.ofNullable(secretName).ifPresent(status::setSecret);
        Optional.ofNullable(username).ifPresent(status::setUsername);
        return new KafkaUserBuilder()
                .withSpec(spec)
                .withStatus(status)
                .build();
    }

    public static KafkaUser getKafkaUserWithStatus(final String name, final String namespace, final String secretName, final String username, final KafkaUserAuthentication authentication) {
        final KafkaUserSpec spec = Optional.ofNullable(authentication)
                .map(auth -> new KafkaUserSpecBuilder().withAuthentication(authentication).build())
                .orElse(new KafkaUserSpecBuilder().build());
        final KafkaUserStatus status = new KafkaUserStatus();
        Optional.ofNullable(secretName).ifPresent(status::setSecret);
        Optional.ofNullable(username).ifPresent(status::setUsername);
        return new KafkaUserBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withSpec(spec)
                .withStatus(status)
                .build();
    }
}
