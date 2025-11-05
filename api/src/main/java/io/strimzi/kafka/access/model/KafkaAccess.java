/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.model;

import io.fabric8.crd.generator.annotation.AdditionalPrinterColumn;
import io.fabric8.crd.generator.annotation.AdditionalPrinterColumns;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Categories;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;
import io.strimzi.api.kafka.model.common.Constants;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

import java.io.Serial;

/**
 * The KafkaAccess custom resource model
 */
@Group("access.strimzi.io")
@Version("v1alpha1")
@ShortNames("ka")
@Buildable(
    editableEnabled = false,
    builderPackage = Constants.FABRIC8_KUBERNETES_API,
    refs = {@BuildableReference(CustomResource.class), @BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class)}
)

@Categories(value = "strimzi")
@AdditionalPrinterColumns(value = { 
    @AdditionalPrinterColumn(jsonPath = ".spec.kafka.listener", name = "Listener"),
    @AdditionalPrinterColumn(jsonPath = ".spec.kafka.name", name = "Cluster"),
    @AdditionalPrinterColumn(jsonPath = ".spec.user.name", name = "User"),
    @AdditionalPrinterColumn(jsonPath = ".status.conditions[?(@.type==\"Ready\")].status", name = "Ready")
})
public class KafkaAccess extends CustomResource<KafkaAccessSpec, KafkaAccessStatus> implements Namespaced {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The `kind` definition of the KafkaAccess custom resource
     */
    public static final String KIND = "KafkaAccess";
}