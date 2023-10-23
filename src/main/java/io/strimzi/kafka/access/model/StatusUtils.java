/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.strimzi.api.kafka.model.status.Condition;
import io.strimzi.api.kafka.model.status.ConditionBuilder;

/**
 * Utility methods for working with status sections of custom resources
 */
public class StatusUtils {
    /**
     * Type of a ready status condition.
     */
    public static final String CONDITION_TYPE_READY = "Ready";

    /**
     * Indicates that a condition is applicable
     */
    public static final String CONDITION_STATUS_TRUE = "True";

    /**
     * Indicates that a condition is not applicable
     */
    public static final String CONDITION_STATUS_FALSE = "False";

    /**
     * Returns the current timestamp in ISO 8601 format, for example "2019-07-23T09:08:12.356Z".
     *
     * @return the current timestamp in ISO 8601 format, for example "2019-07-23T09:08:12.356Z".
     */
    public static String iso8601Now() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

    /**
     * Replaces a condition in the status
     *
     * @param conditions The current condition list
     * @param condition The condition
     */
    public static void setCondition(final List<Condition> conditions, final Condition condition) {
        boolean found = false;

        for (final Condition c : conditions) {
            if (Objects.equals(c.getType(), condition.getType())) {

                found = true;

                if (!Objects.equals(c.getStatus(), condition.getStatus())) {
                    c.setLastTransitionTime(StatusUtils.iso8601Now());
                    c.setStatus(condition.getStatus());
                }
                if (!Objects.equals(c.getReason(), condition.getReason())) {
                    c.setReason(condition.getReason());
                }
                if (!Objects.equals(c.getMessage(), condition.getMessage())) {
                    c.setMessage(condition.getMessage());
                }
                if (c.getLastTransitionTime() == null) {
                    c.setLastTransitionTime(StatusUtils.iso8601Now());
                }
            }
        }

        if (!found) {
            if (condition.getLastTransitionTime() == null) {
                condition.setLastTransitionTime(StatusUtils.iso8601Now());
            }

            conditions.add(condition);
        }

        // keep sorted by type to avoid resource changed events because of changing orders
        conditions.sort(Comparator.comparing(Condition::getType));
    }


    /**
     * Creates a new ready type condition
     *
     * @param ready Whether the resource is ready
     * @param reason Reason for the condition
     * @param message Message of the condition
     *
     * @return  New ready type condition
     */
    public static Condition buildReadyCondition(boolean ready, String reason, String message) {
        return new ConditionBuilder()
            .withType(CONDITION_TYPE_READY)
            .withStatus(ready ? CONDITION_STATUS_TRUE : CONDITION_STATUS_FALSE)
            .withReason(reason)
            .withMessage(message)
            .build();
    }
}
