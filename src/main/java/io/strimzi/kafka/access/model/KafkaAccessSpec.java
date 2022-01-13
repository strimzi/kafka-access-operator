/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

public class KafkaAccessSpec {

    private KafkaReference kafka;

    private KafkaUserReference user;

    public KafkaReference getKafka() {
        return kafka;
    }

    public void setKafka(final KafkaReference kafka) {
        this.kafka = kafka;
    }

    public KafkaUserReference getUser() {
        return user;
    }

    public void setUser(final KafkaUserReference kafkaUser) {
        this.user = kafkaUser;
    }

}
