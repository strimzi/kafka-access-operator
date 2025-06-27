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
    String HELM_CHARTS_PATH = USER_PATH + "/../packaging/helm-charts/helm3/strimzi-access-operator";

    //--------------------------
    // Strimzi related constants
    //--------------------------
    // in case of change in the pom.xml, update this one as well please
    String STRIMZI_API_VERSION = "0.41.0";

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
    String USER_P12 = "user.p12";
    String USER_PASSWORD = "user.password";
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
    String SSL_KEYSTORE_P12 = "ssl.keystore.p12";
    String SSL_KEYSTORE_PASSWORD = "ssl.keystore.password";

    //--------------------------
    // Duration constants
    //--------------------------
    long GLOBAL_POLL_INTERVAL_SHORT_MS = Duration.ofSeconds(1).toMillis();
    long GLOBAL_TIMEOUT_SHORT_MS = Duration.ofMinutes(2).toMillis();
}
