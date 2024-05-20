/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.installation;

import io.skodjob.testframe.enums.InstallType;
import io.skodjob.testframe.installation.InstallationMethod;
import io.strimzi.kafka.access.systemtest.utils.Environment;

public class SetupAccessOperator {
    private final InstallationMethod installationMethod;
    private final String installationNamespace;

    public SetupAccessOperator(String installationNamespace) {
        this.installationNamespace = installationNamespace;
        this.installationMethod = getInstallationMethod();
    }

    public void install() {
        installationMethod.install();
    }

    public void delete() {
        installationMethod.delete();
    }

    private InstallationMethod getInstallationMethod() {
        return Environment.INSTALL_TYPE == InstallType.Helm ? new HelmInstallation(installationNamespace) : new BundleInstallation(installationNamespace);
    }
}
