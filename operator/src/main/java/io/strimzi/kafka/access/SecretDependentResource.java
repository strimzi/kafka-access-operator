/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaResources;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.KafkaUserSpec;
import io.strimzi.api.kafka.model.status.KafkaUserStatus;
import io.strimzi.kafka.access.internal.CustomResourceParseException;
import io.strimzi.kafka.access.internal.KafkaListener;
import io.strimzi.kafka.access.internal.KafkaParser;
import io.strimzi.kafka.access.internal.KafkaUserData;
import io.strimzi.kafka.access.internal.MissingKubernetesResourceException;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaReference;
import io.strimzi.kafka.access.model.KafkaUserReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Class to represent the data in the Secret created by the operator
 * In future updates this could be updated to implement the
 * Java Operator SDK DependentResource class
 */
public class SecretDependentResource {

    private static final String TYPE_SECRET_KEY = "type";
    private static final String TYPE_SECRET_VALUE = "kafka";
    private static final String PROVIDER_SECRET_KEY = "provider";
    private static final String PROVIDER_SECRET_VALUE = "strimzi";
    private final Map<String, String> commonSecretData = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretDependentResource.class);

    /**
     * Default constructor that initialises the common secret data
     */
    public SecretDependentResource() {
        final Base64.Encoder encoder = Base64.getEncoder();
        commonSecretData.put(TYPE_SECRET_KEY, encoder.encodeToString(TYPE_SECRET_VALUE.getBytes(StandardCharsets.UTF_8)));
        commonSecretData.put(PROVIDER_SECRET_KEY, encoder.encodeToString(PROVIDER_SECRET_VALUE.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * The desired state of the data in the secret
     * @param spec          The spec of the KafkaAccess resource being reconciled
     * @param namespace     The namespace of the KafkaAccess resource being reconciled
     * @param context       The event source context
     * @return              The data for the Secret as a Map
     */
    public Map<String, String> desired(final KafkaAccessSpec spec, final String namespace, final Context<KafkaAccess> context) {
        final KafkaReference kafkaReference = spec.getKafka();
        final String kafkaClusterName = kafkaReference.getName();
        final String kafkaClusterNamespace = Optional.ofNullable(kafkaReference.getNamespace()).orElse(namespace);
        final Kafka kafka = context.getSecondaryResource(Kafka.class).orElseThrow(missingKubernetesResourceException("Kafka", kafkaClusterNamespace, kafkaClusterName));
        final Map<String, String> data  = new HashMap<>(commonSecretData);
        final KafkaListener listener;
        String kafkaUserType = null;
        final Optional<KafkaUserReference> kafkaUserReference = Optional.ofNullable(spec.getUser());
        if (kafkaUserReference.isPresent()) {
            if (!KafkaUser.RESOURCE_KIND.equals(kafkaUserReference.get().getKind()) || !KafkaUser.RESOURCE_GROUP.equals(kafkaUserReference.get().getApiGroup())) {
                throw new IllegalStateException(String.format("User kind must be %s and apiGroup must be %s", KafkaUser.RESOURCE_KIND, KafkaUser.RESOURCE_GROUP));
            }
            final String kafkaUserName = kafkaUserReference.get().getName();
            final String kafkaUserNamespace = Optional.ofNullable(kafkaUserReference.get().getNamespace()).orElse(namespace);
            final KafkaUser kafkaUser = context.getSecondaryResource(KafkaUser.class).orElseThrow(missingKubernetesResourceException("KafkaUser", kafkaUserNamespace, kafkaUserName));
            kafkaUserType = Optional.ofNullable(kafkaUser.getSpec())
                    .map(KafkaUserSpec::getAuthentication)
                    .map(KafkaUserAuthentication::getType)
                    .orElse(KafkaParser.USER_AUTH_UNDEFINED);
            data.putAll(getKafkaUserSecretData(context, kafkaUser, kafkaUserName, kafkaUserNamespace));
        }
        try {
            listener = KafkaParser.getKafkaListener(kafka, spec, kafkaUserType);
        } catch (CustomResourceParseException e) {
            LOGGER.error("Reconcile failed due to ParserException " + e.getMessage());
            throw e;
        }
        if (listener.isTls()) {
            listener.withCaCertSecret(getKafkaCaCertData(context, kafkaClusterName, kafkaClusterNamespace));
        }
        data.putAll(listener.getConnectionSecretData());
        return data;
    }

    private Map<String, String> getKafkaUserSecretData(final Context<KafkaAccess> context, final KafkaUser kafkaUser, final String kafkaUserName, final String kafkaUserNamespace) {
        final String userSecretName = Optional.ofNullable(kafkaUser.getStatus())
                .map(KafkaUserStatus::getSecret)
                .orElseThrow(missingKubernetesResourceException("Secret in KafkaUser status", kafkaUserNamespace, kafkaUserName));
        final InformerEventSource<Secret, KafkaAccess> kafkaUserSecretEventSource = (InformerEventSource<Secret, KafkaAccess>) context.eventSourceRetriever()
                .getResourceEventSourceFor(Secret.class, KafkaAccessReconciler.KAFKA_USER_SECRET_EVENT_SOURCE);
        final Secret kafkaUserSecret = kafkaUserSecretEventSource.get(new ResourceID(userSecretName, kafkaUserNamespace))
                .orElseThrow(missingKubernetesResourceException(String.format("Secret %s for KafkaUser", userSecretName), kafkaUserNamespace, kafkaUserName));
        return new KafkaUserData(kafkaUser).withSecret(kafkaUserSecret).getConnectionSecretData();
    }

    private Map<String, String> getKafkaCaCertData(final Context<KafkaAccess> context, String kafkaClusterName, String kafkaClusterNamespace) {
        final String caCertSecretName = KafkaResources.clusterCaCertificateSecretName(kafkaClusterName);
        final InformerEventSource<Secret, KafkaAccess> strimziSecretEventSource = (InformerEventSource<Secret, KafkaAccess>) context.eventSourceRetriever()
                .getResourceEventSourceFor(Secret.class, KafkaAccessReconciler.STRIMZI_SECRET_EVENT_SOURCE);
        return strimziSecretEventSource.get(new ResourceID(caCertSecretName, kafkaClusterNamespace))
                .map(Secret::getData)
                .orElse(Map.of());
    }

    private static Supplier<MissingKubernetesResourceException> missingKubernetesResourceException(String type, String namespace, String name) {
        return () -> new MissingKubernetesResourceException(String.format("%s %s/%s missing", type, namespace, name));
    }
}
