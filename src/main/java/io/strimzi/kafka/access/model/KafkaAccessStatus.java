/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import io.strimzi.api.kafka.model.status.Condition;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The status model of the KafkaAccess resource
 */
public class KafkaAccessStatus extends ObservedGenerationAwareStatus {

    private BindingStatus binding;
    private List<Condition> conditions = new ArrayList<>();

    /**
     * Gets the BindingStatus instance
     *
     * @return A BindingStatus instance
     */
    public BindingStatus getBinding() {
        return binding;
    }

    /**
     * Sets the BindingStatus instance
     *
     * @param bindingStatus The BindingStatus model
     */
    public void setBinding(final BindingStatus bindingStatus) {
        this.binding = bindingStatus;
    }

    /**
     * Gets the status conditions
     *
     * @return The status conditions
     */
    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * Replaces the Ready condition in the status
     *
     * @param ready Whether the resource is ready
     */
    public void setReadyCondition(final boolean ready) {
        setReadyCondition(ready, null, null);
    }

    /**
     * Replaces the Ready condition in the status
     *
     * @param ready Whether the resource is ready
     * @param message The message for the status condition
     * @param reason The reason for the status condition
     */
    public void setReadyCondition(final boolean ready, final String message, final String reason) {
        final Condition condition = new Condition();
        condition.setType("Ready");
        condition.setStatus(ready ? "True" : "False");
        condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        Optional.ofNullable(message)
                .ifPresent(condition::setMessage);
        Optional.ofNullable(reason)
                .ifPresent(condition::setReason);
        this.conditions = List.of(condition);
    }

}
