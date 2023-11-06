/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import java.util.ArrayList;
import java.util.List;

import io.strimzi.api.kafka.model.status.Condition;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StatusUtilsTest {
    @Test
    @DisplayName("When a condition is added, the LastTransitionTime should be set if empty")
    void testLastTransitionTimeSetIfEmpty() {
        final List<Condition> conditions = new ArrayList<>();

        var c1 = new ConditionBuilder()
            .withType(StatusUtils.CONDITION_TYPE_READY)
            .withStatus(StatusUtils.CONDITION_STATUS_FALSE)
            .withMessage("foo-1")
            .withReason("bar")
            .build();

        StatusUtils.setCondition(conditions, c1);

        assertThat(conditions).first().satisfies(c -> {
            assertThat(c.getLastTransitionTime()).isNotNull();
        });
    }

    @Test
    @DisplayName("When a condition is amended, the LastTransitionTime should be set if empty")
    void testLastTransitionTimeSetIfEmptyWhenAmend() {
        final List<Condition> conditions = new ArrayList<>();

        var c1 = new ConditionBuilder()
            .withType(StatusUtils.CONDITION_TYPE_READY)
            .withStatus(StatusUtils.CONDITION_STATUS_FALSE)
            .withMessage("foo-1")
            .withReason("bar")
            .build();
        var c2 = new ConditionBuilder()
            .withType(StatusUtils.CONDITION_TYPE_READY)
            .withStatus(StatusUtils.CONDITION_STATUS_FALSE)
            .withMessage("foo-2")
            .withReason("bar")
            .build();

        conditions.add(c1);

        StatusUtils.setCondition(conditions, c2);

        assertThat(conditions).first().satisfies(c -> {
            assertThat(c.getLastTransitionTime()).isNotNull();
            assertThat(c.getMessage()).isEqualTo(c2.getMessage());
        });
    }

    @Test
    @DisplayName("When a condition is amended and its status changes, the LastTransitionTime should be updated accordingly")
    void testLastTransitionTimeUpdatedOnStatusChange() {
        final List<Condition> conditions = new ArrayList<>();

        var c1 = new ConditionBuilder()
            .withType(StatusUtils.CONDITION_TYPE_READY)
            .withStatus(StatusUtils.CONDITION_STATUS_FALSE)
            .withMessage("foo-1")
            .withReason("bar")
            .build();
        var c2 = new ConditionBuilder()
            .withType(StatusUtils.CONDITION_TYPE_READY)
            .withStatus(StatusUtils.CONDITION_STATUS_TRUE)
            .withMessage("foo-2")
            .withReason("bar")
            .build();

        // add a condition, LastTransitionTime should be set
        StatusUtils.setCondition(conditions, c1);

        assertThat(conditions).first().satisfies(c -> {
            assertThat(c.getLastTransitionTime()).isNotNull();
        });

        var ltt1 = conditions.get(0).getLastTransitionTime();

        // amend the condition status, LastTransitionTime should be updated
        StatusUtils.setCondition(conditions, c2);

        assertThat(conditions).first().satisfies(c -> {
            assertThat(c.getLastTransitionTime()).isNotNull();
            assertThat(c.getLastTransitionTime()).isNotSameAs(ltt1);
            assertThat(c.getMessage()).isEqualTo(c2.getMessage());
        });
    }

    @Test
    @DisplayName("When a condition is amended and its status does not change, the LastTransitionTime should not be updated")
    void testLastTransitionTimeNotUpdated() {
        final List<Condition> conditions = new ArrayList<>();

        var c1 = new ConditionBuilder()
            .withType(StatusUtils.CONDITION_TYPE_READY)
            .withStatus(StatusUtils.CONDITION_STATUS_FALSE)
            .withMessage("foo-1")
            .withReason("bar")
            .build();
        var c2 = new ConditionBuilder()
            .withType(StatusUtils.CONDITION_TYPE_READY)
            .withStatus(StatusUtils.CONDITION_STATUS_FALSE)
            .withMessage("foo-2")
            .withReason("bar")
            .build();

        // add a condition, LastTransitionTime should be set
        StatusUtils.setCondition(conditions, c1);

        assertThat(conditions).first().satisfies(c -> {
            assertThat(c.getLastTransitionTime()).isNotNull();
        });

        var ltt1 = conditions.get(0).getLastTransitionTime();

        // amend the condition but not its status, LastTransitionTime should not be updated
        StatusUtils.setCondition(conditions, c2);

        assertThat(conditions).first().satisfies(c -> {
            assertThat(c.getLastTransitionTime()).isNotNull();
            assertThat(c.getLastTransitionTime()).isSameAs(ltt1);
            assertThat(c.getMessage()).isEqualTo(c2.getMessage());
        });
    }
}
