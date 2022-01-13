/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

public class BindingStatus {

    private String name;

    public BindingStatus() {
    }

    public BindingStatus(final String secretName) {
        this.setName(secretName);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

}
