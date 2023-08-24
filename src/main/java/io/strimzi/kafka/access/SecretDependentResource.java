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
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.KafkaUserSpec;
import io.strimzi.api.kafka.model.status.KafkaUserStatus;
import io.strimzi.kafka.access.internal.CustomResourceParseException;
import io.strimzi.kafka.access.internal.KafkaListener;
import io.strimzi.kafka.access.internal.KafkaParser;
import io.strimzi.kafka.access.internal.KafkaUserData;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaReference;
import io.strimzi.kafka.access.model.KafkaUserReference;

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
        final Kafka kafka = context.getSecondaryResource(Kafka.class).orElseThrow(illegalStateException("Kafka", kafkaClusterNamespace, kafkaClusterName));
        final Map<String, String> data  = new HashMap<>(commonSecretData);
        final Optional<KafkaUserReference> kafkaUserReference = Optional.ofNullable(spec.getUser());
        if (kafkaUserReference.isPresent()) {
            if (!KafkaUser.RESOURCE_KIND.equals(kafkaUserReference.get().getKind()) || !KafkaUser.RESOURCE_GROUP.equals(kafkaUserReference.get().getApiGroup())) {
                throw new CustomResourceParseException(String.format("User kind must be %s and apiGroup must be %s", KafkaUser.RESOURCE_KIND, KafkaUser.RESOURCE_GROUP));
            }
            final String kafkaUserName = kafkaUserReference.get().getName();
            final String kafkaUserNamespace = Optional.ofNullable(kafkaUserReference.get().getNamespace()).orElse(namespace);
            final KafkaUser kafkaUser = context.getSecondaryResource(KafkaUser.class).orElseThrow(illegalStateException("KafkaUser", kafkaUserNamespace, kafkaUserName));
            final String userSecretName = Optional.ofNullable(kafkaUser.getStatus())
                    .map(KafkaUserStatus::getSecret)
                    .orElseThrow(illegalStateException("Secret in KafkaUser status", kafkaUserNamespace, kafkaUserName));
            final InformerEventSource<Secret, KafkaAccess> kafkaUserSecretEventSource = (InformerEventSource<Secret, KafkaAccess>) context.eventSourceRetriever()
                    .getResourceEventSourceFor(Secret.class, KafkaAccessReconciler.KAFKA_USER_SECRET_EVENT_SOURCE);
            final Secret kafkaUserSecret = kafkaUserSecretEventSource.get(new ResourceID(userSecretName, kafkaUserNamespace))
                    .orElseThrow(illegalStateException(String.format("Secret %s for KafkaUser", userSecretName), kafkaUserNamespace, kafkaUserName));
            data.putAll(secretDataWithUser(spec, kafka, kafkaUser, new KafkaUserData(kafkaUser).withSecret(kafkaUserSecret)));
        } else {
            data.putAll(secretData(spec, kafka));
        }
        return data;
    }

    protected Map<String, String> secretData(final KafkaAccessSpec spec, final Kafka kafka) {
        final Map<String, String> data  = new HashMap<>(commonSecretData);
        final KafkaListener listener;
        try {
            listener = KafkaParser.getKafkaListener(kafka, spec);
        } catch (CustomResourceParseException e) {
            throw new IllegalStateException("Reconcile failed due to ParserException " + e.getMessage());
        }
        data.putAll(listener.getConnectionSecretData());
        return data;
    }

    protected Map<String, String> secretDataWithUser(final KafkaAccessSpec spec, final Kafka kafka, final KafkaUser kafkaUser, final KafkaUserData kafkaUserData) {
        final Map<String, String> data  = new HashMap<>(commonSecretData);
        final KafkaListener listener;
        try {
            final String kafkaUserType = Optional.ofNullable(kafkaUser.getSpec())
                    .map(KafkaUserSpec::getAuthentication)
                    .map(KafkaUserAuthentication::getType)
                    .orElse(KafkaParser.USER_AUTH_UNDEFINED);
            listener = KafkaParser.getKafkaListener(kafka, spec, kafkaUserType);
            data.putAll(kafkaUserData.getConnectionSecretData());
        } catch (CustomResourceParseException e) {
            throw new IllegalStateException("Reconcile failed due to ParserException " + e.getMessage());
        }
        data.putAll(listener.getConnectionSecretData());
        return data;
    }

    private static Supplier<IllegalStateException> illegalStateException(String type, String namespace, String name) {
        return () -> new IllegalStateException(String.format("%s %s/%s missing", type, namespace, name));
    }
}
