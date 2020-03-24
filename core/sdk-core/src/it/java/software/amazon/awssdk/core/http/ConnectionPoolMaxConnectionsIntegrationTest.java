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

package software.amazon.awssdk.core.http;

import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.executionContext;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;

import java.time.Duration;

import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.server.MockServer;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.response.EmptySdkResponseHandler;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import utils.HttpTestUtils;

public class ConnectionPoolMaxConnectionsIntegrationTest {

    private static MockServer server;

    @BeforeClass
    public static void setup() {
        server = MockServer.createMockServer(MockServer.ServerBehavior.OVERLOADED);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test(timeout = 60 * 1000)
    public void leasing_a_new_connection_fails_with_connection_pool_timeout() {

        AmazonSyncHttpClient httpClient = HttpTestUtils.testClientBuilder()
                                                       .retryPolicy(RetryPolicy.none())
                                                       .httpClient(ApacheHttpClient.builder()
                                                                                   .connectionTimeout(Duration.ofMillis(100))
                                                                                   .maxConnections(1)
                                                                                   .build())
                                                       .build();

        SdkHttpFullRequest request = server.configureHttpEndpoint(SdkHttpFullRequest.builder())
                                           .method(SdkHttpMethod.GET)
                                           .build();

        // Block the first connection in the pool with this request.
        httpClient.requestExecutionBuilder()
                  .request(request)
                  .originalRequest(NoopTestRequest.builder().build())
                  .executionContext(executionContext(request))
                  .execute(combinedSyncResponseHandler(new EmptySdkResponseHandler(), null));

        try {
            // A new connection will be leased here which would fail in
            // ConnectionPoolTimeoutException.
            httpClient.requestExecutionBuilder()
                      .request(request)
                      .originalRequest(NoopTestRequest.builder().build())
                      .executionContext(executionContext(request))
                      .execute(combinedSyncResponseHandler(null, null));
            Assert.fail("Connection pool timeout exception is expected!");
        } catch (SdkClientException e) {
            Assert.assertTrue(e.getCause() instanceof ConnectionPoolTimeoutException);
        }
    }
}
