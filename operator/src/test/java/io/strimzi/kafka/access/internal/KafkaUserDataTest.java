/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.internal;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserTlsClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserTlsExternalClientAuthentication;
import io.strimzi.kafka.access.ResourceProvider;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.strimzi.kafka.access.Base64Encoder.encodeUtf8;
import static org.assertj.core.api.Assertions.assertThat;

public class KafkaUserDataTest {
    private static final String USERNAME = "my-user";
    private static final String SECRET_NAME = "my-secret";

    @Test
    @DisplayName("When a ScramSha512 KafkaUserData is created with a secret, then the connection data contains the SASL properties")
    void testKafkaUserDataScramSha512Secret() {
        final String password = encodeUtf8("password");
        final String saslJaasConfig = encodeUtf8("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"my-user-scram\" password=\"password\";");
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(SECRET_NAME, USERNAME, new KafkaUserScramSha512ClientAuthentication());

        final Map<String, String> kafkaUserSecretData = new HashMap<>();
        kafkaUserSecretData.put("password", password);
        kafkaUserSecretData.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        final Secret kafkaUserSecret = new SecretBuilder().withData(kafkaUserSecretData).build();

        final Map<String, String> secretData = new KafkaUserData(kafkaUser).withSecret(kafkaUserSecret).getConnectionSecretData();
        assertThat(secretData.get("username")).isEqualTo(encodeUtf8(USERNAME));
        assertThat(secretData.get("user")).isEqualTo(encodeUtf8(USERNAME));
        assertThat(secretData.get("password")).isEqualTo(password);
        assertThat(secretData.get(SaslConfigs.SASL_JAAS_CONFIG)).isEqualTo(saslJaasConfig);
        assertThat(secretData.get(SaslConfigs.SASL_MECHANISM)).isEqualTo(encodeUtf8("SCRAM-SHA-512"));
        assertThat(secretData.get("saslMechanism")).isEqualTo(encodeUtf8("SCRAM-SHA-512"));
    }

    @Test
    @DisplayName("When a ScramSha512 KafkaUserData is created with a secret and the status is missing, then the connection data contains the SASL properties without the username")
    void testKafkaUserDataScramSha512MissingStatus() {
        final String password = encodeUtf8("password");
        final String saslJaasConfig = encodeUtf8("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"my-user-scram\" password=\"password\";");
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
        assertThat(secretData.get(SaslConfigs.SASL_MECHANISM)).isEqualTo(encodeUtf8("SCRAM-SHA-512"));
        assertThat(secretData.get("saslMechanism")).isEqualTo(encodeUtf8("SCRAM-SHA-512"));
    }

    @Test
    @DisplayName("When a ScramSha512 KafkaUserData is created without a secret, then the connection data contains only the username and SASL mechanism")
    void testKafkaUserSecretDataSasl() {
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(SECRET_NAME, USERNAME, new KafkaUserScramSha512ClientAuthentication());

        final Map<String, String> secretData = new KafkaUserData(kafkaUser).getConnectionSecretData();
        assertThat(secretData.get("username")).isEqualTo(encodeUtf8(USERNAME));
        assertThat(secretData.get("user")).isEqualTo(encodeUtf8(USERNAME));
        assertThat(secretData.get("password")).isNull();
        assertThat(secretData.get(SaslConfigs.SASL_JAAS_CONFIG)).isNull();
        assertThat(secretData.get(SaslConfigs.SASL_MECHANISM)).isEqualTo(encodeUtf8("SCRAM-SHA-512"));
        assertThat(secretData.get("saslMechanism")).isEqualTo(encodeUtf8("SCRAM-SHA-512"));
    }

    @Test
    @DisplayName("When a tls KafkaUserData is created with a secret, then the connection data contains the correct certificate properties")
    void testKafkaUserDataTlsSecret() {
        final String cert = encodeUtf8("-----BEGIN CERTIFICATE-----\nMIIFLTCCAx\n-----END CERTIFICATE-----\n");
        final String key = encodeUtf8("-----BEGIN PRIVATE KEY-----\nMIIEvA\n-----END PRIVATE KEY-----\n");
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(SECRET_NAME, USERNAME, new KafkaUserTlsClientAuthentication());

        final Map<String, String> kafkaUserSecretData = new HashMap<>();
        kafkaUserSecretData.put("user.crt", cert);
        kafkaUserSecretData.put("user.key", key);
        final Secret kafkaUserSecret = new SecretBuilder().withData(kafkaUserSecretData).build();

        final Map<String, String> secretData = new KafkaUserData(kafkaUser).withSecret(kafkaUserSecret).getConnectionSecretData();
        assertThat(secretData.get("ssl.keystore.crt")).isEqualTo(cert);
        assertThat(secretData.get("ssl.keystore.key")).isEqualTo(key);
    }

    @Test
    @DisplayName("When a tls KafkaUserData is created without a secret, then the connection data contains only the username and SASL mechanism")
    void testKafkaUserDataTlsNoSecret() {
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(SECRET_NAME, USERNAME, new KafkaUserTlsClientAuthentication());
        final Map<String, String> secretData = new KafkaUserData(kafkaUser).getConnectionSecretData();
        assertThat(secretData).isEmpty();
    }

    @Test
    @DisplayName("When a tls external KafkaUserData is created with a secret, then the connection data contains the correct certificate properties")
    void testKafkaUserDataTlsExternalSecret() {
        final String cert = encodeUtf8("-----BEGIN CERTIFICATE-----\nMIIFLTCCAx\n-----END CERTIFICATE-----\n");
        final String key = encodeUtf8("-----BEGIN PRIVATE KEY-----\nMIIEvA\n-----END PRIVATE KEY-----\n");
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(SECRET_NAME, USERNAME, new KafkaUserTlsExternalClientAuthentication());

        final Map<String, String> kafkaUserSecretData = new HashMap<>();
        kafkaUserSecretData.put("user.crt", cert);
        kafkaUserSecretData.put("user.key", key);
        final Secret kafkaUserSecret = new SecretBuilder().withData(kafkaUserSecretData).build();

        final Map<String, String> secretData = new KafkaUserData(kafkaUser).withSecret(kafkaUserSecret).getConnectionSecretData();
        assertThat(secretData.get("ssl.keystore.crt")).isEqualTo(cert);
        assertThat(secretData.get("ssl.keystore.key")).isEqualTo(key);
    }

    @Test
    @DisplayName("When a tls external KafkaUserData is created without a secret, then the connection data contains only the username and SASL mechanism")
    void testKafkaUserDataTlsExternalNoSecret() {
        final KafkaUser kafkaUser = ResourceProvider.getKafkaUserWithStatus(SECRET_NAME, USERNAME, new KafkaUserTlsExternalClientAuthentication());
        final Map<String, String> secretData = new KafkaUserData(kafkaUser).getConnectionSecretData();
        assertThat(secretData).isEmpty();
    }
}
