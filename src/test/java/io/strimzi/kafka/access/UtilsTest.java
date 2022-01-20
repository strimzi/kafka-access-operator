/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {

    static final String ACCESS_NAME_1 = "my-access-1";
    static final String ACCESS_NAME_2 = "my-access-2";
    static final String KAFKA_NAME_1 = "my-kafka-1";
    static final String KAFKA_NAME_2 = "my-kafka-2";
    static final String SECRET_NAME = "my-secret";
    static final String NAMESPACE_1 = "my-namespace-1";
    static final String NAMESPACE_2 = "my-namespace-2";

    @Test
    @DisplayName("When getKafkaAccessResourceIDsForKafkaInstance() is called with a list of two KafkaAccess objects and one " +
            "references the Kafka, then the correct KafkaAccess is returned")
    void testCorrectKafkaAccessReturnedForKafka() {
        final KafkaReference kafkaReference1 = ResourceProvider.getKafkaReference(KAFKA_NAME_1, NAMESPACE_2);
        final KafkaReference kafkaReference2 = ResourceProvider.getKafkaReference(KAFKA_NAME_2, NAMESPACE_2);
        final KafkaAccess kafkaAccess1 = ResourceProvider.getKafkaAccess(ACCESS_NAME_1, NAMESPACE_1, kafkaReference1);
        final KafkaAccess kafkaAccess2 = ResourceProvider.getKafkaAccess(ACCESS_NAME_2, NAMESPACE_1, kafkaReference2);

        final Set<ResourceID> matches = Utils.getKafkaAccessResourceIDsForKafkaInstance(
                Stream.of(kafkaAccess1, kafkaAccess2),
                ResourceProvider.getKafka(KAFKA_NAME_1, NAMESPACE_2));
        assertThat(matches).containsExactly(new ResourceID(ACCESS_NAME_1, NAMESPACE_1));
    }

    @Test
    @DisplayName("When getKafkaAccessResourceIDsForKafkaInstance() is called with a list of two KafkaAccess objects and both " +
            "reference the Kafka, then both KafkaAccess instances are returned")
    void testTwoCorrectKafkaAccessReturnedForKafka() {
        final KafkaReference kafkaReference = ResourceProvider.getKafkaReference(KAFKA_NAME_1, NAMESPACE_2);
        final KafkaAccess kafkaAccess1 = ResourceProvider.getKafkaAccess(ACCESS_NAME_1, NAMESPACE_1, kafkaReference);
        final KafkaAccess kafkaAccess2 = ResourceProvider.getKafkaAccess(ACCESS_NAME_2, NAMESPACE_2, kafkaReference);

        final Set<ResourceID> matches = Utils.getKafkaAccessResourceIDsForKafkaInstance(
                Stream.of(kafkaAccess1, kafkaAccess2),
                ResourceProvider.getKafka(KAFKA_NAME_1, NAMESPACE_2));
        assertThat(matches).containsExactly(new ResourceID(ACCESS_NAME_1, NAMESPACE_1), new ResourceID(ACCESS_NAME_2, NAMESPACE_2));
    }

    @Test
    @DisplayName("When getKafkaAccessResourceIDsForKafkaInstance() is called with a list of two KafkaAccess objects and the " +
            "KafkaAccess doesn't explicitly list the namespace, then the KafkaAccess in the same namespace as the Kafka is returned")
    void testKafkaAccessInMatchingNamespaceReturnedForKafka() {
        final KafkaReference kafkaReferenceNullNamespace = ResourceProvider.getKafkaReference(KAFKA_NAME_1, null);
        final KafkaAccess kafkaAccess1 = ResourceProvider.getKafkaAccess(ACCESS_NAME_1, NAMESPACE_1, kafkaReferenceNullNamespace);
        final KafkaAccess kafkaAccess2 = ResourceProvider.getKafkaAccess(ACCESS_NAME_2, NAMESPACE_2, kafkaReferenceNullNamespace);

        final Set<ResourceID> matches = Utils.getKafkaAccessResourceIDsForKafkaInstance(
                Stream.of(kafkaAccess1, kafkaAccess2),
                ResourceProvider.getKafka(KAFKA_NAME_1, NAMESPACE_1));
        assertThat(matches).containsExactly(new ResourceID(ACCESS_NAME_1, NAMESPACE_1));
    }

    @Test
    @DisplayName("When getKafkaAccessResourceIDsForKafkaInstance() is called with a list of KafkaAccess objects and none " +
            "reference the Kafka, then an empty set is returned")
    void testKafkaAccessNoneMatchKafka() {
        final KafkaReference kafkaReference1 = ResourceProvider.getKafkaReference(KAFKA_NAME_1, NAMESPACE_2);
        final KafkaReference kafkaReference2 = ResourceProvider.getKafkaReference(KAFKA_NAME_2, NAMESPACE_1);
        final KafkaAccess kafkaAccess1 = ResourceProvider.getKafkaAccess(ACCESS_NAME_1, NAMESPACE_1, kafkaReference1);
        final KafkaAccess kafkaAccess2 = ResourceProvider.getKafkaAccess(ACCESS_NAME_2, NAMESPACE_2, kafkaReference2);

        final Set<ResourceID> matches = Utils.getKafkaAccessResourceIDsForKafkaInstance(
                Stream.of(kafkaAccess1, kafkaAccess2),
                ResourceProvider.getKafka(KAFKA_NAME_1, NAMESPACE_1));
        assertThat(matches).isEmpty();
    }

    @Test
    @DisplayName("When getKafkaAccessResourceIDsForSecret() is called with a secret that is managed by a KafkaAccess, " +
            "then the correct KafkaAccess is returned")
    void testCorrectKafkaAccessReturnedForKafkaAccessSecret() {
        final KafkaAccess kafkaAccess1 = ResourceProvider.getKafkaAccess(ACCESS_NAME_1, NAMESPACE_1);
        final KafkaAccess kafkaAccess2 = ResourceProvider.getKafkaAccess(ACCESS_NAME_2, NAMESPACE_1);

        final Set<ResourceID> matches = Utils.getKafkaAccessResourceIDsForSecret(
                Stream.of(kafkaAccess1, kafkaAccess2),
                ResourceProvider.getEmptyKafkaAccessSecret(SECRET_NAME, NAMESPACE_1, ACCESS_NAME_1));
        assertThat(matches).containsExactly(new ResourceID(ACCESS_NAME_1, NAMESPACE_1));
    }

    @Test
    @DisplayName("When getKafkaAccessResourceIDsForSecret() is called with an empty cache and a secret that is managed " +
            "by a KafkaAccess, then the correct KafkaAccess is returned")
    void testCorrectKafkaAccessReturnedForKafkaAccessSecretEmptyCache() {
        final Set<ResourceID> matches = Utils.getKafkaAccessResourceIDsForSecret(
                Stream.of(),
                ResourceProvider.getEmptyKafkaAccessSecret(SECRET_NAME, NAMESPACE_1, ACCESS_NAME_1));
        assertThat(matches).containsExactly(new ResourceID(ACCESS_NAME_1, NAMESPACE_1));
    }

    @Test
    @DisplayName("When getKafkaAccessResourceIDsForSecret() is called with a secret that is managed by Strimzi, " +
            "then the correct KafkaAccess is returned")
    void testCorrectKafkaAccessReturnedForStrimziSecret() {
        final KafkaReference kafkaReference1 = ResourceProvider.getKafkaReference(KAFKA_NAME_1, NAMESPACE_1);
        final KafkaReference kafkaReference2 = ResourceProvider.getKafkaReference(KAFKA_NAME_2, NAMESPACE_1);
        final KafkaAccess kafkaAccess1 = ResourceProvider.getKafkaAccess(ACCESS_NAME_1, NAMESPACE_1, kafkaReference1);
        final KafkaAccess kafkaAccess2 = ResourceProvider.getKafkaAccess(ACCESS_NAME_2, NAMESPACE_1, kafkaReference2);

        final Set<ResourceID> matches = Utils.getKafkaAccessResourceIDsForSecret(
                Stream.of(kafkaAccess1, kafkaAccess2),
                ResourceProvider.getStrimziSecret(SECRET_NAME, NAMESPACE_1, KAFKA_NAME_1));
        assertThat(matches).containsExactly(new ResourceID(ACCESS_NAME_1, NAMESPACE_1));
    }

    @Test
    @DisplayName("When getKafkaAccessResourceIDsForSecret() is called with a secret that is managed by an unknown operator, " +
            "then an empty set is returned")
    void testEmptySetForSecretManagedByUnknown() {
        final KafkaReference kafkaReference1 = ResourceProvider.getKafkaReference(KAFKA_NAME_1, NAMESPACE_1);
        final KafkaReference kafkaReference2 = ResourceProvider.getKafkaReference(KAFKA_NAME_2, NAMESPACE_1);
        final KafkaAccess kafkaAccess1 = ResourceProvider.getKafkaAccess(ACCESS_NAME_1, NAMESPACE_1, kafkaReference1);
        final KafkaAccess kafkaAccess2 = ResourceProvider.getKafkaAccess(ACCESS_NAME_2, NAMESPACE_1, kafkaReference2);

        final Map<String, String> labels = new HashMap<>();
        labels.put(Utils.MANAGED_BY_LABEL_KEY, "unknown");
        final Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName(SECRET_NAME)
                .withNamespace(NAMESPACE_1)
                .withLabels(labels)
                .endMetadata()
                .build();

        final Set<ResourceID> matches = Utils.getKafkaAccessResourceIDsForSecret(
                Stream.of(kafkaAccess1, kafkaAccess2),
                secret);
        assertThat(matches).isEmpty();
    }

    @Test
    @DisplayName("When getKafkaAccessResourceIDsForSecret() is called with a secret that is not managed by any resource, " +
            "then an empty set is returned")
    void testEmptySetForUnmanagedSecret() {
        final KafkaReference kafkaReference1 = ResourceProvider.getKafkaReference(KAFKA_NAME_1, NAMESPACE_1);
        final KafkaReference kafkaReference2 = ResourceProvider.getKafkaReference(KAFKA_NAME_2, NAMESPACE_1);
        final KafkaAccess kafkaAccess1 = ResourceProvider.getKafkaAccess(ACCESS_NAME_1, NAMESPACE_1, kafkaReference1);
        final KafkaAccess kafkaAccess2 = ResourceProvider.getKafkaAccess(ACCESS_NAME_2, NAMESPACE_1, kafkaReference2);

        final Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName(SECRET_NAME)
                .withNamespace(NAMESPACE_1)
                .endMetadata()
                .build();

        final Set<ResourceID> matches = Utils.getKafkaAccessResourceIDsForSecret(
                Stream.of(kafkaAccess1, kafkaAccess2),
                secret);
        assertThat(matches).isEmpty();
    }
}
