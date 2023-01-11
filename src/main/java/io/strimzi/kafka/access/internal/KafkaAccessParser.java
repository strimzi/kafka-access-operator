/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaUserReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KafkaAccessParser {

    public static final String MANAGED_BY_LABEL_KEY = "app.kubernetes.io/managed-by";
    public static final String INSTANCE_LABEL_KEY = "app.kubernetes.io/instance";
    public static final String STRIMZI_CLUSTER_LABEL_VALUE = "strimzi-cluster-operator";
    public static final String KAFKA_ACCESS_LABEL_VALUE = "kafka-access-operator";

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAccessParser.class);

    /**
     * Filters the stream of KafkaAccess objects to find only the ones that reference the provided Kafka resource.
     *
     * @param kafkaAccessList    Stream of KafkaAccess objects in the current cache
     * @param kafka              Kafka resource to check for in the KafkaAccess objects
     *
     * @return                   Set of ResourceIDs for the KafkaAccess objects that reference the Kafka resource
     */
    public static Set<ResourceID> getKafkaAccessSetForKafka(final Stream<KafkaAccess> kafkaAccessList, final Kafka kafka) {
        final Optional<String> kafkaName = Optional.ofNullable(kafka.getMetadata()).map(ObjectMeta::getName);
        final Optional<String> kafkaNamespace = Optional.ofNullable(kafka.getMetadata()).map(ObjectMeta::getNamespace);
        if (kafkaName.isEmpty() || kafkaNamespace.isEmpty()) {
            LOGGER.error("getKafkaAccessSetForKafka called with Kafka resource that is missing metadata, returning empty set");
            return Collections.emptySet();
        }
        return getResourceIDsForInstance(kafkaAccessList, kafkaAccess -> {
            // If the KafkaReference omits a namespace, assume the Kafka is in the KafkaAccess namespace
            final String expectedNamespace = Optional.ofNullable(kafkaAccess.getSpec().getKafka().getNamespace())
                    .orElse(Optional.ofNullable(kafkaAccess.getMetadata())
                            .map(ObjectMeta::getNamespace)
                            .orElse(null));
            return kafkaNamespace.get().equals(expectedNamespace) && kafkaName.get().equals(kafkaAccess.getSpec().getKafka().getName());
        });
    }

    /**
     * Filters the stream of KafkaAccess objects to find only the ones that reference the provided KafkaUser resource.
     *
     * @param kafkaAccessList    Stream of KafkaAccess objects in the current cache
     * @param kafkaUser          KafkaUser resource to check for in the KafkaAccess objects
     *
     * @return                   Set of ResourceIDs for the KafkaAccess objects that reference the KafkaUser resource
     */
    public static Set<ResourceID> getKafkaAccessSetForKafkaUser(final Stream<KafkaAccess> kafkaAccessList, final KafkaUser kafkaUser) {
        final Optional<String> kafkaUserName = Optional.ofNullable(kafkaUser.getMetadata()).map(ObjectMeta::getName);
        final Optional<String> kafkaUserNamespace = Optional.ofNullable(kafkaUser.getMetadata()).map(ObjectMeta::getNamespace);
        if (kafkaUserName.isEmpty() || kafkaUserNamespace.isEmpty()) {
            LOGGER.error("getKafkaAccessSetForKafkaUser called with KafkaUser resource that is missing metadata, returning empty set");
            return Collections.emptySet();
        }
        return getResourceIDsForInstance(kafkaAccessList, kafkaAccess -> {
            // If the KafkaReference omits a namespace, assume the Kafka is in the KafkaAccess namespace
            final String expectedNamespace = Optional.ofNullable(kafkaAccess.getSpec().getUser())
                    .map(KafkaUserReference::getNamespace)
                    .orElse(Optional.ofNullable(kafkaAccess.getMetadata())
                            .map(ObjectMeta::getNamespace)
                            .orElse(null));
            return kafkaUserNamespace.get().equals(expectedNamespace) && kafkaUserName.get().equals(kafkaAccess.getSpec().getUser().getName());
        });
    }

    private static Set<ResourceID> getResourceIDsForInstance(final Stream<KafkaAccess> kafkaAccessList, final Function<KafkaAccess, Boolean> filterMatching) {
        return kafkaAccessList
                .filter(filterMatching::apply)
                .map(kafkaAccess -> {
                    final Optional<ObjectMeta> metadata = Optional.ofNullable(kafkaAccess.getMetadata());
                    final Optional<String> kafkaAccessName = metadata.map(ObjectMeta::getName);
                    final Optional<String> kafkaAccessNamespace = metadata.map(ObjectMeta::getNamespace);
                    if (kafkaAccessName.isPresent() && kafkaAccessNamespace.isPresent()) {
                        return new ResourceID(kafkaAccessName.get(), kafkaAccessNamespace.get());
                    } else {
                        LOGGER.error("Found KafkaAccess with matching instance reference, but metadata is missing.");
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Filters the stream of KafkaAccess objects to find only the ones that should be informed that the secret has changed.
     *
     * @param kafkaAccessList    Stream of KafkaAccess objects in the current cache
     * @param secret             Secret to check if it is related to one or more KafkaAccess objects
     *
     * @return                   Set of ResourceIDs for the KafkaAccess objects that reference the Kafka resource
     */
    public static Set<ResourceID> getKafkaAccessResourceIDsForSecret(final Stream<KafkaAccess> kafkaAccessList, final Secret secret) {
        final Set<ResourceID> resourceIDS = new HashSet<>();

        final Optional<String> secretNamespace = Optional.ofNullable(secret.getMetadata())
                .map(ObjectMeta::getNamespace);

        if (secretNamespace.isEmpty()) {
            LOGGER.error("Namespace missing from secret, returning empty list.");
            return resourceIDS;
        }

        final Map<String, String> labels = Optional.ofNullable(secret.getMetadata())
                .map(ObjectMeta::getLabels)
                .orElse(new HashMap<>());

        if (KAFKA_ACCESS_LABEL_VALUE.equals(labels.get(MANAGED_BY_LABEL_KEY))) {
            Optional.ofNullable(secret.getMetadata())
                .map(ObjectMeta::getOwnerReferences)
                .orElse(Collections.emptyList())
                .stream()
                .filter(ownerReference -> KafkaAccess.KIND.equals(ownerReference.getKind()))
                .findFirst()
                .map(OwnerReference::getName)
                .ifPresent(s -> resourceIDS.add(new ResourceID(s, secretNamespace.get())));
        }

        if (STRIMZI_CLUSTER_LABEL_VALUE.equals(labels.get(MANAGED_BY_LABEL_KEY))) {
            Optional.ofNullable(labels.get(INSTANCE_LABEL_KEY))
                .ifPresent(clusterName -> {
                    final Kafka kafka = new KafkaBuilder()
                            .withNewMetadata()
                            .withName(clusterName)
                            .withNamespace(secretNamespace.get())
                            .endMetadata()
                            .build();
                    resourceIDS.addAll(KafkaAccessParser.getKafkaAccessSetForKafka(kafkaAccessList, kafka));
                });
        }

        return resourceIDS;
    }

    /**
     * Finds the KafkaUser that is referenced by this KafkaAccess object.
     *
     * @param kafkaAccess    KafkaAccess object to parse
     *
     * @return               Set of ResourceIDs containing the KafkaUser that is referenced by the KafkaAccess
     */
    public static Set<ResourceID> getKafkaUserForKafkaAccess(final KafkaAccess kafkaAccess) {
        final Set<ResourceID> resourceIDS = new HashSet<>();
        Optional.ofNullable(kafkaAccess.getSpec())
                .map(KafkaAccessSpec::getUser)
                .ifPresent(kafkaUserReference -> {
                    String name = kafkaUserReference.getName();
                    String namespace = Optional.ofNullable(kafkaUserReference.getNamespace())
                            .orElseGet(() -> Optional.ofNullable(kafkaAccess.getMetadata()).map(ObjectMeta::getNamespace).orElse(null));
                    if (name == null || namespace == null) {
                        LOGGER.error("Found KafkaUser for KafkaAccess instance, but metadata is missing.");
                    } else {
                        resourceIDS.add(new ResourceID(name, namespace));
                    }
                });
        return resourceIDS;
    }
}
