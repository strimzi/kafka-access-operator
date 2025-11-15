/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.strimzi.api.kafka.model.common.Constants;
import io.sundr.builder.annotations.Buildable;

import java.util.Map;

/**
 * Template for Kubernetes resource metadata (labels, annotations)
 */
@Buildable(
    editableEnabled = false,
    builderPackage = Constants.FABRIC8_KUBERNETES_API
)
public class MetadataTemplate {

    private Map<String, String> labels;
    private Map<String, String> annotations;

    /**
     * Gets the labels
     *
     * @return A map of labels
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Sets the labels
     *
     * @param labels A map of labels
     */
    public void setLabels(final Map<String, String> labels) {
        this.labels = labels;
    }

    /**
     * Gets the annotations
     *
     * @return A map of annotations
     */
    public Map<String, String> getAnnotations() {
        return annotations;
    }

    /**
     * Sets the annotations
     *
     * @param annotations A map of annotations
     */
    public void setAnnotations(final Map<String, String> annotations) {
        this.annotations = annotations;
    }
}

