/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.strimzi.api.kafka.model.common.Constants;
import io.sundr.builder.annotations.Buildable;

/**
 * The status class for keeping the state of service binding status
 */
@Buildable(
    editableEnabled = false,
    builderPackage = Constants.FABRIC8_KUBERNETES_API
)
public class BindingStatus {

    private String name;

    /**
     * Default constructor
     */
    public BindingStatus() {
    }

    /**
     * Constructor
     *
     * @param name    The Kubernetes secret name
     */
    public BindingStatus(final String name) {
        this.setName(name);
    }

    /**
     * Gets the name of the BindingStatus instance
     *
     * @return  A name for the BindingStatus instance
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the name of the BindingStatus instance
     *
     * @param name The name of the BindingStatus instance
     */
    public void setName(final String name) {
        this.name = name;
    }

}
