/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.skodjob.testframe.enums.InstallType;
import io.skodjob.testframe.environment.TestEnvironmentVariables;

public class Environment {

    private static final TestEnvironmentVariables ENVIRONMENT_VARIABLES = new TestEnvironmentVariables();

    //---------------------------------------
    // Env variables initialization
    //---------------------------------------
    private static final String INSTALL_TYPE_ENV = "INSTALL_TYPE";
    public static final InstallType INSTALL_TYPE = ENVIRONMENT_VARIABLES.getOrDefault(INSTALL_TYPE_ENV, InstallType::fromString, InstallType.Yaml);

    private static final String OPERATOR_REGISTRY_ENV = "DOCKER_REGISTRY";
    public static final String OPERATOR_REGISTRY = ENVIRONMENT_VARIABLES.getOrDefault(OPERATOR_REGISTRY_ENV, null);

    private static final String OPERATOR_ORG_ENV = "DOCKER_ORG";
    public static final String OPERATOR_ORG = ENVIRONMENT_VARIABLES.getOrDefault(OPERATOR_ORG_ENV, null);

    private static final String OPERATOR_TAG_ENV = "DOCKER_TAG";
    public static final String OPERATOR_TAG = ENVIRONMENT_VARIABLES.getOrDefault(OPERATOR_TAG_ENV, null);

    static {
        ENVIRONMENT_VARIABLES.logEnvironmentVariables();
    }
}
