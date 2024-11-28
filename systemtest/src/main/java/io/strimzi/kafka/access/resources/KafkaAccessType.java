/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.model.KafkaAccessStatus;

import java.util.Optional;
import java.util.function.Consumer;

public class KafkaAccessType implements ResourceType<KafkaAccess> {

    private final MixedOperation<KafkaAccess, KubernetesResourceList<KafkaAccess>, Resource<KafkaAccess>> client;

    /**
     * Constructor
     */
    public KafkaAccessType() {
        this.client = kafkaAccessClient();
    }


    @Override
    public MixedOperation<?, ?, ?> getClient() {
        return this.client;
    }

    @Override
    public String getKind() {
        return KafkaAccess.KIND;
    }

    @Override
    public void create(KafkaAccess kafkaAccess) {
        client.resource(kafkaAccess).create();
    }

    @Override
    public void update(KafkaAccess kafkaAccess) {
        client.resource(kafkaAccess).update();
    }

    @Override
    public void delete(KafkaAccess kafkaAccess) {
        client.resource(kafkaAccess).delete();
    }

    @Override
    public void replace(KafkaAccess kafkaAccess, Consumer<KafkaAccess> editor) {
        KafkaAccess toBeReplaced = client.inNamespace(kafkaAccess.getMetadata().getNamespace()).withName(kafkaAccess.getMetadata().getName()).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaAccess kafkaAccess) {
        KafkaAccessStatus kafkaAccessStatus = client.resource(kafkaAccess).get().getStatus();
        Optional<Condition> readyCondition = kafkaAccessStatus.getConditions().stream().filter(condition -> condition.getType().equals("Ready")).findFirst();

        return readyCondition.map(condition -> condition.getStatus().equals("True")).orElse(false);
    }

    @Override
    public boolean isDeleted(KafkaAccess kafkaAccess) {
        return kafkaAccess == null;
    }

    public static MixedOperation<KafkaAccess, KubernetesResourceList<KafkaAccess>, Resource<KafkaAccess>> kafkaAccessClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(KafkaAccess.class);
    }
}
