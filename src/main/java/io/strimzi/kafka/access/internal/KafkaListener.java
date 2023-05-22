/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.strimzi.api.kafka.model.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationTls;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.strimzi.kafka.access.internal.KafkaParser.LISTENER_AUTH_NONE;

/**
 * Representation of a Kafka listener that returns the connection details for the listener
 */

public class KafkaListener {

    private final String name;
    private final KafkaListenerType type;
    private final boolean tls;
    private final String authenticationType;
    private String bootstrapServer;

    /**
     * @param genericListener
     */
    public KafkaListener(final GenericKafkaListener genericListener) {
        this.name = genericListener.getName();
        this.type = genericListener.getType();
        this.tls = genericListener.isTls();
        this.authenticationType = Optional.ofNullable(genericListener.getAuth()).map(KafkaListenerAuthentication::getType).orElse(LISTENER_AUTH_NONE);
    }

    /**
     * @param bootstrapServer
     * @return
     */
    public KafkaListener withBootstrapServer(final String bootstrapServer) {
        this.bootstrapServer = bootstrapServer;
        return this;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public KafkaListenerType getType() {
        return type;
    }

    /**
     * @return
     */
    public String getBootstrapServer() {
        return bootstrapServer;
    }

    /**
     * @return
     */
    public boolean isTls() {
        return tls;
    }

    /**
     * @return
     */
    public String getAuthenticationType() {
        return authenticationType;
    }

    /**
     * Collects the connection data for the Kafka resource
     *
     * @return A map of the connection data
     */
    public Map<String, String> getConnectionSecretData() {
        final Base64.Encoder encode = Base64.getEncoder();
        final SecurityProtocol securityProtocol = getSecurityProtocol();
        final Map<String, String> data = new HashMap<>();
        final String bootstrapServers = encode.encodeToString(this.bootstrapServer.getBytes(StandardCharsets.UTF_8));
        data.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        final String encodedSecurityProtocol = encode.encodeToString(securityProtocol.name.getBytes(StandardCharsets.UTF_8));
        data.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, encodedSecurityProtocol);
        //quarkus settings
        data.put("bootstrapServers", bootstrapServers);
        data.put("securityProtocol", encodedSecurityProtocol);
        //Spring settings
        data.put("bootstrap-servers", bootstrapServers);
        return data;
    }

    /**
     * @return
     */
    private SecurityProtocol getSecurityProtocol() {
        final SecurityProtocol securityProtocol;
        switch (this.authenticationType) {
            case LISTENER_AUTH_NONE:
                securityProtocol = this.tls ? SecurityProtocol.SSL : SecurityProtocol.PLAINTEXT;
                break;
            case KafkaListenerAuthenticationTls.TYPE_TLS:
                securityProtocol = SecurityProtocol.SSL;
                break;
            case KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512:
                securityProtocol = this.tls ? SecurityProtocol.SASL_SSL : SecurityProtocol.SASL_PLAINTEXT;
                break;
            default:
                securityProtocol = SecurityProtocol.PLAINTEXT;
        }
        return securityProtocol;
    }
}
