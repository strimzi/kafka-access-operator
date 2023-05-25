/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.fabric8.kubernetes.api.model.Secret;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.KafkaUserSpec;
import io.strimzi.api.kafka.model.status.KafkaUserStatus;
import org.apache.kafka.common.config.SaslConfigs;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.strimzi.kafka.access.internal.KafkaParser.USER_AUTH_UNDEFINED;

/**
 *  Representation of a Kafka user data that returns the secret details
 */
public class KafkaUserData {

    private final Map<String, String> rawUserData = new HashMap<>();
    private final String authType;

    /**
     * Constructor
     *
     * @param kafkaUser The KafkaUser resource
     */
    public KafkaUserData(final KafkaUser kafkaUser) {
        Optional.ofNullable(kafkaUser.getStatus())
                .map(KafkaUserStatus::getUsername)
                .ifPresent(username -> rawUserData.put("username", Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8))));
        this.authType = Optional.ofNullable(kafkaUser.getSpec())
                .map(KafkaUserSpec::getAuthentication)
                .map(KafkaUserAuthentication::getType)
                .orElse(USER_AUTH_UNDEFINED);
    }

    /**
     * Decorates a KafkaUserData instance with secret information
     *
     * @param secret    The Kubernetes secret
     * @return          A KafkaUserData instance
     */
    public KafkaUserData withSecret(final Secret secret) {
        Optional.ofNullable(secret.getData())
                .ifPresent(rawUserData::putAll);
        return this;
    }

    /**
     * Collects the connection data for connecting to Kafka with this Kafka User
     *
     * @return A map of the connection data
     */
    public Map<String, String> getConnectionSecretData() {
        final Map<String, String> secretData = new HashMap<>();
        if (KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512.equals(authType)) {
            Optional.ofNullable(rawUserData.get("username"))
                    .ifPresent(username -> {
                        secretData.put("username", username);
                        secretData.put("user", username); //quarkus
                    });
            final String encodedSaslMechanism = Base64.getEncoder().encodeToString("SCRAM-SHA-512".getBytes(StandardCharsets.UTF_8));
            secretData.put(SaslConfigs.SASL_MECHANISM, encodedSaslMechanism);
            secretData.put("saslMechanism", encodedSaslMechanism); //quarkus
            Optional.ofNullable(rawUserData.get("password"))
                    .ifPresent(password -> secretData.put("password", password));
            Optional.ofNullable(rawUserData.get(SaslConfigs.SASL_JAAS_CONFIG))
                    .ifPresent(jaasConfig -> secretData.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig));
        }
        return secretData;
    }
}
