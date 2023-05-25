/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

/**
 * The status model of the KafkaAccess resource
 */
public class KafkaAccessStatus extends ObservedGenerationAwareStatus {

    private BindingStatus binding;

    /**
     * Gets the BindingStatus instance
     *
     * @return The BindingStatus instance
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

}
