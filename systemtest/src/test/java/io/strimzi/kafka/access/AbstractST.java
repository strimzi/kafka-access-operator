/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.Namespace;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.resources.ClusterRoleBindingType;
import io.skodjob.testframe.resources.ClusterRoleType;
import io.skodjob.testframe.resources.CustomResourceDefinitionType;
import io.skodjob.testframe.resources.DeploymentType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.NamespaceType;
import io.skodjob.testframe.utils.KubeUtils;
import io.strimzi.kafka.access.installation.SetupAccessOperator;
import io.strimzi.kafka.access.log.TestExecutionWatcher;
import io.strimzi.kafka.access.resources.KafkaAccessType;
import io.strimzi.kafka.access.utils.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ResourceManager
@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TestExecutionWatcher.class)
@SuppressWarnings("ClassDataAbstractionCoupling")
public abstract class AbstractST {
    protected final KubeResourceManager resourceManager = KubeResourceManager.get();
    public final String namespace = "main-namespace";
    private final String kafkaCrdUrl = "https://raw.githubusercontent.com/strimzi/strimzi-kafka-operator/%s/packaging/install/cluster-operator/040-Crd-kafka.yaml".formatted(TestConstants.STRIMZI_API_VERSION);
    private final String kafkaUserCrdUrl = "https://raw.githubusercontent.com/strimzi/strimzi-kafka-operator/%s/packaging/install/cluster-operator/044-Crd-kafkauser.yaml".formatted(TestConstants.STRIMZI_API_VERSION);
    private final SetupAccessOperator setupAccessOperator = new SetupAccessOperator(namespace);

    static {
        KubeResourceManager.get().setResourceTypes(
            new ClusterRoleBindingType(),
            new ClusterRoleType(),
            new CustomResourceDefinitionType(),
            new DeploymentType(),
            new NamespaceType(),
            new KafkaAccessType()
        );

        KubeResourceManager.get().addCreateCallback(resource -> {
            if (resource instanceof Namespace namespace) {
                String testClass = TestUtils.removePackageName(KubeResourceManager.get().getTestContext().getRequiredTestClass().getName());

                KubeUtils.labelNamespace(
                    namespace.getMetadata().getName(),
                    TestConstants.TEST_SUITE_NAME_LABEL,
                    testClass
                );

                if (KubeResourceManager.get().getTestContext().getTestMethod().isPresent()) {
                    String testCaseName = KubeResourceManager.get().getTestContext().getRequiredTestMethod().getName();

                    KubeUtils.labelNamespace(
                        namespace.getMetadata().getName(),
                        TestConstants.TEST_CASE_NAME_LABEL,
                        TestUtils.trimTestCaseBaseOnItsLength(testCaseName)
                    );
                }
            }
        });

        KubeResourceManager.get().setStoreYamlPath(Environment.TEST_LOG_DIR);
    }

    @BeforeAll
    void createResources() {
        // apply Kafka and KafkaUser CRDs for the tests
        KubeResourceManager.get().kubeCmdClient().inNamespace(namespace).exec("apply", "-f", kafkaCrdUrl);
        KubeResourceManager.get().kubeCmdClient().inNamespace(namespace).exec("apply", "-f", kafkaUserCrdUrl);

        // install KafkaAccessOperator
        setupAccessOperator.install();
    }

    @AfterAll
    void deleteResources() {
        // delete KafkaAccessOperator
        setupAccessOperator.delete();

        // delete CRDs
        KubeResourceManager.get().kubeCmdClient().inNamespace(namespace).exec("delete", "-f", kafkaCrdUrl);
        KubeResourceManager.get().kubeCmdClient().inNamespace(namespace).exec("delete", "-f", kafkaUserCrdUrl);
    }
}
