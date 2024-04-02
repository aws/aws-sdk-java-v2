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

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.SdkAsyncHttpClientH1TestSuite;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Testing the scenario where h1 server sends 5xx errors.
 */
public class H1SAsyncServerBehaviorTest extends SdkAsyncHttpClientH1TestSuite {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @AfterAll
    public static void afterAll() {
        CrtResource.waitForNoResources();
    }

    @Override
    protected SdkAsyncHttpClient setupClient() {
        return AwsCrtAsyncHttpClient.builder()
                                    .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }
}
