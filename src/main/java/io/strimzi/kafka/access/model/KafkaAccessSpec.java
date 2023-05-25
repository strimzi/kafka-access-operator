/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import javax.validation.constraints.NotNull;

/**
 * The spec model of the KafkaAccess resource
 */
public class KafkaAccessSpec {

    @NotNull
    private KafkaReference kafka;
    private KafkaUserReference user;

    /**
     * Gets the KafkaReference instance
     *
     * @return The KafkaReference instance
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
     * @return The KafkaUserReference instance
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

}
