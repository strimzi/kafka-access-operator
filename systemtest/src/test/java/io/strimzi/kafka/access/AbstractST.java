/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.resources.ClusterRoleBindingType;
import io.skodjob.testframe.resources.ClusterRoleType;
import io.skodjob.testframe.resources.CustomResourceDefinitionType;
import io.skodjob.testframe.resources.DeploymentType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.NamespaceType;
import io.strimzi.kafka.access.installation.SetupAccessOperator;
import io.strimzi.kafka.access.resources.KafkaAccessType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@ResourceManager
@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings("ClassDataAbstractionCoupling")
public abstract class AbstractST {
    protected final KubeResourceManager resourceManager = KubeResourceManager.getInstance();
    public final String namespace = "main-namespace";
    private final String kafkaCrdUrl = "https://raw.githubusercontent.com/strimzi/strimzi-kafka-operator/%s/packaging/install/cluster-operator/040-Crd-kafka.yaml".formatted(TestConstants.STRIMZI_API_VERSION);
    private final String kafkaUserCrdUrl = "https://raw.githubusercontent.com/strimzi/strimzi-kafka-operator/%s/packaging/install/cluster-operator/044-Crd-kafkauser.yaml".formatted(TestConstants.STRIMZI_API_VERSION);
    private final SetupAccessOperator setupAccessOperator = new SetupAccessOperator(namespace);

    static {
        KubeResourceManager.getInstance().setResourceTypes(
            new ClusterRoleBindingType(),
            new ClusterRoleType(),
            new CustomResourceDefinitionType(),
            new DeploymentType(),
            new NamespaceType(),
            new KafkaAccessType()
        );
    }

    @BeforeAll
    void createResources() {
        // apply Kafka and KafkaUser CRDs for the tests
        KubeResourceManager.getKubeCmdClient().inNamespace(namespace).execInCurrentNamespace("apply", "-f", kafkaCrdUrl);
        KubeResourceManager.getKubeCmdClient().inNamespace(namespace).execInCurrentNamespace("apply", "-f", kafkaUserCrdUrl);

        // install KafkaAccessOperator
        setupAccessOperator.install();
    }

    @AfterAll
    void deleteResources() {
        // delete KafkaAccessOperator
        setupAccessOperator.delete();

        // delete CRDs
        KubeResourceManager.getKubeCmdClient().inNamespace(namespace).execInCurrentNamespace("delete", "-f", kafkaCrdUrl);
        KubeResourceManager.getKubeCmdClient().inNamespace(namespace).execInCurrentNamespace("delete", "-f", kafkaUserCrdUrl);
    }
}
