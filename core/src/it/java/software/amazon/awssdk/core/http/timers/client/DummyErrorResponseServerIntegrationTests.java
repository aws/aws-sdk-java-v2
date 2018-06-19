/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.core.internal.http.timers.TimeoutTestConstants.SLOW_REQUEST_HANDLER_TIMEOUT;
import static software.amazon.awssdk.core.internal.http.timers.TimeoutTestConstants.TEST_TIMEOUT;

import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.TestPreConditions;
import software.amazon.awssdk.core.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.MockServerTestBase;
import software.amazon.awssdk.core.http.server.MockServer;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.request.SlowExecutionInterceptor;
import software.amazon.awssdk.core.internal.http.response.NullErrorResponseHandler;
import software.amazon.awssdk.core.internal.http.response.UnresponsiveErrorResponseHandler;
import software.amazon.awssdk.core.internal.interceptor.ExecutionInterceptorChain;
import utils.HttpTestUtils;

/**
 * Tests that use a server that returns a predetermined error response within the timeout limit
 */
@Ignore
@ReviewBeforeRelease("add it back once execution time out is added back")
public class DummyErrorResponseServerIntegrationTests extends MockServerTestBase {

    private static final int STATUS_CODE = 500;
    private AmazonSyncHttpClient httpClient;

    @BeforeClass
    public static void preConditions() {
        TestPreConditions.assumeNotJava6();
    }

    @Override
    protected MockServer buildMockServer() {
        return new MockServer(
                MockServer.DummyResponseServerBehavior.build(STATUS_CODE, "Internal Server Failure", "Dummy response"));
    }

    @Test(timeout = TEST_TIMEOUT, expected = ClientExecutionTimeoutException.class)
    public void clientExecutionTimeoutEnabled_SlowErrorResponseHandler_ThrowsClientExecutionTimeoutException()
            throws Exception {
        httpClient = HttpTestUtils.testAmazonHttpClient();

        httpClient.requestExecutionBuilder().request(newGetRequest()).errorResponseHandler(new UnresponsiveErrorResponseHandler())
                .execute();
    }

    @Test(timeout = TEST_TIMEOUT, expected = ClientExecutionTimeoutException.class)
    public void clientExecutionTimeoutEnabled_SlowAfterErrorRequestHandler_ThrowsClientExecutionTimeoutException()
            throws Exception {
        httpClient = HttpTestUtils.testAmazonHttpClient();

        ExecutionInterceptorChain interceptors =
                new ExecutionInterceptorChain(Collections.singletonList(
                        new SlowExecutionInterceptor().onExecutionFailureWaitInSeconds(SLOW_REQUEST_HANDLER_TIMEOUT)));

        httpClient.requestExecutionBuilder()
                .request(newGetRequest())
                .errorResponseHandler(new NullErrorResponseHandler())
                .executionContext(ExecutionContext.builder().interceptorChain(interceptors).build())
                .execute();
    }

}
