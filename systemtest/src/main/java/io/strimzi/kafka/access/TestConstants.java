/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import java.time.Duration;

/**
 * Interface for keeping global constants used across system tests.
 */
public interface TestConstants {
    String USER_PATH = System.getProperty("user.dir");

    String INSTALL_PATH = USER_PATH + "/../packaging/install/";
    String HELM_CHARTS_PATH = USER_PATH + "/../packaging/helm-charts/helm3/kafka-access-operator";

    //--------------------------
    // Resource types
    //--------------------------
    String NAMESPACE = "Namespace";
    String DEPLOYMENT = "Deployment";
    String SERVICE_ACCOUNT = "ServiceAccount";
    String CLUSTER_ROLE = "ClusterRole";
    String CLUSTER_ROLE_BINDING = "ClusterRoleBinding";
    String CUSTOM_RESOURCE_DEFINITION_SHORT = "Crd";

    //--------------------------
    // KafkaUser Secret fields
    //--------------------------
    String USER_CRT = "user.crt";
    String USER_KEY = "user.key";
    String PASSWORD = "password";
    String SASL_JAAS_CONFIG = "sasl.jaas.config";

    //--------------------------
    // Labels
    //--------------------------

    //--------------------------
    // Access Secret's fields
    //--------------------------
    String BOOTSTRAP_SERVERS = "bootstrapServers";
    String SECURITY_PROTOCOL = "securityProtocol";
    String SSL_KEYSTORE_CRT = "ssl.keystore.crt";
    String SSL_KEYSTORE_KEY = "ssl.keystore.key";

    //--------------------------
    // Duration constants
    //--------------------------
    long GLOBAL_POLL_INTERVAL_SMALL_MS = Duration.ofSeconds(1).toMillis();
    long GLOBAL_TIMEOUT_SMALL_MS = Duration.ofMinutes(2).toMillis();
}
