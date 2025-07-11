/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.installation;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.skodjob.testframe.installation.InstallationMethod;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.ImageUtils;
import io.skodjob.testframe.utils.TestFrameUtils;
import io.strimzi.kafka.access.Environment;
import io.strimzi.kafka.access.TestConstants;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class BundleInstallation implements InstallationMethod {
    private final String installationNamespace;

    public BundleInstallation(String installationNamespace) {
        this.installationNamespace = installationNamespace;
    }

    @Override
    public void install() {
        List<File> accessOperatorFiles = Arrays.stream(new File(TestConstants.INSTALL_PATH).listFiles())
            .sorted()
            .filter(File::isFile)
            .toList();

        accessOperatorFiles.forEach(file -> {
            final String resourceType = file.getName().split("-")[1].replace(".yaml", "");

            switch (resourceType) {
                case TestConstants.NAMESPACE:
                    // create Namespace
                    KubeResourceManager.get().createResourceWithWait(new NamespaceBuilder()
                        .editOrNewMetadata()
                            .withName(installationNamespace)
                        .endMetadata()
                        .build()
                    );
                    break;
                case TestConstants.SERVICE_ACCOUNT:
                    ServiceAccount serviceAccount = TestFrameUtils.configFromYaml(file, ServiceAccount.class);
                    KubeResourceManager.get().createOrUpdateResourceWithWait(new ServiceAccountBuilder(serviceAccount)
                        .editMetadata()
                            .withNamespace(installationNamespace)
                        .endMetadata()
                        .build()
                    );
                    break;
                case TestConstants.CLUSTER_ROLE:
                    ClusterRole clusterRole = TestFrameUtils.configFromYaml(file, ClusterRole.class);
                    KubeResourceManager.get().createOrUpdateResourceWithWait(clusterRole);
                    break;
                case TestConstants.CLUSTER_ROLE_BINDING:
                    ClusterRoleBinding clusterRoleBinding = TestFrameUtils.configFromYaml(file, ClusterRoleBinding.class);
                    KubeResourceManager.get().createOrUpdateResourceWithWait(new ClusterRoleBindingBuilder(clusterRoleBinding)
                        .editOrNewMetadata()
                            .withNamespace(installationNamespace)
                        .endMetadata()
                        .editFirstSubject()
                            .withNamespace(installationNamespace)
                        .endSubject()
                        .build()
                    );
                    break;
                case TestConstants.CUSTOM_RESOURCE_DEFINITION_SHORT:
                    CustomResourceDefinition customResourceDefinition = TestFrameUtils.configFromYaml(file, CustomResourceDefinition.class);
                    KubeResourceManager.get().createOrUpdateResourceWithWait(customResourceDefinition);
                    break;
                case TestConstants.DEPLOYMENT:
                    deployKafkaAccessOperator(file);
                    break;
                default:
                    // nothing to do, skipping
                    break;
            }
        });
    }

    @Override
    public void delete() {
        // nothing to do here, as we are deleting everything using KubeResourceManager, which deletes everything at the
        // end of the tests
    }

    private void deployKafkaAccessOperator(File deploymentFile) {
        Deployment accessOperatorDeployment = TestFrameUtils.configFromYaml(deploymentFile, Deployment.class);

        String deploymentImage = accessOperatorDeployment
            .getSpec()
            .getTemplate()
            .getSpec()
            .getContainers()
            .get(0)
            .getImage();

        accessOperatorDeployment = new DeploymentBuilder(accessOperatorDeployment)
            .editOrNewMetadata()
                .withNamespace(installationNamespace)
            .endMetadata()
            .editSpec()
                .editTemplate()
                    .editSpec()
                        .editContainer(0)
                            .withImage(ImageUtils.changeRegistryOrgAndTag(deploymentImage, Environment.OPERATOR_REGISTRY, Environment.OPERATOR_ORG, Environment.OPERATOR_TAG))
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();

        KubeResourceManager.get().createResourceWithWait(accessOperatorDeployment);
    }
}