/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.fabric8.generator.annotation.Required;
import io.strimzi.api.kafka.model.common.Constants;
import io.sundr.builder.annotations.Buildable;

/**
 * The Kafka user reference, which keeps state for a KafkaUser resource of Strimzi Kafka Operator
 */
@Buildable(
    editableEnabled = false,
    builderPackage = Constants.FABRIC8_KUBERNETES_API
)
public class KafkaUserReference {

    @Required
    private String kind;
    @Required
    private String apiGroup;
    @Required
    @io.fabric8.crd.generator.annotation.PrinterColumn(name = "User")
    private String name;
    private String namespace;

    /**
     * Gets the name of the Kafka user reference
     *
     * @return A name for the Kafka user reference
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
     * @return A namespace definition for the Kafka user reference
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
     * @return A resource `kind` definition for the Kafka user reference
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
     * @return A resource API group definition for the Kafka user reference
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
     * @return A serialized string of the KafkaUserReference object
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
