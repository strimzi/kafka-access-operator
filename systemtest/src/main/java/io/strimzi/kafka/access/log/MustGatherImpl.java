/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.log;

import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.LogCollectorBuilder;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.Kubectl;
import io.skodjob.testframe.interfaces.MustGatherSupplier;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.kafka.access.Environment;
import io.strimzi.kafka.access.TestConstants;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.utils.TestUtils;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MustGatherImpl implements MustGatherSupplier {
    private final LogCollector defaultLogCollector = new LogCollectorBuilder()
            .withKubeClient(new KubeClient())
            .withKubeCmdClient(new Kubectl())
            .withRootFolderPath(Environment.TEST_LOG_DIR)
            .withNamespacedResources(List.of(
                TestConstants.SECRET.toLowerCase(Locale.ROOT),
                TestConstants.DEPLOYMENT.toLowerCase(Locale.ROOT),
                Kafka.RESOURCE_SINGULAR,
                KafkaUser.RESOURCE_SINGULAR,
                KafkaAccess.KIND
            ).toArray(new String[0]))
            .withCollectPreviousLogs()
            .build();

    private static final String CURRENT_DATE;

    static {
        // Get current date to create a unique folder
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        dateTimeFormatter = dateTimeFormatter.withZone(ZoneId.of("GMT"));
        CURRENT_DATE = dateTimeFormatter.format(LocalDateTime.now());
    }

    @Override
    public void saveKubernetesState(ExtensionContext extensionContext) {
        String testClass = extensionContext.getRequiredTestClass().getName();
        String testClassShortName = TestUtils.removePackageName(testClass);
        String testCase = extensionContext.getRequiredTestClass() != null ? extensionContext.getRequiredTestClass().getSimpleName() : null;

        LogCollector logCollector = new LogCollectorBuilder(defaultLogCollector)
            .withRootFolderPath(buildFullPathToLogs(testClass, testCase).toString())
            .build();

        // firstly collect resources for class
        logCollector.collectFromNamespacesWithLabels(
            new LabelSelectorBuilder()
                .withMatchLabels(Map.of(TestConstants.TEST_SUITE_NAME_LABEL, testClassShortName))
                .build()
        );

        // then collect from Namespaces in test itself
        if (testCase != null) {
            logCollector.collectFromNamespacesWithLabels(
                new LabelSelectorBuilder()
                    .withMatchLabels(
                        Map.of(
                            TestConstants.TEST_SUITE_NAME_LABEL, testClassShortName,
                            TestConstants.TEST_CASE_NAME_LABEL, testCase
                        )
                    )
                    .build()
            );
        }
    }

    /**
     * Method that checks existence of the folder on specified path.
     * From there, if there are no sub-dirs created - for each of the test-case run/re-run - the method returns the
     * full path containing the specified path and index (1).
     * Otherwise, it lists all the directories, filtering all the folders that are indexes, takes the last one, and returns
     * the full path containing specified path and index increased by one.
     *
     * @param rootPathToLogsForTestCase     complete path for test-class/test-class and test-case logs
     *
     * @return  full path to logs directory built from specified root path and index
     */
    private Path checkPathAndReturnFullRootPathWithIndexFolder(Path rootPathToLogsForTestCase) {
        File logsForTestCase = rootPathToLogsForTestCase.toFile();
        int index = 1;

        if (logsForTestCase.exists()) {
            String[] filesInLogsDir = logsForTestCase.list();

            if (filesInLogsDir != null && filesInLogsDir.length > 0) {
                List<String> indexes = Arrays
                    .stream(filesInLogsDir)
                    .filter(file -> {
                        try {
                            Integer.parseInt(file);
                            return true;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    })
                    .sorted()
                    .toList();

                // check if there is actually something in the list of folders
                if (!indexes.isEmpty()) {
                    // take the highest index and increase it by one for a new directory
                    index = Integer.parseInt(indexes.get(indexes.size() - 1)) + 1;
                }
            }
        }

        return rootPathToLogsForTestCase.resolve(String.valueOf(index));
    }

    /**
     * Method for building the full path to logs for specified test-class and test-case.
     *
     * @param testClass     name of the test-class
     * @param testCase      name of the test-case
     *
     * @return full path to the logs for test-class and test-case, together with index
     */
    private Path buildFullPathToLogs(String testClass, String testCase) {
        Path rootPathToLogsForTestCase = Path.of(Environment.TEST_LOG_DIR, CURRENT_DATE, testClass);

        if (testCase != null) {
            rootPathToLogsForTestCase = rootPathToLogsForTestCase.resolve(testCase);
        }

        return checkPathAndReturnFullRootPathWithIndexFolder(rootPathToLogsForTestCase);
    }
}
