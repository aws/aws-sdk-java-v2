/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.testutils.service;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import reactor.blockhound.BlockHound;

/**
 * Installs BlockHound when the JUnit Platform starts executing a test plan. Registered through
 * {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}, so every module with service-test-utils on
 * its test class path runs its tests with BlockHound installed, including the {@link BlockHoundAllowlist} integration.
 * <p>
 * This replaces {@code reactor.blockhound.junit.platform.BlockHoundTestExecutionListener}, which installs BlockHound in
 * its static initializer, as soon as the launcher loads it. With {@code forkCount > 1}, surefire creates a launcher in the
 * Maven JVM to discover test classes before it starts the forks. That loaded BlockHound's listener and installed
 * BlockHound into the Maven JVM, which fails on JDK 13+ unless the Maven JVM itself runs with
 * {@code -XX:+AllowRedefinitionToAddDeleteMethods} (https://github.com/reactor/BlockHound/issues/33). Discovery never
 * executes a test plan, so this listener installs BlockHound only in the JVMs that run tests, before any test runs.
 * Those JVMs get the flag from the {@code argLine} in the {@code jdk-13-plus} profile of the root pom.
 */
public class BlockHoundInstallingTestExecutionListener implements TestExecutionListener {
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // BlockHound.install() is a no-op after the first call.
        BlockHound.install();
    }
}
