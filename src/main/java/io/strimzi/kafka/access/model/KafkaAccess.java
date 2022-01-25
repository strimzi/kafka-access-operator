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

@Group("strimzi.io")
@Version("v1")
@ShortNames("ka")
public class KafkaAccess extends CustomResource<KafkaAccessSpec, KafkaAccessStatus> implements Namespaced {

    public static final String KIND = "KafkaAccess";

    @Override
    protected KafkaAccessStatus initStatus() {
        return new KafkaAccessStatus();
    }
}