/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

/**
 *
 */
public class KafkaAccessStatus extends ObservedGenerationAwareStatus {

    private BindingStatus binding;

    /**
     * @return
     */
    public BindingStatus getBinding() {
        return binding;
    }

    /**
     * @param bindingStatus
     */
    public void setBinding(final BindingStatus bindingStatus) {
        this.binding = bindingStatus;
    }

}
