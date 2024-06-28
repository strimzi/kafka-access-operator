/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.utils;

import io.skodjob.testframe.wait.Wait;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.kafka.access.TestConstants;
import io.strimzi.kafka.access.resources.KafkaAccessType;

import java.util.Optional;

public class KafkaAccessUtils {

    private KafkaAccessUtils() {}

    public static final String READY_TYPE = "Ready";

    public static void waitForKafkaAccessReady(String namespaceName, String accessName) {
        waitForKafkaAccessStatus(namespaceName, accessName, READY_TYPE, "True");
    }

    public static void waitForKafkaAccessNotReady(String namespaceName, String accessName) {
        waitForKafkaAccessStatus(namespaceName, accessName, READY_TYPE, "False");
    }

    public static void waitForKafkaAccessStatus(String namespaceName, String accessName, String conditionType, String conditionStatus) {
        Wait.until(
            "KafkaAccess %s/%s to contain condition %s with status %s".formatted(namespaceName, accessName, conditionType, conditionStatus),
            TestConstants.GLOBAL_POLL_INTERVAL_SMALL_MS,
            TestConstants.GLOBAL_TIMEOUT_SMALL_MS,
            () -> {
                Optional<Condition> desiredStatus = KafkaAccessType.kafkaAccessClient().inNamespace(namespaceName).withName(accessName).get()
                    .getStatus().getConditions().stream().filter(condition -> condition.getType().equals(conditionType)).findFirst();

                return desiredStatus.map(condition -> condition.getStatus().equals(conditionStatus)).orElse(false);
            }
        );
    }
}
