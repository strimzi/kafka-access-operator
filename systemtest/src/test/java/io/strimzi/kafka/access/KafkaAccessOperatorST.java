/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access;

import io.fabric8.kubernetes.api.model.Secret;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationTls;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserTlsClientAuthentication;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.resources.KafkaAccessType;
import io.strimzi.kafka.access.templates.KafkaAccessTemplates;
import io.strimzi.kafka.access.templates.KafkaTemplates;
import io.strimzi.kafka.access.templates.KafkaUserTemplates;
import io.strimzi.kafka.access.templates.ListenerTemplates;
import io.strimzi.kafka.access.templates.SecretTemplates;
import io.strimzi.kafka.access.utils.Base64Utils;
import io.strimzi.kafka.access.utils.KafkaAccessUtils;
import io.strimzi.kafka.access.utils.ListenerUtils;
import io.strimzi.kafka.access.utils.SecretUtils;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class KafkaAccessOperatorST extends AbstractST {

    private final String defaultHost = "my-host.svc";

    private final String nodePortListenerName = "nodeport";
    private final int nodePortListenerPort = 9098;

    private final String internalListenerName = "internal";
    private final int internalListenerPort = 9093;

    private final String loadBalanceListenerName = "loadbal";
    private final int loadBalanceListenerPort = 9099;

    private final String tlsUserName = "tls";
    private final String scramShaUserName = "scramsha";

    @Test
    void testAccessToSpecifiedListener() {
        TestStorage testStorage = new TestStorage();

        String userKey = SecretUtils.createUserKey("my-super-secret-key");
        String userCrt = SecretUtils.createUserCrt("my-super-secret-crt");

        List<GenericKafkaListener> listeners = List.of(
            ListenerTemplates.tlsListener(internalListenerName, KafkaListenerType.INTERNAL, new KafkaListenerAuthenticationTls(), internalListenerPort),
            ListenerTemplates.tlsListener(nodePortListenerName, KafkaListenerType.NODEPORT, new KafkaListenerAuthenticationTls(), nodePortListenerPort),
            ListenerTemplates.listener(loadBalanceListenerName, KafkaListenerType.LOADBALANCER, new KafkaListenerAuthenticationScramSha512(), loadBalanceListenerPort)
        );

        Kafka kafka = KafkaTemplates.kafkaWithListeners(namespace, testStorage.getKafkaClusterName(), defaultHost, listeners).build();
        KafkaUser tlsUser = KafkaUserTemplates.kafkaUser(namespace, tlsUserName, new KafkaUserTlsClientAuthentication());
        Secret tlsUserSecret = SecretTemplates.tlsSecretForUser(namespace, tlsUserName, testStorage.getKafkaClusterName(), userKey, userCrt);

        resourceManager.createResourceWithWait(
            kafka,
            tlsUser,
            KafkaUserTemplates.kafkaUser(namespace, scramShaUserName, new KafkaUserScramSha512ClientAuthentication()),
            tlsUserSecret
        );
        KubeResourceManager.getKubeClient().getClient().resource(kafka).updateStatus();
        KubeResourceManager.getKubeClient().getClient().resource(tlsUser).updateStatus();

        resourceManager.createResourceWithWait(KafkaAccessTemplates.kafkaAccess(namespace, testStorage.getKafkaAccessName())
            .withNewSpec()
                .withNewKafka()
                    .withName(testStorage.getKafkaClusterName())
                    .withNamespace(namespace)
                    .withListener(nodePortListenerName)
                .endKafka()
                .withNewUser()
                    .withApiGroup(KafkaUser.RESOURCE_GROUP)
                    .withKind(KafkaUser.RESOURCE_KIND)
                    .withName(tlsUserName)
                    .withNamespace(namespace)
                .endUser()
            .endSpec()
            .build()
        );

        assertListenerAndUserValuesInAccessSecret(testStorage.getKafkaAccessName(), ListenerUtils.bootstrapServer(defaultHost, nodePortListenerPort), SecurityProtocol.SSL, tlsUserSecret.getData());
    }

    @Test
    void testAccessToSpecifiedListenerAndWrongUser() {
        TestStorage testStorage = new TestStorage();

        String password = Base64Utils.encodeToBase64("my-secret-password");
        String saslJaasConfig = Base64Utils.encodeToBase64("my-config\nsomething;");

        List<GenericKafkaListener> listeners = List.of(
            ListenerTemplates.tlsListener(internalListenerName, KafkaListenerType.INTERNAL, new KafkaListenerAuthenticationTls(), internalListenerPort),
            ListenerTemplates.tlsListener(nodePortListenerName, KafkaListenerType.NODEPORT, new KafkaListenerAuthenticationTls(), nodePortListenerPort),
            ListenerTemplates.listener(loadBalanceListenerName, KafkaListenerType.LOADBALANCER, new KafkaListenerAuthenticationScramSha512(), loadBalanceListenerPort)
        );

        Kafka kafka = KafkaTemplates.kafkaWithListeners(namespace, testStorage.getKafkaClusterName(), defaultHost, listeners).build();
        KafkaUser scramUser = KafkaUserTemplates.kafkaUser(namespace, scramShaUserName, new KafkaUserScramSha512ClientAuthentication());
        Secret scramSecret = SecretTemplates.scramShaSecretForUser(namespace, scramShaUserName, testStorage.getKafkaClusterName(), password, saslJaasConfig);

        resourceManager.createResourceWithWait(
            kafka,
            scramUser,
            scramSecret
        );
        KubeResourceManager.getKubeClient().getClient().resource(kafka).updateStatus();
        KubeResourceManager.getKubeClient().getClient().resource(scramUser).updateStatus();

        resourceManager.createResourceWithoutWait(KafkaAccessTemplates.kafkaAccess(namespace, testStorage.getKafkaAccessName())
            .withNewSpec()
                .withNewKafka()
                    .withName(testStorage.getKafkaClusterName())
                    .withNamespace(namespace)
                    .withListener(nodePortListenerName)
                .endKafka()
                .withNewUser()
                    .withApiGroup(KafkaUser.RESOURCE_GROUP)
                    .withKind(KafkaUser.RESOURCE_KIND)
                    .withName(scramShaUserName)
                    .withNamespace(namespace)
                .endUser()
            .endSpec()
            .build()
        );

        KafkaAccessUtils.waitForKafkaAccessNotReady(namespace, testStorage.getKafkaAccessName());

        KafkaAccess currentAccess = KafkaAccessType.kafkaAccessClient().inNamespace(namespace).withName(testStorage.getKafkaAccessName()).get();
        Condition currentAccessStatus = currentAccess.getStatus().getConditions().stream().filter(condition -> condition.getType().equals(KafkaAccessUtils.READY_TYPE)).findFirst().get();

        assertThat(currentAccessStatus.getMessage().contains("do not have compatible authentication configuration"), is(true));
    }

    @Test
    void testAccessToUnspecifiedSingleListener() {
        TestStorage testStorage = new TestStorage();
        
        List<GenericKafkaListener> listeners = List.of(
            ListenerTemplates.listener(internalListenerName, KafkaListenerType.INTERNAL, new KafkaListenerAuthenticationScramSha512(), internalListenerPort)
        );

        Kafka kafka = KafkaTemplates.kafkaWithListeners(namespace, testStorage.getKafkaClusterName(), defaultHost, listeners).build();

        resourceManager.createResourceWithWait(kafka);
        KubeResourceManager.getKubeClient().getClient().resource(kafka).updateStatus();

        resourceManager.createResourceWithWait(KafkaAccessTemplates.kafkaAccess(namespace, testStorage.getKafkaAccessName())
            .withNewSpec()
                .withNewKafka()
                    .withName(testStorage.getKafkaClusterName())
                    .withNamespace(namespace)
                .endKafka()
            .endSpec()
            .build());

        assertListenerValuesInAccessSecret(testStorage.getKafkaAccessName(), ListenerUtils.bootstrapServer(defaultHost, internalListenerPort), SecurityProtocol.SASL_PLAINTEXT);
    }

    @Test
    void testAccessToUnspecifiedMultipleListeners() {
        TestStorage testStorage = new TestStorage();
        
        String userKey = SecretUtils.createUserKey("my-super-secret-key");
        String userCrt = SecretUtils.createUserCrt("my-super-secret-crt");

        List<GenericKafkaListener> listeners = List.of(
            ListenerTemplates.tlsListener(nodePortListenerName, KafkaListenerType.NODEPORT, new KafkaListenerAuthenticationTls(), nodePortListenerPort),
            ListenerTemplates.listener(loadBalanceListenerName, KafkaListenerType.LOADBALANCER, new KafkaListenerAuthenticationScramSha512(), loadBalanceListenerPort)
        );

        Kafka kafka = KafkaTemplates.kafkaWithListeners(namespace, testStorage.getKafkaClusterName(), defaultHost, listeners).build();
        KafkaUser tlsUser = KafkaUserTemplates.kafkaUser(namespace, tlsUserName, new KafkaUserTlsClientAuthentication());
        Secret tlsUserSecret = SecretTemplates.tlsSecretForUser(namespace, tlsUserName, testStorage.getKafkaClusterName(), userKey, userCrt);

        resourceManager.createResourceWithWait(
            kafka,
            tlsUser,
            tlsUserSecret
        );
        KubeResourceManager.getKubeClient().getClient().resource(kafka).updateStatus();
        KubeResourceManager.getKubeClient().getClient().resource(tlsUser).updateStatus();

        resourceManager.createResourceWithWait(KafkaAccessTemplates.kafkaAccess(namespace, testStorage.getKafkaAccessName())
            .withNewSpec()
            .withNewKafka()
                .withName(testStorage.getKafkaClusterName())
                .withNamespace(namespace)
            .endKafka()
            .withNewUser()
                .withApiGroup(KafkaUser.RESOURCE_GROUP)
                .withKind(KafkaUser.RESOURCE_KIND)
                .withName(tlsUserName)
                .withNamespace(namespace)
            .endUser()
            .endSpec()
            .build()
        );

        assertListenerAndUserValuesInAccessSecret(testStorage.getKafkaAccessName(), ListenerUtils.bootstrapServer(defaultHost, nodePortListenerPort), SecurityProtocol.SSL, tlsUserSecret.getData());
    }

    @Test
    void testAccessToUnspecifiedMultipleListenersWithSingleInternal() {
        TestStorage testStorage = new TestStorage();
        
        String userKey = SecretUtils.createUserKey("my-super-secret-key");
        String userCrt = SecretUtils.createUserCrt("my-super-secret-crt");

        List<GenericKafkaListener> listeners = List.of(
            ListenerTemplates.tlsListener(nodePortListenerName, KafkaListenerType.NODEPORT, new KafkaListenerAuthenticationTls(), nodePortListenerPort),
            ListenerTemplates.tlsListener(internalListenerName, KafkaListenerType.INTERNAL, new KafkaListenerAuthenticationTls(), internalListenerPort),
            ListenerTemplates.tlsListener(loadBalanceListenerName, KafkaListenerType.LOADBALANCER, new KafkaListenerAuthenticationTls(), loadBalanceListenerPort)
        );

        Kafka kafka = KafkaTemplates.kafkaWithListeners(namespace, testStorage.getKafkaClusterName(), defaultHost, listeners).build();
        KafkaUser tlsUser = KafkaUserTemplates.kafkaUser(namespace, tlsUserName, new KafkaUserTlsClientAuthentication());
        Secret tlsUserSecret = SecretTemplates.tlsSecretForUser(namespace, tlsUserName, testStorage.getKafkaClusterName(), userKey, userCrt);

        resourceManager.createResourceWithWait(
            kafka,
            tlsUser,
            tlsUserSecret
        );
        KubeResourceManager.getKubeClient().getClient().resource(kafka).updateStatus();
        KubeResourceManager.getKubeClient().getClient().resource(tlsUser).updateStatus();

        resourceManager.createResourceWithWait(KafkaAccessTemplates.kafkaAccess(namespace, testStorage.getKafkaAccessName())
            .withNewSpec()
                .withNewKafka()
                    .withName(testStorage.getKafkaClusterName())
                    .withNamespace(namespace)
                .endKafka()
                .withNewUser()
                    .withApiGroup(KafkaUser.RESOURCE_GROUP)
                    .withKind(KafkaUser.RESOURCE_KIND)
                    .withName(tlsUserName)
                    .withNamespace(namespace)
                .endUser()
            .endSpec()
            .build()
        );

        assertListenerAndUserValuesInAccessSecret(testStorage.getKafkaAccessName(), ListenerUtils.bootstrapServer(defaultHost, internalListenerPort), SecurityProtocol.SSL, tlsUserSecret.getData());
    }

    @Test
    void testAccessToUnspecifiedMultipleListenersWithMultipleInternal() {
        TestStorage testStorage = new TestStorage();
        
        String listenerA = "ales";
        String listenerK = "karel";
        String listenerT = "tonda";

        int listenerAPort = 9095;
        int listenerKPort = 9098;
        int listenerTPort = 9125;

        String userKey = SecretUtils.createUserKey("my-super-secret-key");
        String userCrt = SecretUtils.createUserCrt("my-super-secret-crt");

        List<GenericKafkaListener> listeners = List.of(
            ListenerTemplates.tlsListener(listenerA, KafkaListenerType.NODEPORT, new KafkaListenerAuthenticationTls(), listenerAPort),
            ListenerTemplates.tlsListener(listenerK, KafkaListenerType.NODEPORT, new KafkaListenerAuthenticationTls(), listenerKPort),
            ListenerTemplates.tlsListener(listenerT, KafkaListenerType.LOADBALANCER, new KafkaListenerAuthenticationTls(), listenerTPort)
        );

        Kafka kafka = KafkaTemplates.kafkaWithListeners(namespace, testStorage.getKafkaClusterName(), defaultHost, listeners).build();
        KafkaUser tlsUser = KafkaUserTemplates.kafkaUser(namespace, tlsUserName, new KafkaUserTlsClientAuthentication());
        Secret tlsUserSecret = SecretTemplates.tlsSecretForUser(namespace, tlsUserName, testStorage.getKafkaClusterName(), userKey, userCrt);

        resourceManager.createResourceWithWait(
            kafka,
            tlsUser,
            tlsUserSecret
        );
        KubeResourceManager.getKubeClient().getClient().resource(kafka).updateStatus();
        KubeResourceManager.getKubeClient().getClient().resource(tlsUser).updateStatus();

        resourceManager.createResourceWithWait(KafkaAccessTemplates.kafkaAccess(namespace, testStorage.getKafkaAccessName())
            .withNewSpec()
                .withNewKafka()
                    .withName(testStorage.getKafkaClusterName())
                    .withNamespace(namespace)
                .endKafka()
                .withNewUser()
                    .withApiGroup(KafkaUser.RESOURCE_GROUP)
                    .withKind(KafkaUser.RESOURCE_KIND)
                    .withName(tlsUserName)
                    .withNamespace(namespace)
                .endUser()
            .endSpec()
            .build()
        );

        assertListenerAndUserValuesInAccessSecret(testStorage.getKafkaAccessName(), ListenerUtils.bootstrapServer(defaultHost, listenerAPort), SecurityProtocol.SSL, tlsUserSecret.getData());
    }

    private void assertListenerValuesInAccessSecret(
        String accessName,
        String expectedBootstrap,
        SecurityProtocol expectedSecurityProtocol
    ) {
        assertListenerAndUserValuesInAccessSecret(accessName, expectedBootstrap, expectedSecurityProtocol, null);
    }

    private void assertListenerAndUserValuesInAccessSecret(
        String accessName,
        String expectedBootstrap,
        SecurityProtocol expectedSecurityProtocol,
        Map<String, String> userData
    ) {
        Secret accessSecret = KubeResourceManager.getKubeClient().getClient().secrets().inNamespace(namespace).withName(accessName).get();
        Map<String, String> data = accessSecret.getData();

        assertThat(Base64Utils.decodeFromBase64ToString(data.get(TestConstants.BOOTSTRAP_SERVERS)), is(expectedBootstrap));
        assertThat(Base64Utils.decodeFromBase64ToString(data.get(TestConstants.SECURITY_PROTOCOL)), is(expectedSecurityProtocol.name()));

        if (userData != null) {
            assertThat(Base64Utils.decodeFromBase64ToString(data.get(TestConstants.SSL_KEYSTORE_CRT)),
                is(Base64Utils.decodeFromBase64ToString(userData.get(TestConstants.USER_CRT))));
            assertThat(Base64Utils.decodeFromBase64ToString(data.get(TestConstants.SSL_KEYSTORE_KEY)),
                is(Base64Utils.decodeFromBase64ToString(userData.get(TestConstants.USER_KEY))));
        }
    }
}
