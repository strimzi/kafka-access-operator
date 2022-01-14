/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.strimzi.kafka.access.model.KafkaAccess;

import java.util.Collections;
import java.util.List;

@ControllerConfiguration
public class KafkaAccessReconciler implements Reconciler<KafkaAccess>, EventSourceInitializer<KafkaAccess> {

    private final KubernetesClient kubernetesClient;

    public KafkaAccessReconciler(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public UpdateControl<KafkaAccess> reconcile(KafkaAccess kafkaAccess, Context context) {
        return UpdateControl.noUpdate();
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<KafkaAccess> context) {
        return Collections.emptyList();
    }
}
