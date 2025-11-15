/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Serial;

/**
 * Servlet class for health checking of the operator.
 * This servlet provides a simple health check endpoint.
 */
public class HealthServlet extends HttpServlet {

    /**
     * Creates a new HealthServlet.
     * This explicit constructor documents the default servlet instantiation.
     */
    public HealthServlet() {
        super();
    }

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
