/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.kafka.access.ResourceProvider;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaListenerTest {

    private static final String LISTENER_1 = "listener-1";
    private static final String BOOTSTRAP_SERVER_9092 = "my-cluster.svc:9092";

    private final Base64.Encoder encoder = Base64.getEncoder();

    protected static List<String> bootstrapServerKeys() {
        return List.of(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                "bootstrapServers",
                "bootstrap-servers"
        );
    }

    protected static List<String> securityProtocolKeys() {
        return List.of(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                "securityProtocol"
        );
    }

    @Test
    @DisplayName("When a Kafka listener with no auth is created, then the connection data is correct and the security protocol is PLAINTEXT")
    void testSpecifiedPlainListener() {
        final GenericKafkaListener genericKafkaListener = ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false);
        final KafkaListener listener = new KafkaListener(genericKafkaListener).withBootstrapServer(BOOTSTRAP_SERVER_9092);

        final Map<String, String> secretData = listener.getConnectionSecretData();

        final Map<String, String> expectedData = new HashMap<>();
        bootstrapServerKeys().forEach(key -> expectedData.put(key, encodeToString(BOOTSTRAP_SERVER_9092)));
        securityProtocolKeys().forEach(key -> expectedData.put(key, encodeToString(SecurityProtocol.PLAINTEXT.name)));
        assertThat(secretData).containsAllEntriesOf(expectedData);
    }



    @Test
    @DisplayName("When Kafka listener with TLS enabled is created, then the connection data is correct and the security protocol is SSL")
    void testTLSKafkaListener() {
        final String cert = encodeToString("-----BEGIN CERTIFICATE-----\nMIIFLTCCAx\n-----END CERTIFICATE-----\n");
        final Map<String, String> certSecretData = new HashMap<>();
        certSecretData.put("ca.crt", cert);
        final GenericKafkaListener genericKafkaListener = ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true);
        final KafkaListener listener = new KafkaListener(genericKafkaListener).withBootstrapServer(BOOTSTRAP_SERVER_9092)
                .withCaCertSecret(certSecretData);

        final Map<String, String> secretData = listener.getConnectionSecretData();

        final Map<String, String> expectedData = new HashMap<>();
        bootstrapServerKeys().forEach(key -> expectedData.put(key, encodeToString(BOOTSTRAP_SERVER_9092)));
        securityProtocolKeys().forEach(key -> expectedData.put(key, encodeToString(SecurityProtocol.SSL.name)));
        expectedData.put("ssl.truststore.crt", cert);
        assertThat(secretData).containsAllEntriesOf(expectedData);
    }

    @Test
    @DisplayName("When a Kafka listener with SASL auth is created, then the connection data is correct and the security protocol is SASL_PLAINTEXT")
    void testKafkaListenerWithSaslAuth() {
        final GenericKafkaListener genericKafkaListener = ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, false, new KafkaListenerAuthenticationScramSha512());
        final KafkaListener listener = new KafkaListener(genericKafkaListener).withBootstrapServer(BOOTSTRAP_SERVER_9092);

        final Map<String, String> secretData = listener.getConnectionSecretData();

        final Map<String, String> expectedData = new HashMap<>();
        bootstrapServerKeys().forEach(key -> expectedData.put(key, encodeToString(BOOTSTRAP_SERVER_9092)));
        securityProtocolKeys().forEach(key -> expectedData.put(key, encodeToString(SecurityProtocol.SASL_PLAINTEXT.name)));
        assertThat(secretData).containsAllEntriesOf(expectedData);
    }

    @Test
    @DisplayName("When a Kafka listener with SASL auth and TLS enabled is created, then the connection data is correct and has security protocol SASL_SSL")
    void testKafkaListenerWithSaslAuthAndTls() {
        final String cert = encodeToString("-----BEGIN CERTIFICATE-----\nMIIFLTCCAx\n-----END CERTIFICATE-----\n");
        final Map<String, String> certSecretData = new HashMap<>();
        certSecretData.put("ca.crt", cert);
        final GenericKafkaListener genericKafkaListener = ResourceProvider.getListener(LISTENER_1, KafkaListenerType.INTERNAL, true, new KafkaListenerAuthenticationScramSha512());
        final KafkaListener listener = new KafkaListener(genericKafkaListener).withBootstrapServer(BOOTSTRAP_SERVER_9092)
                .withCaCertSecret(certSecretData);

        final Map<String, String> secretData = listener.getConnectionSecretData();

        final Map<String, String> expectedData = new HashMap<>();
        bootstrapServerKeys().forEach(key -> expectedData.put(key, encodeToString(BOOTSTRAP_SERVER_9092)));
        securityProtocolKeys().forEach(key -> expectedData.put(key, encodeToString(SecurityProtocol.SASL_SSL.name)));
        expectedData.put("ssl.truststore.crt", cert);
        assertThat(secretData).containsAllEntriesOf(expectedData);
    }



    private String encodeToString(String data) {
        return encoder.encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

}
