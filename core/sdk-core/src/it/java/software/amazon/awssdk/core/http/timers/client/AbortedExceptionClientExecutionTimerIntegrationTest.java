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

package software.amazon.awssdk.core.http.timers.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.createMockGetRequest;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.execute;

import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import utils.HttpTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AbortedExceptionClientExecutionTimerIntegrationTest  {

    private AmazonSyncHttpClient httpClient;

    @Mock
    private SdkHttpClient sdkHttpClient;

    @Mock
    private ExecutableHttpRequest abortableCallable;

    @Before
    public void setup() throws Exception {
        when(sdkHttpClient.prepareRequest(any())).thenReturn(abortableCallable);
        httpClient = HttpTestUtils.testClientBuilder().httpClient(sdkHttpClient)
                                  .apiCallTimeout(Duration.ofMillis(1000))
                                  .build();
        when(abortableCallable.call()).thenReturn(HttpExecuteResponse.builder().response(SdkHttpResponse.builder()
                                                                                                        .statusCode(200)
                                                                                                        .build())
                                                                     .build());
    }

    @Test(expected = AbortedException.class)
    public void clientExecutionTimeoutEnabled_aborted_exception_occurs_timeout_not_expired() throws Exception {
        when(abortableCallable.call()).thenThrow(AbortedException.builder().build());

        execute(httpClient, createMockGetRequest().build());
    }

    @Test(expected = ApiCallTimeoutException.class)
    public void clientExecutionTimeoutEnabled_aborted_exception_occurs_timeout_expired() throws Exception {
        // Simulate a slow HTTP request
        when(abortableCallable.call()).thenAnswer(i -> {
            Thread.sleep(10_000);
            return null;
        });

        execute(httpClient, createMockGetRequest().build());
    }
}
