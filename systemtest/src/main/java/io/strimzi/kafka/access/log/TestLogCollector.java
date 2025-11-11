/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.log;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.LogCollectorBuilder;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.Kubectl;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.kafka.access.Environment;
import io.strimzi.kafka.access.TestConstants;
import io.strimzi.kafka.access.model.KafkaAccess;
import io.strimzi.kafka.access.utils.TestUtils;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TestLogCollector {
    private static final String CURRENT_DATE;
    private final LogCollector logCollector;

    static {
        // Get current date to create a unique folder
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        dateTimeFormatter = dateTimeFormatter.withZone(ZoneId.of("GMT"));
        CURRENT_DATE = dateTimeFormatter.format(LocalDateTime.now());
    }

    /**
     * TestLogCollector's constructor
     */
    public TestLogCollector() {
        this.logCollector = defaultLogCollector();
    }

    /**
     * Method for creating default configuration of the {@link LogCollector}.
     * It provides default list of resources into the builder and configures the required {@link KubeClient} and
     * {@link Kubectl}
     *
     * @return  {@link LogCollector} configured with default configuration for the tests
     */
    private LogCollector defaultLogCollector() {
        List<String> resources = List.of(
            TestConstants.SECRET.toLowerCase(Locale.ROOT),
            TestConstants.DEPLOYMENT.toLowerCase(Locale.ROOT),
            Kafka.RESOURCE_SINGULAR,
            KafkaUser.RESOURCE_SINGULAR,
            KafkaAccess.KIND
        );

        return new LogCollectorBuilder()
            .withKubeClient(new KubeClient())
            .withKubeCmdClient(new Kubectl())
            .withRootFolderPath(Environment.TEST_LOG_DIR)
            .withNamespacedResources(resources.toArray(new String[0]))
            .withCollectPreviousLogs()
            .build();
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

    /**
     * Method that encapsulates {@link #collectLogs(String, String)}, where test-class is passed as a parameter and the
     * test-case name is `null` -> that's used when the test fail in `@BeforeAll` or `@AfterAll` phases.
     *
     * @param testClass     name of the test-class, for which the logs should be collected
     */
    public void collectLogs(String testClass) {
        collectLogs(testClass, null);
    }

    /**
     * Method that uses {@link LogCollector#collectFromNamespaces(String...)} method for collecting logs from Namespaces
     * for the particular combination of test-class and test-case.
     *
     * @param testClass     name of the test-class, for which the logs should be collected
     * @param testCase      name of the test-case, for which the logs should be collected
     */
    public void collectLogs(String testClass, String testCase) {
        String testClassShortName = TestUtils.removePackageName(testClass);
        Path rootPathToLogsForTestCase = buildFullPathToLogs(testClass, testCase);

        final LogCollector testCaseCollector = new LogCollectorBuilder(logCollector)
            .withRootFolderPath(rootPathToLogsForTestCase.toString())
            .build();

        // List Namespaces with specified test class name and test case name
        List<String> namespaces = new ArrayList<>(getListOfNamespaces(testClassShortName, testCase));

        namespaces = namespaces.stream().distinct().toList();

        testCaseCollector.collectFromNamespaces(namespaces.toArray(new String[0]));
    }

    /**
     * For {@param testClass} and {@param testCase} returns list of Namespaces based on the LabelSelector.
     * In case that {@param testCase} is `null` it returns just the Namespaces that are labeled with the test class.
     *
     * @param testClass     name of the test class for which we should collect logs
     * @param testCase      name of the test case for which we should collect logs
     *
     * @return  list of Namespaces from which we should collect logs
     */
    private List<String> getListOfNamespaces(String testClass, String testCase) {
        List<String> namespaces = new ArrayList<>(KubeResourceManager.get().kubeClient().getClient()
            .namespaces()
            .withLabelSelector(getTestClassLabelSelector(testClass))
            .list()
            .getItems()
            .stream()
            .map(namespace -> namespace.getMetadata().getName())
            .toList());

        if (testCase != null) {
            namespaces.addAll(
                KubeResourceManager.get().kubeClient().getClient()
                    .namespaces()
                    .withLabelSelector(getTestCaseLabelSelector(testClass, testCase))
                    .list()
                    .getItems()
                    .stream()
                    .map(namespace -> namespace.getMetadata().getName())
                    .toList()
            );
        }

        return namespaces;
    }

    /**
     * Returns LabelSelector for the {@param testClass}.
     *
     * @param testClass     name of the test class for which we should collect logs
     *
     * @return  LabelSelector for the test class
     */
    private LabelSelector getTestClassLabelSelector(String testClass) {
        return new LabelSelectorBuilder()
            .withMatchLabels(Map.of(TestConstants.TEST_SUITE_NAME_LABEL, testClass))
            .build();
    }

    /**
     * Returns LabelSelector for the {@param testClass} and {@param testCase}.
     *
     * @param testClass     name of the test class for which we should collect logs
     * @param testCase      name of the test case for which we should collect logs
     *
     * @return  LabelSelector for the test class and test case
     */
    private LabelSelector getTestCaseLabelSelector(String testClass, String testCase) {
        return new LabelSelectorBuilder()
            .withMatchLabels(
                Map.of(
                    TestConstants.TEST_SUITE_NAME_LABEL, testClass,
                    TestConstants.TEST_CASE_NAME_LABEL, TestUtils.trimTestCaseBaseOnItsLength(testCase)
                )
            )
            .build();
    }
}
