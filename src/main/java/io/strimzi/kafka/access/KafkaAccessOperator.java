/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaAccessOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAccessOperator.class);

    public static void main(final String[] args) {
        LOGGER.info("Kafka Access operator starting");
        final Config config = new ConfigBuilder().withNamespace(null).build();
        final KubernetesClient client = new DefaultKubernetesClient(config);
        final Operator operator = new Operator(client, DefaultConfigurationService.instance());
        operator.register(new KafkaAccessReconciler(client));
        operator.installShutdownHook();
        operator.start();
        LOGGER.info("Kafka Access operator started");
    }
}
