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

package software.amazon.awssdk.modulepath.tests;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.modulepath.tests.mocktests.BaseMockApiCall;
import software.amazon.awssdk.modulepath.tests.mocktests.JsonProtocolApiCall;
import software.amazon.awssdk.modulepath.tests.mocktests.XmlProtocolApiCall;

/**
 * Executor to run mock tests on module path.
 */
public class MockTestsRunner {

    private MockTestsRunner() {
    }

    public static void main(String... args) {
        List<BaseMockApiCall> tests = new ArrayList<>();
        tests.add(new XmlProtocolApiCall());
        tests.add(new JsonProtocolApiCall());

        tests.forEach(t -> {
            t.successfulApiCall();
            t.failedApiCall();

            t.successfulAsyncApiCall();
            t.failedAsyncApiCall();
        });
    }
}
