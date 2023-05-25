/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

import java.io.Serial;

/**
 * The KafkaAccess custom resource model
 */
@Group("access.strimzi.io")
@Version("v1alpha1")
@ShortNames("ka")
public class KafkaAccess extends CustomResource<KafkaAccessSpec, KafkaAccessStatus> implements Namespaced {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The `kind` definition of the KafkaAccess custom resource
     */
    public static final String KIND = "KafkaAccess";

    @Override
    protected KafkaAccessStatus initStatus() {
        return new KafkaAccessStatus();
    }
}