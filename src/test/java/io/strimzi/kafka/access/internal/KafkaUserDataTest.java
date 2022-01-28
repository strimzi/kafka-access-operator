/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.KafkaUserTlsClientAuthentication;
import io.strimzi.api.kafka.model.KafkaUserTlsExternalClientAuthentication;
import io.strimzi.kafka.access.ResourceProvider;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class KafkaUserDataTest {
    private static final String USERNAME = "my-user";
    private static final String SECRET_NAME = "my-secret";
    private final Base64.Encoder encoder = Base64.getEncoder();

    @Test
    @DisplayName("When a ScramSha512 KafkaUserData is created with a secret, then the connection data contains the SASL properties")
    void testKafkaUserDataScramSha512Secret() {
        final String password = encodeToString("password");
        final String saslJaasConfig = encodeToString("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"my-user-scram\" password=\"password\";");
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(SECRET_NAME, USERNAME, new KafkaUserScramSha512ClientAuthentication());

        final Map<String, String> kafkaUserSecretData = new HashMap<>();
        kafkaUserSecretData.put("password", password);
        kafkaUserSecretData.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        final Secret kafkaUserSecret = new SecretBuilder().withData(kafkaUserSecretData).build();

        final Map<String, String> secretData = new KafkaUserData(kafkaUser).withSecret(kafkaUserSecret).getConnectionSecretData();
        assertThat(secretData.get("username")).isEqualTo(encodeToString(USERNAME));
        assertThat(secretData.get("user")).isEqualTo(encodeToString(USERNAME));
        assertThat(secretData.get("password")).isEqualTo(password);
        assertThat(secretData.get(SaslConfigs.SASL_JAAS_CONFIG)).isEqualTo(saslJaasConfig);
        assertThat(secretData.get(SaslConfigs.SASL_MECHANISM)).isEqualTo(encodeToString("SCRAM-SHA-512"));
        assertThat(secretData.get("saslMechanism")).isEqualTo(encodeToString("SCRAM-SHA-512"));
    }

    @Test
    @DisplayName("When a ScramSha512 KafkaUserData is created with a secret and the status is missing, then the connection data contains the SASL properties without the username")
    void testKafkaUserDataScramSha512MissingStatus() {
        final String password = encodeToString("password");
        final String saslJaasConfig = encodeToString("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"my-user-scram\" password=\"password\";");
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUser("test", "test", new KafkaUserScramSha512ClientAuthentication());

        final Map<String, String> kafkaUserSecretData = new HashMap<>();
        kafkaUserSecretData.put("password", password);
        kafkaUserSecretData.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        final Secret kafkaUserSecret = new SecretBuilder().withData(kafkaUserSecretData).build();

        final Map<String, String> secretData = new KafkaUserData(kafkaUser).withSecret(kafkaUserSecret).getConnectionSecretData();
        assertThat(secretData.get("username")).isNull();
        assertThat(secretData.get("user")).isNull();
        assertThat(secretData.get("password")).isEqualTo(password);
        assertThat(secretData.get(SaslConfigs.SASL_JAAS_CONFIG)).isEqualTo(saslJaasConfig);
        assertThat(secretData.get(SaslConfigs.SASL_MECHANISM)).isEqualTo(encodeToString("SCRAM-SHA-512"));
        assertThat(secretData.get("saslMechanism")).isEqualTo(encodeToString("SCRAM-SHA-512"));
    }

    @Test
    @DisplayName("When a ScramSha512 KafkaUserData is created without a secret, then the connection data contains only the username and SASL mechanism")
    void testKafkaUserSecretDataSasl() {
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(SECRET_NAME, USERNAME, new KafkaUserScramSha512ClientAuthentication());

        final Map<String, String> secretData = new KafkaUserData(kafkaUser).getConnectionSecretData();
        assertThat(secretData.get("username")).isEqualTo(encodeToString(USERNAME));
        assertThat(secretData.get("user")).isEqualTo(encodeToString(USERNAME));
        assertThat(secretData.get("password")).isNull();
        assertThat(secretData.get(SaslConfigs.SASL_JAAS_CONFIG)).isNull();
        assertThat(secretData.get(SaslConfigs.SASL_MECHANISM)).isEqualTo(encodeToString("SCRAM-SHA-512"));
        assertThat(secretData.get("saslMechanism")).isEqualTo(encodeToString("SCRAM-SHA-512"));
    }

    static Stream<Arguments> nonImplementedUserData() {
        return Stream.of(
                arguments(named(KafkaUserTlsClientAuthentication.TYPE_TLS, new KafkaUserTlsClientAuthentication())),
                arguments(named(KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL, new KafkaUserTlsExternalClientAuthentication()))
        );
    }

    @ParameterizedTest
    @MethodSource("nonImplementedUserData")
    @DisplayName("When the KafkaUserData with non implemented secret data is created, then the connection data is empty")
    void testKafkaUserSecretDataNotImplemented(final KafkaUserAuthentication kafkaUserAuthentication) {
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUser("test", "test", kafkaUserAuthentication);
        final Secret kafkaUserSecret = new SecretBuilder().withData(new HashMap<>()).build();

        final Map<String, String> secretData = new KafkaUserData(kafkaUser).withSecret(kafkaUserSecret).getConnectionSecretData();
        assertThat(secretData).hasSize(0);
    }

    private String encodeToString(String data) {
        return encoder.encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
}
