/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.fabric8.generator.annotation.Required;
import io.strimzi.api.kafka.model.common.Constants;
import io.sundr.builder.annotations.Buildable;

/**
 * The spec model of the KafkaAccess resource
 */
@Buildable(
    editableEnabled = false,
    builderPackage = Constants.FABRIC8_KUBERNETES_API
)
public class KafkaAccessSpec {

    @Required
    private KafkaReference kafka;
    private KafkaUserReference user;
    private String secretName;

    /**
     * Gets the KafkaReference instance
     *
     * @return A KafkaReference instance
     */
    public KafkaReference getKafka() {
        return kafka;
    }

    /**
     * Sets the KafkaReference instance
     *
     * @param kafka The KafkaReference model
     */
    public void setKafka(final KafkaReference kafka) {
        this.kafka = kafka;
    }

    /**
     * Gets the KafkaUserReference instance
     *
     * @return A KafkaUserReference instance
     */
    public KafkaUserReference getUser() {
        return user;
    }

    /**
     * Sets the KafkaUserReference instance
     *
     * @param kafkaUser The KafkaUserReference model
     */
    public void setUser(final KafkaUserReference kafkaUser) {
        this.user = kafkaUser;
    }

    /**
     * Gets the name of the Secret the operator will create to store credentials
     *
     * @return The secret name
     */
    public String getSecretName() {
        return secretName;
    }

    /**
     * Sets the name of the Secret the operator will create to store credentials
     *
     * @param secretName The secret name
     */
    public void setSecretName(String secretName) {
        this.secretName = secretName;
    }
}
