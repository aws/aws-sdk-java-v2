/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.internal.http.timers.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.internal.http.response.NullResponseHandler;
import software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import utils.HttpTestUtils;

/**
 * These tests don't actually start up a mock server. They use a partially mocked Apache HTTP client
 * to return the desired response
 */
@RunWith(MockitoJUnitRunner.class)
public class MockedClientTests {

    @Mock
    private SdkHttpClient sdkHttpClient;

    @Mock
    private AbortableCallable<SdkHttpFullResponse> sdkResponse;

    @Before
    public void setup() throws Exception {
        when(sdkHttpClient.prepareRequest(any(), any())).thenReturn(sdkResponse);
        when(sdkResponse.call()).thenReturn(SdkHttpFullResponse.builder()
                                                               .statusCode(200)
                                                               .build());
    }

    @Test
    public void clientExecutionTimeoutEnabled_RequestCompletesWithinTimeout_TaskCanceled() throws Exception {
        AmazonHttpClient httpClient = HttpTestUtils.testClientBuilder()
                                                   .httpClient(sdkHttpClient)
                                                   .retryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY)
                                                   .build();

        try {
            ClientExecutionAndRequestTimerTestUtils
                    .execute(httpClient, ClientExecutionAndRequestTimerTestUtils.createMockGetRequest());
            fail("Exception expected");
        } catch (AmazonClientException e) {
            NullResponseHandler.assertIsUnmarshallingException(e);
        }

        ScheduledThreadPoolExecutor requestTimerExecutor = httpClient.getClientExecutionTimer().getExecutor();
        ClientExecutionAndRequestTimerTestUtils.assertTimerNeverTriggered(requestTimerExecutor);
        ClientExecutionAndRequestTimerTestUtils.assertCanceledTasksRemoved(requestTimerExecutor);
        // Core threads should be spun up on demand. Since only one task was submitted only one
        // thread should exist
        assertEquals(1, requestTimerExecutor.getPoolSize());
        ClientExecutionAndRequestTimerTestUtils.assertCoreThreadsShutDownAfterBeingIdle(requestTimerExecutor);
    }

}
