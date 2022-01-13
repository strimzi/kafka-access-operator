/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

public class KafkaAccessStatus {

    private BindingStatus binding;

    public BindingStatus getBinding() {
        return binding;
    }

    public void setBinding(final BindingStatus bindingStatus) {
        this.binding = bindingStatus;
    }

}
