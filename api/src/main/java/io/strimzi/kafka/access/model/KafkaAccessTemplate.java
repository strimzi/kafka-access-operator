/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.strimzi.api.kafka.model.common.Constants;
import io.sundr.builder.annotations.Buildable;

/**
 * Template for KafkaAccess resources (e.g., Secret template)
 */
@Buildable(
    editableEnabled = false,
    builderPackage = Constants.FABRIC8_KUBERNETES_API
)
public class KafkaAccessTemplate {

    private SecretTemplate secret;

    /**
     * Gets the Secret template
     *
     * @return The SecretTemplate instance
     */
    public SecretTemplate getSecret() {
        return secret;
    }

    /**
     * Sets the Secret template
     *
     * @param secret The SecretTemplate model
     */
    public void setSecret(final SecretTemplate secret) {
        this.secret = secret;
    }
}

