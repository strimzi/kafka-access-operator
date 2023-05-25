/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

/**
 * The status class for keeping the state of service binding status
 */
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
     * @param secretName    The Kubernetes secret name
     */
    public BindingStatus(final String secretName) {
        this.setName(secretName);
    }

    /**
     * Gets the name of the BindingStatus instance
     *
     * @return  The name of the BindingStatus instance
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
