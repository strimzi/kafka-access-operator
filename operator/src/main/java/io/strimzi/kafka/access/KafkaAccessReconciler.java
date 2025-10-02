/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.kafka.access.internal.KafkaAccessMapper;
import io.strimzi.kafka.access.internal.MissingKubernetesResourceException;
import io.strimzi.kafka.access.model.BindingStatus;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The custom reconciler of Strimzi Access Operator
 */
@SuppressWarnings("ClassFanOutComplexity")
@ControllerConfiguration
public class KafkaAccessReconciler implements Reconciler<KafkaAccess> {

    private final KubernetesClient kubernetesClient;
    private final SecretDependentResource secretDependentResource;
    private final Map<String, String> commonSecretLabels = new HashMap<>();
    private static final String SECRET_TYPE = "servicebinding.io/kafka";
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAccessReconciler.class);

    /**
     * Name of the event source for Strimzi Secret resources
     */
    public static final String STRIMZI_SECRET_EVENT_SOURCE = "STRIMZI_SECRET_EVENT_SOURCE";

    /**
     * Name of the event source for KafkaUser Secret resources
     */
    public static final String KAFKA_USER_SECRET_EVENT_SOURCE = "KAFKA_USER_SECRET_EVENT_SOURCE";

    /**
     * Name of the event source for KafkaAccess Secret resources
     */
    public static final String KAFKA_ACCESS_SECRET_EVENT_SOURCE = "KAFKA_ACCESS_SECRET_EVENT_SOURCE";

    /**
     * @param kubernetesClient      The Kubernetes client
     */
    public KafkaAccessReconciler(final KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        secretDependentResource = new SecretDependentResource();
        commonSecretLabels.put(KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.KAFKA_ACCESS_LABEL_VALUE);
    }

    /**
     * Does the reconciliation
     *
     * @param kafkaAccess       The KafkaAccess resource model
     * @param context           The Operator SDK context
     *
     * @return                  A new instance of UpdateControl for the particular reconciler actions
     */
    @Override
    public UpdateControl<KafkaAccess> reconcile(final KafkaAccess kafkaAccess, final Context<KafkaAccess> context) {
        final String kafkaAccessName = kafkaAccess.getMetadata().getName();
        final String kafkaAccessNamespace = kafkaAccess.getMetadata().getNamespace();
        LOGGER.info("Reconciling KafkaAccess {}/{}", kafkaAccessNamespace, kafkaAccessName);
        final String secretName = determineSecretName(kafkaAccess);

        createOrUpdateSecret(secretDependentResource.desired(kafkaAccess.getSpec(), kafkaAccessNamespace, context), kafkaAccess, secretName, context);
        deleteOldSecretIfRenamed(kafkaAccess.getStatus(), secretName, kafkaAccessNamespace, kafkaAccessName);

        final KafkaAccessStatus kafkaAccessStatus = Optional.ofNullable(kafkaAccess.getStatus())
                .orElseGet(() -> {
                    final KafkaAccessStatus status = new KafkaAccessStatus();
                    kafkaAccess.setStatus(status);
                    return status;
                });

        kafkaAccessStatus.setBinding(new BindingStatus(secretName));
        kafkaAccessStatus.setReadyCondition(true, "Ready", "Ready");
        // replaces using the old ObservedGenerationAwareStatus method
        kafkaAccessStatus.setObservedGeneration(kafkaAccess.getMetadata().getGeneration());

        return UpdateControl.patchStatus(kafkaAccess);
    }

    private void createOrUpdateSecret(final Map<String, String> data, final KafkaAccess kafkaAccess, final String secretName, final Context<KafkaAccess> context) {
        final String kafkaAccessName = kafkaAccess.getMetadata().getName();
        final String kafkaAccessNamespace = kafkaAccess.getMetadata().getNamespace();

        // Use the Context to get the Secret from the cache. Secret is secondary resource, KafkaAccess is primary resource.
        Optional<Secret> existingSecret = context.getSecondaryResource(Secret.class, KAFKA_ACCESS_SECRET_EVENT_SOURCE);

        if (existingSecret.isPresent()) {
            final Map<String, String> currentData = existingSecret.get().getData();
            if (!data.equals(currentData)) {
                LOGGER.info("Updating existing secret {}/{}", kafkaAccessNamespace, secretName);
                kubernetesClient.secrets()
                        .inNamespace(kafkaAccessNamespace)
                        .withName(secretName)
                        .edit(s -> new SecretBuilder(s).withData(data).build());
            }
        } else {
            LOGGER.info("Creating new secret {}/{}", kafkaAccessNamespace, secretName);
            kubernetesClient
                    .secrets()
                    .inNamespace(kafkaAccessNamespace)
                    .resource(
                            new SecretBuilder()
                                    .withType(SECRET_TYPE)
                                    .withNewMetadata()
                                    .withName(secretName)
                                    .withLabels(commonSecretLabels)
                                    .withOwnerReferences(
                                            new OwnerReferenceBuilder()
                                                    .withApiVersion(kafkaAccess.getApiVersion())
                                                    .withKind(kafkaAccess.getKind())
                                                    .withName(kafkaAccessName)
                                                    .withUid(kafkaAccess.getMetadata().getUid())
                                                    .withBlockOwnerDeletion(false)
                                                    .withController(false)
                                                    .build()
                                    )
                                    .endMetadata()
                                    .withData(data)
                                    .build()
                    )
                    .create();
        }
    }

    /**
     * Prepares the event sources required for triggering the reconciliation. This tells the JOSDK framework which resources the operator needs to watch.
     *
     * @param context       The EventSourceContext for KafkaAccess resource
     *
     * @return              A new map of event sources
     */
    public List<EventSource<?, KafkaAccess>> prepareEventSources(EventSourceContext<KafkaAccess> context) {
        LOGGER.info("Preparing event sources");
        InformerEventSourceConfiguration<Kafka> kafkaEventSource =
                // now using updated in v5: InformerEventSourceConfiguration
                InformerEventSourceConfiguration.from(Kafka.class, KafkaAccess.class)
                        .withSecondaryToPrimaryMapper(kafka -> KafkaAccessMapper.kafkaSecondaryToPrimaryMapper(context.getPrimaryCache().list(), kafka))
                        .withPrimaryToSecondaryMapper(kafkaAccess -> KafkaAccessMapper.kafkaPrimaryToSecondaryMapper((KafkaAccess) kafkaAccess))
                        .build();
        InformerEventSourceConfiguration<KafkaUser> kafkaUserEventSource =
                InformerEventSourceConfiguration.from(KafkaUser.class, KafkaAccess.class)
                        .withSecondaryToPrimaryMapper(kafkaUser -> KafkaAccessMapper.kafkaUserSecondaryToPrimaryMapper(context.getPrimaryCache().list(), kafkaUser))
                        .withPrimaryToSecondaryMapper(kafkaAccess -> KafkaAccessMapper.kafkaUserPrimaryToSecondaryMapper((KafkaAccess) kafkaAccess))
                        .build();
        InformerEventSourceConfiguration<Secret> strimziSecretEventSource =
                InformerEventSourceConfiguration.from(Secret.class, KafkaAccess.class)
                        .withName(STRIMZI_SECRET_EVENT_SOURCE)
                        .withLabelSelector(String.format("%s=%s", KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.STRIMZI_CLUSTER_LABEL_VALUE))
                        .withSecondaryToPrimaryMapper(secret -> KafkaAccessMapper.secretSecondaryToPrimaryMapper(context.getPrimaryCache().list(), secret))
                        .build();
        InformerEventSourceConfiguration<Secret> strimziKafkaUserSecretEventSource =
                InformerEventSourceConfiguration.from(Secret.class, KafkaAccess.class)
                        .withName(KAFKA_USER_SECRET_EVENT_SOURCE)
                        .withLabelSelector(String.format("%s=%s", KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.STRIMZI_USER_LABEL_VALUE))
                        .withSecondaryToPrimaryMapper(secret -> KafkaAccessMapper.secretSecondaryToPrimaryMapper(context.getPrimaryCache().list(), secret))
                        .build();
        InformerEventSourceConfiguration<Secret> kafkaAccessSecretEventSource =
                InformerEventSourceConfiguration.from(Secret.class, KafkaAccess.class)
                        .withName(KAFKA_ACCESS_SECRET_EVENT_SOURCE)
                        .withLabelSelector(String.format("%s=%s", KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.KAFKA_ACCESS_LABEL_VALUE))
                        .withSecondaryToPrimaryMapper(secret -> KafkaAccessMapper.secretSecondaryToPrimaryMapper(context.getPrimaryCache().list(), secret))
                        .build();

        LOGGER.info("Finished preparing event sources");
        return List.of(
                new InformerEventSource<>(kafkaEventSource, context),
                new InformerEventSource<>(kafkaUserEventSource, context),
                new InformerEventSource<>(strimziSecretEventSource, context),
                new InformerEventSource<>(strimziKafkaUserSecretEventSource, context),
                new InformerEventSource<>(kafkaAccessSecretEventSource, context));
    }

    @Override
    public ErrorStatusUpdateControl<KafkaAccess> updateErrorStatus(KafkaAccess kafkaAccess, Context<KafkaAccess> context, Exception e) {
        final KafkaAccessStatus status = Optional.ofNullable(kafkaAccess.getStatus())
                .orElseGet(() -> {
                    final KafkaAccessStatus newStatus = new KafkaAccessStatus();
                    kafkaAccess.setStatus(newStatus);
                    return newStatus;
                });
        String reason = null;
        if (e instanceof MissingKubernetesResourceException) {
            reason = "MissingKubernetesResource";
        } else if (e instanceof IllegalStateException) {
            reason = "InvalidUserKind";
        }
        status.setReadyCondition(false, e.getMessage(), reason);

        return ErrorStatusUpdateControl.patchStatus(kafkaAccess);
    }

    /**
     * Determines the name of the Kubernetes Secret based on the KafkaAccess resource.
     * If kafkaAccess.spec.secretName is provided and not empty, it is used.
     * Otherwise, the name of the KafkaAccess resource is used as the default.
     *
     * @param kafkaAccess The KafkaAccess custom resource.
     * @return The determined secret name.
     */
    private String determineSecretName(final KafkaAccess kafkaAccess) {
        final String userProvidedSecretName = kafkaAccess.getSpec().getSecretName();
        final String secretName;

        if (userProvidedSecretName != null && !userProvidedSecretName.isEmpty()) {
            secretName = userProvidedSecretName;
            LOGGER.debug("Determined secret name: '{}' (user-provided)", secretName);
        } else {
            secretName = kafkaAccess.getMetadata().getName();
            LOGGER.debug("Determined secret name: '{}' (default from KafkaAccess name)", secretName);
        }
        return secretName;
    }

    private void deleteOldSecretIfRenamed(final KafkaAccessStatus status, final String newSecretName, final String namespace, final String kafkaAccessName) {
        String oldSecretName = (status != null && status.getBinding() != null) ? status.getBinding().getName() : null;

        if (oldSecretName == null || newSecretName.equals(oldSecretName)) {
            return;
        }

        LOGGER.info("Deleting old secret '{}' for KafkaAccess {}/{}.",
                oldSecretName, namespace, kafkaAccessName);

        try {
            kubernetesClient.secrets()
                    .inNamespace(namespace)
                    .withName(oldSecretName)
                    .delete();
        } catch (KubernetesClientException e) {
            LOGGER.error("Encountered error when deleting old secret '{}' for KafkaAccess {}/{}. Secret must be deleted manually. Exception: {}",
                    oldSecretName, namespace, kafkaAccessName, e.getMessage());
        }
    }
}
