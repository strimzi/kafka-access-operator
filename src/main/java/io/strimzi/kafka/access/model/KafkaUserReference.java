/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import javax.validation.constraints.NotNull;

/**
 *
 */
public class KafkaUserReference {

    @NotNull
    private String kind;
    @NotNull
    private String apiGroup;
    @NotNull
    private String name;
    private String namespace;

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
    public String getKind() {
        return kind;
    }

    /**
     * @param kind
     */
    public void setKind(final String kind) {
        this.kind = kind;
    }

    /**
     * @return
     */
    public String getApiGroup() {
        return apiGroup;
    }

    /**
     * @param apiGroup
     */
    public void setApiGroup(final String apiGroup) {
        this.apiGroup = apiGroup;
    }


    /**
     * @return
     */
    @Override
    public String toString() {
        return "KafkaUserReference{" +
                "kind='" + kind + '\'' +
                ", apiGroup='" + apiGroup + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
