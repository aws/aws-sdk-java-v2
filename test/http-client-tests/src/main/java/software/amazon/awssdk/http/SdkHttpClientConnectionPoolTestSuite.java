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

package software.amazon.awssdk.http;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;
import static utils.HttpTestUtils.executionContext;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.response.EmptySdkResponseHandler;
import software.amazon.awssdk.http.server.MockServer;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import utils.HttpTestUtils;

/**
 * Verifies that a client whose connection pool is limited to a single connection fails to lease a second connection while the
 * first one is still in use.
 *
 * <p>This suite applies to sync clients that expose a bounded connection pool through a {@code maxConnections} setting, which
 * today means {@code apache-client} and {@code apache5-client}. {@code url-connection-client} and {@code aws-crt-client} do not
 * expose that setting, so they do not subclass this suite.</p>
 *
 * <p>The exception raised on pool exhaustion is specific to the underlying HTTP library, so subclasses declare it through
 * {@link #expectedPoolTimeoutCause()}.</p>
 */
public abstract class SdkHttpClientConnectionPoolTestSuite {

    private static MockServer server;

    /**
     * Returns a client whose connection pool holds at most {@code maxConnections} connections, and which waits no longer than
     * {@code connectionTimeout} for a connection.
     */
    protected abstract SdkHttpClient createSdkHttpClient(int maxConnections, Duration connectionTimeout);

    /**
     * Returns the exception the underlying HTTP library raises when the pool cannot supply a connection in time.
     */
    protected abstract Class<? extends Exception> expectedPoolTimeoutCause();

    @BeforeAll
    public static void setup() {
        server = MockServer.createMockServer(MockServer.ServerBehavior.OVERLOADED);
        server.startServer();
    }

    @AfterAll
    public static void tearDown() {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    @Timeout(60)
    public void leasing_a_new_connection_fails_with_connection_pool_timeout() {

        AmazonSyncHttpClient httpClient = HttpTestUtils.testClientBuilder()
                                                       .retryStrategy(DefaultRetryStrategy.doNotRetry())
                                                       .httpClient(createSdkHttpClient(1, Duration.ofMillis(100)))
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
            Assertions.fail("Connection pool timeout exception is expected!");
        } catch (SdkClientException e) {
            assertThat(e.getCause()).isInstanceOf(expectedPoolTimeoutCause());
        }
    }
}
