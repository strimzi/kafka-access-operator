/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.strimzi.api.kafka.model.common.Constants;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.kafka.access.internal.StatusUtils;
import io.sundr.builder.annotations.Buildable;

import java.util.ArrayList;
import java.util.List;

/**
 * The status model of the KafkaAccess resource
 */
@Buildable(
    editableEnabled = false,
    builderPackage = Constants.FABRIC8_KUBERNETES_API
)
public class KafkaAccessStatus {

    private BindingStatus binding;
    private long observedGeneration;
    private final List<Condition> conditions = new ArrayList<>();

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
        StatusUtils.setCondition(
            this.conditions,
            StatusUtils.buildReadyCondition(ready, null, null)
        );
    }

    /**
     * Replaces the Ready condition in the status
     *
     * @param ready Whether the resource is ready
     * @param message The message for the status condition
     * @param reason The reason for the status condition
     */
    public void setReadyCondition(final boolean ready, final String message, final String reason) {
        StatusUtils.setCondition(
            this.conditions,
            StatusUtils.buildReadyCondition(ready, reason, message)
        );
    }

    /**
     * Gets the observed generation of the KafkaAccess resource.
     *
     * @return The observed generation.
     */
    public Long getObservedGeneration() {
        return observedGeneration;
    }

    public void setObservedGeneration(Long observedGeneration) {
        this.observedGeneration = observedGeneration;
    }
}
