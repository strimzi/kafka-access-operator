/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.KafkaUserBuilder;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.api.kafka.model.status.KafkaStatus;
import io.strimzi.api.kafka.model.status.KafkaStatusBuilder;
import io.strimzi.api.kafka.model.status.ListenerAddress;
import io.strimzi.api.kafka.model.status.ListenerAddressBuilder;
import io.strimzi.api.kafka.model.status.ListenerStatus;
import io.strimzi.api.kafka.model.status.ListenerStatusBuilder;
import io.strimzi.kafka.access.internal.KafkaAccessMapper;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessBuilder;
import io.strimzi.kafka.access.model.KafkaReference;
import io.strimzi.kafka.access.model.KafkaReferenceBuilder;
import io.strimzi.kafka.access.model.KafkaUserReference;
import io.strimzi.kafka.access.model.KafkaUserReferenceBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ClassDataAbstractionCoupling", "ClassFanOutComplexity"})
public class ResourceProvider {

    public static KafkaAccess getKafkaAccess(final String kafkaAccessName, final String kafkaAccessNamespace) {
        return new KafkaAccessBuilder()
            .withNewMetadata()
                .withNamespace(kafkaAccessNamespace)
                .withName(kafkaAccessName)
            .endMetadata()
            .build();
    }

    public static KafkaAccess getKafkaAccess(final String kafkaAccessName, final String kafkaAccessNamespace, final KafkaReference kafkaReference) {
        return new KafkaAccessBuilder(getKafkaAccess(kafkaAccessName, kafkaAccessNamespace))
            .withNewSpec()
                .withKafka(kafkaReference)
            .endSpec()
            .build();
    }

    public static KafkaAccess getKafkaAccess(final String kafkaAccessName, final String kafkaAccessNamespace, final KafkaReference kafkaReference, final KafkaUserReference kafkaUserReference) {
        return new KafkaAccessBuilder(getKafkaAccess(kafkaAccessName, kafkaAccessNamespace))
            .withNewSpec()
                .withKafka(kafkaReference)
                .withUser(kafkaUserReference)
            .endSpec()
            .build();
    }

    public static Secret getEmptyKafkaAccessSecret(String secretName, String secretNamespace, String kafkaAccessName) {
        return new SecretBuilder()
            .withNewMetadata()
                .withName(secretName)
                .withNamespace(secretNamespace)
                .addToLabels(KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.KAFKA_ACCESS_LABEL_VALUE)
                .addNewOwnerReference()
                    .withName(kafkaAccessName)
                    .withKind(KafkaAccess.KIND)
                .endOwnerReference()
            .endMetadata()
            .build();
    }

    public static Secret getStrimziSecret(final String secretName, final String secretNamespace, final String kafkaInstanceName) {
        final Map<String, String> labels = new HashMap<>();
        labels.put(KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.STRIMZI_CLUSTER_LABEL_VALUE);
        labels.put(KafkaAccessMapper.INSTANCE_LABEL_KEY, kafkaInstanceName);
        return buildSecret(secretName, secretNamespace, labels);
    }

    public static Secret getStrimziUserSecret(final String secretName, final String secretNamespace, final String kafkaInstanceName) {
        final Map<String, String> labels = new HashMap<>();
        labels.put(KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.STRIMZI_USER_LABEL_VALUE);
        labels.put(KafkaAccessMapper.STRIMZI_CLUSTER_LABEL_KEY, kafkaInstanceName);
        return buildSecret(secretName, secretNamespace, labels);
    }

    private static Secret buildSecret(final String name, final String namespace, final Map<String, String> labels) {
        return new SecretBuilder()
            .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(labels)
            .endMetadata()
            .build();
    }

    public static KafkaReference getKafkaReference(final String kafkaName, final String kafkaNamespace) {
        return new KafkaReferenceBuilder()
            .withName(kafkaName)
            .withNamespace(kafkaNamespace)
            .build();
    }

    public static KafkaReference getKafkaReferenceWithListener(final String kafkaName, final String listenerName, final String kafkaNamespace) {
        return new KafkaReferenceBuilder(getKafkaReference(kafkaName, kafkaNamespace))
            .withListener(listenerName)
            .build();
    }

    public static KafkaUserReference getKafkaUserReference(final String kafkaUserName, final String kafkaUserNamespace) {
        return getUserReference(KafkaUser.RESOURCE_KIND, KafkaUser.RESOURCE_GROUP, kafkaUserName, kafkaUserNamespace);
    }

    public static KafkaUserReference getUserReference(final String kind, final String apiGroup, final String kafkaUserName, final String kafkaUserNamespace) {
        return new KafkaUserReferenceBuilder()
            .withName(kafkaUserName)
            .withNamespace(kafkaUserNamespace)
            .withKind(kind)
            .withApiGroup(apiGroup)
            .build();
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
        return new KafkaUserBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .withAuthentication(authentication)
                .endSpec()
                .build();
    }

    public static KafkaUser getKafkaUserWithStatus(final String secretName, final String username, final KafkaUserAuthentication authentication) {
        return new KafkaUserBuilder()
                .withNewSpec()
                    .withAuthentication(authentication)
                .endSpec()
                .withNewStatus()
                    .withSecret(secretName)
                    .withUsername(username)
                .endStatus()
                .build();
    }

    public static KafkaUser getKafkaUserWithStatus(final String name, final String namespace, final String secretName, final String username, final KafkaUserAuthentication authentication) {
        return new KafkaUserBuilder(getKafkaUserWithStatus(secretName, username, authentication))
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace)
                .endMetadata()
                .build();
    }
}
