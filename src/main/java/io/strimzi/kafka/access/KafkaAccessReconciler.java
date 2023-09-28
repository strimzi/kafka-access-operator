/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.kafka.access.internal.KafkaAccessMapper;
import io.strimzi.kafka.access.internal.MissingKubernetesResourceException;
import io.strimzi.kafka.access.model.BindingStatus;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessStatus;
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
public class KafkaAccessReconciler implements Reconciler<KafkaAccess>, EventSourceInitializer<KafkaAccess>, ErrorStatusHandler<KafkaAccess> {

    private final KubernetesClient kubernetesClient;
    private InformerEventSource<Secret, KafkaAccess> kafkaAccessSecretEventSource;
    private final SecretDependentResource secretDependentResource;
    private final Map<String, String> commonSecretLabels = new HashMap<>();
    private static final String SECRET_TYPE = "servicebinding.io/kafka";
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAccessOperator.class);

    /**
     * Name of the event source for Strimzi Secret resources
     */
    public static final String STRIMZI_SECRET_EVENT_SOURCE = "STRIMZI_SECRET_EVENT_SOURCE";

    /**
     * Name of the event source for KafkaUser Secret resources
     */
    public static final String KAFKA_USER_SECRET_EVENT_SOURCE = "KAFKA_USER_SECRET_EVENT_SOURCE";

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

        createOrUpdateSecret(secretDependentResource.desired(kafkaAccess.getSpec(), kafkaAccessNamespace, context), kafkaAccess);

        final KafkaAccessStatus kafkaAccessStatus = Optional.ofNullable(kafkaAccess.getStatus())
                .orElseGet(() -> {
                    final KafkaAccessStatus status = new KafkaAccessStatus();
                    kafkaAccess.setStatus(status);
                    return status;
                });
        kafkaAccessStatus.setBinding(new BindingStatus(kafkaAccessName));
        kafkaAccessStatus.setReadyCondition(true);
        return UpdateControl.patchStatus(kafkaAccess);
    }

    private void createOrUpdateSecret(final Map<String, String> data, final KafkaAccess kafkaAccess) {
        final String kafkaAccessName = kafkaAccess.getMetadata().getName();
        final String kafkaAccessNamespace = kafkaAccess.getMetadata().getNamespace();
        if (kafkaAccessSecretEventSource == null) {
            throw new IllegalStateException("Event source for Kafka Access Secret not initialized, cannot reconcile");
        }
        kafkaAccessSecretEventSource.get(new ResourceID(kafkaAccessName, kafkaAccessNamespace))
                .ifPresentOrElse(secret -> {
                    final Map<String, String> currentData = secret.getData();
                    if (!data.equals(currentData)) {
                        kubernetesClient.secrets()
                                .inNamespace(kafkaAccessNamespace)
                                .withName(kafkaAccessName)
                                .edit(s -> new SecretBuilder(s).withData(data).build());
                    }
                }, () -> kubernetesClient
                        .secrets()
                        .inNamespace(kafkaAccessNamespace)
                        .resource(
                                new SecretBuilder()
                                        .withType(SECRET_TYPE)
                                        .withNewMetadata()
                                        .withName(kafkaAccessName)
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
                        .create()
            );
    }

    /**
     * Prepares the event sources required for triggering the reconciliation
     *
     * @param context       The EventSourceContext for KafkaAccess resource
     *
     * @return              A new map of event sources
     */
    @Override
    public Map<String, EventSource> prepareEventSources(final EventSourceContext<KafkaAccess> context) {
        LOGGER.info("Preparing event sources");
        InformerEventSource<Kafka, KafkaAccess> kafkaEventSource = new InformerEventSource<>(
                InformerConfiguration.from(Kafka.class, context)
                        .withSecondaryToPrimaryMapper(kafka -> KafkaAccessMapper.kafkaSecondaryToPrimaryMapper(context.getPrimaryCache().list(), kafka))
                        .withPrimaryToSecondaryMapper(kafkaAccess -> KafkaAccessMapper.kafkaPrimaryToSecondaryMapper((KafkaAccess) kafkaAccess))
                        .build(),
                context);
        InformerEventSource<KafkaUser, KafkaAccess> kafkaUserEventSource = new InformerEventSource<>(
                InformerConfiguration.from(KafkaUser.class, context)
                        .withSecondaryToPrimaryMapper(kafkaUser -> KafkaAccessMapper.kafkaUserSecondaryToPrimaryMapper(context.getPrimaryCache().list(), kafkaUser))
                        .withPrimaryToSecondaryMapper(kafkaAccess -> KafkaAccessMapper.kafkaUserPrimaryToSecondaryMapper((KafkaAccess) kafkaAccess))
                        .build(),
                context);
        InformerEventSource<Secret, KafkaAccess> strimziSecretEventSource = new InformerEventSource<>(
                InformerConfiguration.from(Secret.class)
                        .withLabelSelector(String.format("%s=%s", KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.STRIMZI_CLUSTER_LABEL_VALUE))
                        .withSecondaryToPrimaryMapper(secret -> KafkaAccessMapper.secretSecondaryToPrimaryMapper(context.getPrimaryCache().list(), secret))
                        .build(),
                context);
        InformerEventSource<Secret, KafkaAccess> strimziKafkaUserSecretEventSource = new InformerEventSource<>(
                InformerConfiguration.from(Secret.class)
                        .withLabelSelector(String.format("%s=%s", KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.STRIMZI_USER_LABEL_VALUE))
                        .withSecondaryToPrimaryMapper(secret -> KafkaAccessMapper.secretSecondaryToPrimaryMapper(context.getPrimaryCache().list(), secret))
                        .build(),
                context);
        kafkaAccessSecretEventSource = new InformerEventSource<>(
                InformerConfiguration.from(Secret.class)
                        .withLabelSelector(String.format("%s=%s", KafkaAccessMapper.MANAGED_BY_LABEL_KEY, KafkaAccessMapper.KAFKA_ACCESS_LABEL_VALUE))
                        .withSecondaryToPrimaryMapper(secret -> KafkaAccessMapper.secretSecondaryToPrimaryMapper(context.getPrimaryCache().list(), secret))
                        .build(),
                context);
        Map<String, EventSource> eventSources = EventSourceInitializer.nameEventSources(
                kafkaEventSource,
                kafkaUserEventSource,
                kafkaAccessSecretEventSource
        );
        eventSources.put(STRIMZI_SECRET_EVENT_SOURCE, strimziSecretEventSource);
        eventSources.put(KAFKA_USER_SECRET_EVENT_SOURCE, strimziKafkaUserSecretEventSource);
        LOGGER.info("Finished preparing event sources");
        return eventSources;
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
}
