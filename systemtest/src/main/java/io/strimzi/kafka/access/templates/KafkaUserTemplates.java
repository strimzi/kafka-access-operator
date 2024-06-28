/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.templates;

import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;

public class KafkaUserTemplates {

    private KafkaUserTemplates() {}

    public static KafkaUser kafkaUser(String namespaceName, String userName, KafkaUserAuthentication auth) {
        return new KafkaUserBuilder()
            .withNewMetadata()
                .withName(userName)
                .withNamespace(namespaceName)
            .endMetadata()
            .withNewSpec()
                .withAuthentication(auth)
            .endSpec()
            .withNewStatus()
                .withSecret(userName)
                .withUsername("CN=" + userName)
            .endStatus()
            .build();
    }
}
