/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.access.utils;

public class TestUtils {
    private TestUtils() {}

    /**
     * Method for cutting the length of the test case in case that it's too long for having it as label in the particular resource.
     *
     * @param testCaseName  test case name that should be trimmed
     *
     * @return  trimmed test case name if needed
     */
    public static String trimTestCaseBaseOnItsLength(String testCaseName) {
        // because label values `must be no more than 63 characters`
        if (testCaseName.length() > 63) {
            // we cut to 62 characters
            return testCaseName.substring(0, 62);
        }

        return testCaseName;
    }

    public static String removePackageName(String testClassPath) {
        return testClassPath.replace("io.strimzi.kafka.access.", "");
    }
}
