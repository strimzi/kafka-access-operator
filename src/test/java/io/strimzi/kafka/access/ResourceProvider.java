/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    public static Secret getKafkaAccessSecret(String secretName, String secretNamespace, String kafkaAccessName) {
        final Map<String, String> labels = new HashMap<>();
        labels.put(Utils.MANAGED_BY_LABEL_KEY, Utils.KAFKA_ACCESS_LABEL_VALUE);
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
        labels.put(Utils.MANAGED_BY_LABEL_KEY, Utils.STRIMZI_CLUSTER_LABEL_VALUE);
        labels.put(Utils.INSTANCE_LABEL_KEY, kafkaInstanceName);
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
        return  kafkaReference;
    }

    public static Kafka getKafka(final String name, final String namespace) {
        return new KafkaBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace)
                .endMetadata()
                .build();
    }
}
