/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import javax.validation.constraints.NotNull;

/**
 *
 */
public class KafkaAccessSpec {

    @NotNull
    private KafkaReference kafka;
    private KafkaUserReference user;

    /**
     * @return
     */
    public KafkaReference getKafka() {
        return kafka;
    }

    /**
     * @param kafka
     */
    public void setKafka(final KafkaReference kafka) {
        this.kafka = kafka;
    }

    /**
     * @return
     */
    public KafkaUserReference getUser() {
        return user;
    }

    /**
     * @param kafkaUser
     */
    public void setUser(final KafkaUserReference kafkaUser) {
        this.user = kafkaUser;
    }

}
