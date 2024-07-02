/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import java.util.Random;

public class TestStorage {

    private static final String ACCESS_PREFIX = "access-";
    private static final String CLUSTER_NAME_PREFIX = "cluster-";
    private static final Random RANDOM = new Random();

    private final String kafkaAccessName;
    private final String kafkaClusterName;

    public TestStorage() {
        int randomized = Math.abs(RANDOM.nextInt(Integer.MAX_VALUE));
        this.kafkaAccessName = ACCESS_PREFIX + randomized;
        this.kafkaClusterName = CLUSTER_NAME_PREFIX + randomized;
    }

    public String getKafkaAccessName() {
        return kafkaAccessName;
    }

    public String getKafkaClusterName() {
        return kafkaClusterName;
    }
}
