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

/**
 * The main operator class for Strimzi Access Operator
 */
public class KafkaAccessOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAccessOperator.class);
    private static final int HEALTH_CHECK_PORT = 8080;

    /**
     * Initializes the operator and runs a servlet for health checking
     *
     * @param args      Main method arguments
     */
    public static void main(final String[] args) {
        LOGGER.info("Kafka Access operator starting");
        final Operator operator = new Operator();
        operator.register(new KafkaAccessReconciler(operator.getKubernetesClient()));
        operator.start();
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
