/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.javaoperatorsdk.operator.Operator;
import io.strimzi.kafka.access.server.HealthServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * The main operator class for Strimzi Access Operator
 */
public class KafkaAccessOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAccessOperator.class);
    private static final int HEALTH_CHECK_PORT = 8080;
    private final static String ANY_NAMESPACE = "*";

    /**
     * Parses namespace configuration from a string
     *
     * @param namespacesList    Comma-separated list of namespaces or "*" for all namespaces
     * @return                  Set of namespace names
     */
    private static Set<String> parseNamespaces(String namespacesList) {
        Set<String> namespaces;
        if (namespacesList.equals(ANY_NAMESPACE)) {
            namespaces = Collections.singleton(ANY_NAMESPACE);
        } else {
            if (namespacesList.trim().equals(ANY_NAMESPACE)) {
                namespaces = Collections.singleton(ANY_NAMESPACE);
            } else if (namespacesList.matches("(\\s*[a-z0-9.-]+\\s*,)*\\s*[a-z0-9.-]+\\s*")) {
                namespaces = new HashSet<>(asList(namespacesList.trim().split("\\s*,+\\s*")));
            } else {
                throw new IllegalArgumentException("Not a valid list of namespaces nor the 'any namespace' wildcard "
                        + ANY_NAMESPACE);
            }
        }
        return namespaces;
    }

    /**
     * Initializes the operator and runs a servlet for health checking
     *
     * @param args      Main method arguments
     */
    public static void main(final String[] args) {
        LOGGER.info("Kafka Access operator starting");
        final Operator operator = new Operator(overrider -> overrider
                .withUseSSAToPatchPrimaryResource(false));

        String accessNamespaceConfig = System.getenv().getOrDefault("ACCESS_WATCHED_NAMESPACES", "*");
        Set<String> accessNamespaces = parseNamespaces(accessNamespaceConfig);
        String kafkaNamespaceConfig = System.getenv().getOrDefault("KAFKA_WATCHED_NAMESPACES", "*");
        Set<String> kafkaNamespaces = parseNamespaces(kafkaNamespaceConfig);

        operator.register(new KafkaAccessReconciler(operator.getKubernetesClient(), kafkaNamespaces),
            configuration -> configuration.settingNamespaces(accessNamespaces));

        try {
            operator.start();
            LOGGER.info("Kafka Access operator started successfully. Watching KafkaAccess in namespaces {}, Kafka resources in namespaces: {}", accessNamespaces, kafkaNamespaces);
        } catch (Exception e) {
            LOGGER.error("Failed to start Kafka Access operator. This may be due to missing RoleBindings for one or more namespaces. " +
                    "Access namespaces: {}, Kafka namespaces: {}. " +
                    "Please ensure that appropriate RoleBindings exist for all configured namespaces. Error: {}",
                    accessNamespaces, kafkaNamespaces, e.getMessage(), e);
            LOGGER.warn("Operator will continue running but may not be able to reconcile resources in namespaces without proper permissions");
        }

        Server server = new Server(HEALTH_CHECK_PORT);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(HealthServlet.class, "/healthy");
        handler.addServletWithMapping(HealthServlet.class, "/ready");
        try {
            server.start();
            LOGGER.info("Kafka Access operator is now ready (health server listening)");
            server.join();
        } catch (Exception e) {
            LOGGER.error("Failed to start health server", e);
        }
    }
}
