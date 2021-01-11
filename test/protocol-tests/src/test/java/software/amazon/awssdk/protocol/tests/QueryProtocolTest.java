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

package software.amazon.awssdk.protocol.tests;

import java.io.IOException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.protocol.ProtocolTestSuiteLoader;
import software.amazon.awssdk.protocol.model.TestCase;
import software.amazon.awssdk.protocol.runners.ProtocolTestRunner;

@RunWith(Parameterized.class)
public class QueryProtocolTest extends ProtocolTestBase {

    private static final ProtocolTestSuiteLoader testSuiteLoader = new ProtocolTestSuiteLoader();
    private static ProtocolTestRunner testRunner;

    @Parameterized.Parameter
    public TestCase testCase;

    @Parameterized.Parameters(name = "{0}")
    public static List<TestCase> data() throws IOException {
        return testSuiteLoader.load("query-suite.json");
    }

    @BeforeClass
    public static void setupFixture() {
        testRunner = new ProtocolTestRunner("/models/query-2016-03-11-intermediate.json");
    }

    @Test
    public void runProtocolTest() throws Exception {
        testRunner.runTest(testCase);
    }
}
