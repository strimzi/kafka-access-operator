/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.strimzi.api.kafka.model.common.Constants;
import io.sundr.builder.annotations.Buildable;

/**
 * Template for Secret metadata (labels, annotations)
 */
@Buildable(
    editableEnabled = false,
    builderPackage = Constants.FABRIC8_KUBERNETES_API
)
public class SecretTemplate {

    private MetadataTemplate metadata;

    /**
     * Gets the metadata template
     *
     * @return The MetadataTemplate instance
     */
    public MetadataTemplate getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata template
     *
     * @param metadata The MetadataTemplate model
     */
    public void setMetadata(final MetadataTemplate metadata) {
        this.metadata = metadata;
    }
}

