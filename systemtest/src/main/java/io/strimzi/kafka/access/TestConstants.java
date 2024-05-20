/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

/**
 * Interface for keeping global constants used across system tests.
 */
public interface TestConstants {
    String USER_PATH = System.getProperty("user.dir");

    String INSTALL_PATH = USER_PATH + "/packaging/install/";
    String HELM_CHARTS_PATH = USER_PATH + "/packaging/helm-charts/helm3/kafka-access-operator";

    //--------------------------
    // Resource types
    //--------------------------
    String NAMESPACE = "Namespace";
    String DEPLOYMENT = "Deployment";
    String SERVICE_ACCOUNT = "ServiceAccount";
    String CLUSTER_ROLE = "ClusterRole";
    String CLUSTER_ROLE_BINDING = "ClusterRoleBinding";
    String CUSTOM_RESOURCE_DEFINITION_SHORT = "Crd";
}
