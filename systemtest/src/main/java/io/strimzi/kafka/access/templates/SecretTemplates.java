/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.templates;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.strimzi.kafka.access.TestConstants;

import java.util.HashMap;
import java.util.Map;

public class SecretTemplates {

    private SecretTemplates() {}

    public static Secret tlsSecretForUser(String namespaceName, String userName, String clusterName, String userKey, String userCrt, String userP12, String userPassword) {
        Map<String, String> data = Map.of(
            TestConstants.USER_KEY, userKey,
            TestConstants.USER_CRT, userCrt,
            TestConstants.USER_P12, userP12,
            TestConstants.USER_PASSWORD, userPassword
        );

        return defaultSecretForUser(namespaceName, userName, clusterName, data);
    }

    public static Secret scramShaSecretForUser(String namespaceName, String userName, String clusterName, String password, String saslJaasConfig) {
        Map<String, String> data = Map.of(
            TestConstants.PASSWORD, password,
            TestConstants.SASL_JAAS_CONFIG, saslJaasConfig
        );

        return defaultSecretForUser(namespaceName, userName, clusterName, data);
    }

    private static Secret defaultSecretForUser(String namespaceName, String userName, String clusterName, Map<String, String> data) {
        final Map<String, String> labels = new HashMap<>();

        labels.put("app.kubernetes.io/managed-by", "strimzi-user-operator");
        labels.put("strimzi.io/cluster", clusterName);

        return new SecretBuilder()
            .withNewMetadata()
                .withName(userName)
                .withNamespace(namespaceName)
                .withLabels(labels)
            .endMetadata()
            .addToData(data)
            .build();
    }
}
