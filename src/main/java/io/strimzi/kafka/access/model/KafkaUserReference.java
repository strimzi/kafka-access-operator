/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import javax.validation.constraints.NotNull;

/**
 * The Kafka user reference, which keeps state for a KafkaUser resource of Strimzi Kafka Operator
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
     * Gets the name of the Kafka user reference
     *
     * @return The name of the Kafka user reference
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the Kafka user reference
     *
     * @param name The name of the Kafka user reference
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the namespace of the Kafka user reference
     *
     * @return The namespace of the Kafka user reference
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace of the Kafka user reference
     *
     * @param namespace The namespace of the Kafka user reference
     */
    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }


    /**
     * Gets the resource kind of the Kafka user reference
     *
     * @return The resource kind of the Kafka user reference
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the resource kind of the Kafka user reference
     *
     * @param kind The resource kind of the Kafka user reference
     */
    public void setKind(final String kind) {
        this.kind = kind;
    }

    /**
     * Gets the resource API group of the Kafka user reference
     *
     * @return The resource API group of the Kafka user reference
     */
    public String getApiGroup() {
        return apiGroup;
    }

    /**
     * Sets the resource API group of the Kafka user reference
     *
     * @param apiGroup The resource API group of the Kafka user reference
     */
    public void setApiGroup(final String apiGroup) {
        this.apiGroup = apiGroup;
    }


    /**
     * Returns the serialized string of the KafkaUserReference object
     *
     * @return The serialized string of the KafkaUserReference object
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
