/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import java.util.ArrayList;
import java.util.List;

import io.strimzi.api.kafka.model.status.Condition;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StatusUtilsTest {
    @Test
    void testConditionUpdate() {
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
        var c3 = new ConditionBuilder()
            .withType(StatusUtils.CONDITION_TYPE_READY)
            .withStatus(StatusUtils.CONDITION_STATUS_TRUE)
            .withMessage("foo-3")
            .withReason("bar")
            .build();
        var c4 = new ConditionBuilder()
            .withType(StatusUtils.CONDITION_TYPE_READY)
            .withStatus(StatusUtils.CONDITION_STATUS_TRUE)
            .withMessage("foo-4")
            .withReason("bar")
            .build();

        List<Condition> conditions = new ArrayList<>();
        conditions.add(c1);

        {
            // condition is added to the list, not went through the StatusUtils
            // hence the transition time should not have been set

            assertThat(conditions.get(0)).satisfies(c -> {
                assertThat(c.getLastTransitionTime()).isNull();
            });
        }


        {
            // condition amended, it should have the last transition time
            // set as it was null before

            StatusUtils.setCondition(conditions, c2);

            assertThat(conditions.get(0)).satisfies(c -> {
                assertThat(c.getLastTransitionTime()).isNotNull();
                assertThat(c.getMessage()).isEqualTo(c2.getMessage());
            });
        }


        {
            // condition amended and status changed from false -> true so
            // the transition time should have been updated

            var ltt2 = conditions.get(0).getLastTransitionTime();

            StatusUtils.setCondition(conditions, c3);

            assertThat(conditions.get(0)).satisfies(c -> {
                assertThat(c.getLastTransitionTime()).isNotNull();
                assertThat(c.getLastTransitionTime()).isNotSameAs(ltt2);
                assertThat(c.getMessage()).isEqualTo(c3.getMessage());
            });
        }

        {
            // condition amended without altering the status, hence the
            // last transition time should not change

            var ltt3 = conditions.get(0).getLastTransitionTime();

            StatusUtils.setCondition(conditions, c4);

            assertThat(conditions.get(0)).satisfies(c -> {
                assertThat(c.getLastTransitionTime()).isNotNull();
                assertThat(c.getLastTransitionTime()).isSameAs(ltt3);
                assertThat(c.getMessage()).isEqualTo(c4.getMessage());
            });
        }
    }
}
