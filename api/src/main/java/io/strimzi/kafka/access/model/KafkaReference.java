/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.fabric8.generator.annotation.Required;
import io.strimzi.api.kafka.model.common.Constants;
import io.sundr.builder.annotations.Buildable;

/**
 * The Kafka reference. Keeps state for a Kafka resource of Strimzi Kafka Operator
 */
@Buildable(
    editableEnabled = false,
    builderPackage = Constants.FABRIC8_KUBERNETES_API
)
public class KafkaReference {

    @Required
    @io.fabric8.crd.generator.annotation.PrinterColumn(name = "Cluster")
    private String name;
    private String namespace;
    @io.fabric8.crd.generator.annotation.PrinterColumn()
    private String listener;

    /**
     * Gets the name of the Kafka reference
     *
     * @return The name of the Kafka reference
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the Kafka reference
     *
     * @param name The name of the Kafka reference
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the namespace of the Kafka reference
     *
     * @return A namespace definition for the Kafka reference
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace of the Kafka reference
     *
     * @param namespace The namespace of the Kafka reference
     */
    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    /**
     * Gets the listener of the Kafka reference
     *
     * @return A listener definition for the Kafka reference
     */
    public String getListener() {
        return listener;
    }

    /**
     * Sets the listener of the Kafka reference
     *
     * @param listener The listener of the Kafka reference
     */
    public void setListener(final String listener) {
        this.listener = listener;
    }

}
