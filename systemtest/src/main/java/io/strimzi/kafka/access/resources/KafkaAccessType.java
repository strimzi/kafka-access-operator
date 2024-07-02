/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.NamespacedResourceType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.utils.KafkaAccessUtils;

import java.util.function.Consumer;

public class KafkaAccessType implements NamespacedResourceType<KafkaAccess> {

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
    public void delete(String resourceName) {
        client.withName(resourceName).delete();
    }

    @Override
    public void replace(String resourceName, Consumer<KafkaAccess> editor) {
        KafkaAccess toBeReplaced = client.withName(resourceName).get();
        editor.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean waitForReadiness(KafkaAccess kafkaAccess) {
        return KafkaAccessUtils.waitForKafkaAccessReady(kafkaAccess.getMetadata().getNamespace(), kafkaAccess.getMetadata().getName());
    }

    @Override
    public boolean waitForDeletion(KafkaAccess kafkaAccess) {
        return kafkaAccess == null;
    }

    @Override
    public void createInNamespace(String namespaceName, KafkaAccess kafkaAccess) {
        client.inNamespace(namespaceName).resource(kafkaAccess).create();
    }

    @Override
    public void updateInNamespace(String namespaceName, KafkaAccess kafkaAccess) {
        client.inNamespace(namespaceName).resource(kafkaAccess).update();
    }

    @Override
    public void deleteFromNamespace(String namespaceName, String resourceName) {
        client.inNamespace(namespaceName).withName(resourceName).delete();
    }

    @Override
    public void replaceInNamespace(String namespaceName, String resourceName, Consumer<KafkaAccess> editor) {
        KafkaAccess toBeReplaced = client.inNamespace(namespaceName).withName(resourceName).get();
        editor.accept(toBeReplaced);
        updateInNamespace(namespaceName, toBeReplaced);
    }

    public static MixedOperation<KafkaAccess, KubernetesResourceList<KafkaAccess>, Resource<KafkaAccess>> kafkaAccessClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(KafkaAccess.class);
    }
}
