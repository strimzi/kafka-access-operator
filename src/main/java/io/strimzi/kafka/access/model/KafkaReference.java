/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import javax.validation.constraints.NotNull;

/**
 *
 */
public class KafkaReference {

    @NotNull
    private String name;
    private String namespace;
    private String listener;

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace
     */
    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return
     */
    public String getListener() {
        return listener;
    }

    /**
     * @param listener
     */
    public void setListener(final String listener) {
        this.listener = listener;
    }

}
