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
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.KafkaUserSpec;
import io.strimzi.kafka.access.internal.KafkaListener;
import io.strimzi.kafka.access.internal.KafkaAccessParser;
import io.strimzi.kafka.access.model.BindingStatus;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessSpec;
import io.strimzi.kafka.access.model.KafkaAccessStatus;
import io.strimzi.kafka.access.model.KafkaReference;
import io.strimzi.kafka.access.model.KafkaUserReference;
import io.strimzi.kafka.access.internal.KafkaParser;
import io.strimzi.kafka.access.internal.CustomResourceParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("ClassFanOutComplexity")
@ControllerConfiguration
public class KafkaAccessReconciler implements Reconciler<KafkaAccess>, EventSourceInitializer<KafkaAccess> {

    private static final String TYPE_SECRET_KEY = "type";
    private static final String TYPE_SECRET_VALUE = "kafka";
    private static final String PROVIDER_SECRET_KEY = "provider";
    private static final String PROVIDER_SECRET_VALUE = "strimzi";
    private static final String SECRET_TYPE = "servicebinding.io/kafka";
    private static final String ACCESS_SECRET_EVENT_SOURCE = "access-secret-event-source";

    private final KubernetesClient kubernetesClient;
    private final Map<String, String> commonSecretData = new HashMap<>();
    private final Map<String, String> commonSecretLabels = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAccessOperator.class);

    public KafkaAccessReconciler(final KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        final Base64.Encoder encoder = Base64.getEncoder();
        commonSecretData.put(TYPE_SECRET_KEY, encoder.encodeToString(TYPE_SECRET_VALUE.getBytes(StandardCharsets.UTF_8)));
        commonSecretData.put(PROVIDER_SECRET_KEY, encoder.encodeToString(PROVIDER_SECRET_VALUE.getBytes(StandardCharsets.UTF_8)));
        commonSecretLabels.put(KafkaAccessParser.MANAGED_BY_LABEL_KEY, KafkaAccessParser.KAFKA_ACCESS_LABEL_VALUE);
    }

    @Override
    public UpdateControl<KafkaAccess> reconcile(final KafkaAccess kafkaAccess, final Context<KafkaAccess> context) {
        final String kafkaAccessName = kafkaAccess.getMetadata().getName();
        final String kafkaAccessNamespace = kafkaAccess.getMetadata().getNamespace();
        LOGGER.info("Reconciling KafkaAccess {}/{}", kafkaAccessNamespace, kafkaAccessName);

        final Map<String, String> data  = secretData(kafkaAccess.getSpec(), kafkaAccessNamespace);
        createOrUpdateSecret(context, data, kafkaAccess);

        final boolean bindingStatusCorrect = Optional.ofNullable(kafkaAccess.getStatus())
                .map(KafkaAccessStatus::getBinding)
                .map(BindingStatus::getName)
                .map(kafkaAccessName::equals)
                .orElse(false);
        if (!bindingStatusCorrect) {
            final KafkaAccessStatus status = new KafkaAccessStatus();
            status.setBinding(new BindingStatus(kafkaAccessName));
            kafkaAccess.setStatus(status);
            return UpdateControl.updateStatus(kafkaAccess);
        } else {
            return UpdateControl.noUpdate();
        }
    }

    protected Map<String, String> secretData(final KafkaAccessSpec spec, final String kafkaAccessNamespace) {
        final KafkaReference kafkaReference = spec.getKafka();
        final String kafkaClusterNamespace = Optional.ofNullable(kafkaReference.getNamespace()).orElse(kafkaAccessNamespace);
        final Optional<KafkaUserReference> kafkaUserReference = Optional.ofNullable(spec.getUser());

        final Kafka kafka = getKafka(kafkaReference.getName(), kafkaClusterNamespace);

        final KafkaListener listener;
        try {
            if (kafkaUserReference.isPresent()) {
                if (!KafkaUser.RESOURCE_KIND.equals(kafkaUserReference.get().getKind()) || !KafkaUser.RESOURCE_GROUP.equals(kafkaUserReference.get().getApiGroup())) {
                    throw new CustomResourceParseException(String.format("User kind must be %s and apiGroup must be %s", KafkaUser.RESOURCE_KIND, KafkaUser.RESOURCE_GROUP));
                }
                final String kafkaUserName = kafkaUserReference.get().getName();
                final String kafkaUserNamespace = Optional.ofNullable(kafkaUserReference.get().getNamespace()).orElse(kafkaAccessNamespace);
                final KafkaUser kafkaUser = getKafkaUser(kafkaUserName, kafkaUserNamespace);
                final String kafkaUserType = Optional.ofNullable(kafkaUser)
                        .map(KafkaUser::getSpec)
                        .map(KafkaUserSpec::getAuthentication)
                        .map(KafkaUserAuthentication::getType)
                        .orElse(KafkaParser.USER_AUTH_UNDEFINED);
                listener = KafkaParser.getKafkaListener(kafka, spec, kafkaUserType);
            } else {
                listener = KafkaParser.getKafkaListener(kafka, spec);
            }
        } catch (CustomResourceParseException e) {
            throw new IllegalStateException("Reconcile failed due to ParserException " + e.getMessage());
        }
        final Map<String, String> data  = new HashMap<>(commonSecretData);
        data.putAll(listener.getConnectionSecretData());
        return data;
    }

    private void createOrUpdateSecret(final Context<KafkaAccess> context, final Map<String, String> data, final KafkaAccess kafkaAccess) {
        final String kafkaAccessName = kafkaAccess.getMetadata().getName();
        final String kafkaAccessNamespace = kafkaAccess.getMetadata().getNamespace();
        context.getSecondaryResource(Secret.class, ACCESS_SECRET_EVENT_SOURCE)
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

    @Override
    public Map<String, EventSource> prepareEventSources(final EventSourceContext<KafkaAccess> context) {
        LOGGER.info("Preparing event sources");
        InformerEventSource<Kafka, KafkaAccess> kafkaEventSource = new InformerEventSource<>(
                InformerConfiguration.from(Kafka.class, context)
                        .withSecondaryToPrimaryMapper(kafka -> KafkaAccessParser.getKafkaAccessResourceIDsForKafkaInstance(context.getPrimaryCache().list(), kafka))
                        .build(),
                context);
        InformerEventSource<Secret, KafkaAccess> strimziSecretEventSource = new InformerEventSource<>(
                InformerConfiguration.from(Secret.class)
                        .withLabelSelector(String.format("%s=%s", KafkaAccessParser.MANAGED_BY_LABEL_KEY, KafkaAccessParser.STRIMZI_CLUSTER_LABEL_VALUE))
                        .withSecondaryToPrimaryMapper(secret -> KafkaAccessParser.getKafkaAccessResourceIDsForSecret(context.getPrimaryCache().list(), secret))
                        .build(),
                context);
        InformerEventSource<Secret, KafkaAccess> kafkaAccessSecretEventSource = new InformerEventSource<>(
                InformerConfiguration.from(Secret.class)
                        .withLabelSelector(String.format("%s=%s", KafkaAccessParser.MANAGED_BY_LABEL_KEY, KafkaAccessParser.KAFKA_ACCESS_LABEL_VALUE))
                        .withSecondaryToPrimaryMapper(secret -> KafkaAccessParser.getKafkaAccessResourceIDsForSecret(context.getPrimaryCache().list(), secret))
                        .build(),
                context);
        Map<String, EventSource> eventSources = EventSourceInitializer.nameEventSources(kafkaEventSource, strimziSecretEventSource);
        eventSources.put(ACCESS_SECRET_EVENT_SOURCE, kafkaAccessSecretEventSource);
        LOGGER.info("Finished preparing event sources");
        return eventSources;
    }

    private Kafka getKafka(final String clusterName, final String namespace) {
        return Crds.kafkaOperation(kubernetesClient)
                .inNamespace(namespace)
                .withName(clusterName)
                .get();
    }

    private KafkaUser getKafkaUser(final String name, final String namespace) {
        return Crds.kafkaUserOperation(kubernetesClient)
                .inNamespace(namespace)
                .withName(name)
                .get();
    }
}
