/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.installation;

import com.marcnuri.helm.Helm;
import com.marcnuri.helm.InstallCommand;
import com.marcnuri.helm.UninstallCommand;
import io.skodjob.testframe.installation.InstallationMethod;
import io.strimzi.kafka.access.systemtest.utils.Environment;
import io.strimzi.kafka.access.systemtest.utils.TestConstants;

import java.nio.file.Paths;

public class HelmInstallation implements InstallationMethod {

    private final String installationNamespace;

    public HelmInstallation(String installationNamespace) {
        this.installationNamespace = installationNamespace;
    }

    public static final String HELM_RELEASE_NAME = "kao-systemtests";

    @Override
    public void install() {
        InstallCommand installCommand = new Helm(Paths.get(TestConstants.HELM_CHARTS_PATH))
            .install()
            .withName(HELM_RELEASE_NAME)
            .withNamespace(installationNamespace)
            .waitReady();

        if (Environment.OPERATOR_REGISTRY != null) {
            // image registry config
            installCommand.set("image.registry", Environment.OPERATOR_REGISTRY);
        }

        if (Environment.OPERATOR_ORG != null) {
            // image repository config
            installCommand.set("image.repository", Environment.OPERATOR_ORG);
        }

        if (Environment.OPERATOR_TAG != null) {
            // image tags config
            installCommand.set("image.tag", Environment.OPERATOR_TAG);
        }

        installCommand.call();
    }

    @Override
    public void delete() {
        Helm.uninstall(HELM_RELEASE_NAME)
            .withCascade(UninstallCommand.Cascade.ORPHAN)
            .call();
    }
}
