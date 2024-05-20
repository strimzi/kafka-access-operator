/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.templates;

import io.strimzi.kafka.access.model.KafkaAccessBuilder;

public class KafkaAccessTemplates {

    private KafkaAccessTemplates() {}

    public static KafkaAccessBuilder kafkaAccess(String namespaceName, String name) {
        return new KafkaAccessBuilder()
            .withNewMetadata()
                .withName(name)
                .withNamespace(namespaceName)
            .endMetadata();
    }
}
