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

package software.amazon.awssdk.http.timers.client;

import static software.amazon.awssdk.internal.http.request.RequestHandlerTestUtils.buildRequestHandlerList;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.CLIENT_EXECUTION_TIMEOUT;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.SLOW_REQUEST_HANDLER_TIMEOUT;
import static software.amazon.awssdk.internal.http.timers.TimeoutTestConstants.TEST_TIMEOUT;
import static utils.HttpTestUtils.builderWithDefaultClient;

import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.MockServerTestBase;
import software.amazon.awssdk.http.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.http.server.MockServer;
import software.amazon.awssdk.internal.http.request.SlowRequestHandler;
import software.amazon.awssdk.internal.http.response.DummyResponseHandler;
import software.amazon.awssdk.internal.http.response.UnresponsiveResponseHandler;

public class DummySuccessfulResponseServerIntegrationTests extends MockServerTestBase {

    private static final int STATUS_CODE = 200;

    private AmazonHttpClient httpClient;

    @Override
    protected MockServer buildMockServer() {
        return new MockServer(MockServer.DummyResponseServerBehavior.build(STATUS_CODE, "OK", "Hi"));
    }

    @Test(timeout = TEST_TIMEOUT, expected = ClientExecutionTimeoutException.class)
    public void clientExecutionTimeoutEnabled_SlowResponseHandler_ThrowsClientExecutionTimeoutException()
            throws Exception {
        httpClient = builderWithDefaultClient()
                                     .clientConfiguration(
                                             new LegacyClientConfiguration().withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT))
                                     .build();
        requestBuilder().execute(new UnresponsiveResponseHandler());
    }

    @Test(timeout = TEST_TIMEOUT, expected = ClientExecutionTimeoutException.class)
    public void clientExecutionTimeoutEnabled_SlowAfterResponseRequestHandler_ThrowsClientExecutionTimeoutException()
            throws Exception {
        httpClient = builderWithDefaultClient()
                                     .clientConfiguration(
                                             new LegacyClientConfiguration().withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT))
                                     .build();

        List<RequestHandler> requestHandlers = buildRequestHandlerList(
                new SlowRequestHandler().withAfterResponseWaitInSeconds(SLOW_REQUEST_HANDLER_TIMEOUT));

        requestBuilder().executionContext(withHandlers(requestHandlers)).execute(new DummyResponseHandler());
    }

    @Test(timeout = TEST_TIMEOUT, expected = ClientExecutionTimeoutException.class)
    public void clientExecutionTimeoutEnabled_SlowBeforeRequestRequestHandler_ThrowsClientExecutionTimeoutException()
            throws Exception {
        httpClient = builderWithDefaultClient()
                                     .clientConfiguration(
                                             new LegacyClientConfiguration().withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT))
                                     .build();

        List<RequestHandler> requestHandlers = buildRequestHandlerList(
                new SlowRequestHandler().withBeforeRequestWaitInSeconds(SLOW_REQUEST_HANDLER_TIMEOUT));

        requestBuilder().executionContext(withHandlers(requestHandlers)).execute(new DummyResponseHandler());
    }

    private AmazonHttpClient.RequestExecutionBuilder requestBuilder() {
        return httpClient.requestExecutionBuilder().request(newGetRequest());
    }

    private ExecutionContext withHandlers(List<RequestHandler> requestHandlers) {
        return ExecutionContext.builder().withRequestHandlers(requestHandlers).build();
    }

}
