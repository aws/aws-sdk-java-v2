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

package software.amazon.awssdk.http.crt;

import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientLongRunningRequestTestSuite;
import software.amazon.awssdk.utils.AttributeMap;

public class AwsCrtHttpClientLongRunningRequestTest extends SdkHttpClientLongRunningRequestTestSuite {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @Override
    protected SdkHttpClient createSdkHttpClient(AttributeMap config) {
        return AwsCrtHttpClient.builder().buildWithDefaults(config);
    }

    // Empty test; the CRT sync client does not currently enforce READ_TIMEOUT. Delete this
    // override when connection health monitoring is re-added.
    @Override
    public void executeWhenReadTimeoutAndServerDelaysResponseFailsWithinTimeoutBound() {
    }

    // Empty test; the CRT sync client does not currently enforce READ_TIMEOUT. Delete this
    // override when connection health monitoring is re-added.
    @Override
    public void executeWhenReadTimeoutAndStreamingResponsePausesFailsWithinTimeoutBound() {
    }
}
