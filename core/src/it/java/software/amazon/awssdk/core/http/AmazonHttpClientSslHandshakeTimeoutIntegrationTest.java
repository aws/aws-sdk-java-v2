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

package software.amazon.awssdk.core.http;

import static org.junit.Assert.fail;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.executionContext;

import java.time.Duration;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.http.request.EmptyHttpRequest;
import software.amazon.awssdk.core.internal.http.response.NullErrorResponseHandler;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.apache.ApacheSdkHttpClientFactory;
import utils.HttpTestUtils;

/**
 * This test is to verify that the apache-httpclient library has fixed the bug where socket timeout configuration is
 * incorrectly ignored during SSL handshake. This test is expected to hang (and fail after the junit timeout) if run
 * against the problematic httpclient version (e.g. 4.3).
 *
 * @link https://issues.apache.org/jira/browse/HTTPCLIENT-1478
 */
public class AmazonHttpClientSslHandshakeTimeoutIntegrationTest extends UnresponsiveMockServerTestBase {

    private static final Duration CLIENT_SOCKET_TO = Duration.ofSeconds(1);

    @Test(timeout = 60 * 1000)
    public void testSslHandshakeTimeout() {
        AmazonHttpClient httpClient = HttpTestUtils.testClientBuilder()
                                                   .clientExecutionTimeout(null)
                                                   .retryPolicy(RetryPolicy.NONE)
                                                   .httpClient(ApacheSdkHttpClientFactory.builder()
                                                                                         .socketTimeout(CLIENT_SOCKET_TO)
                                                                                         .build()
                                                                                         .createHttpClient())
                                                   .build();

        System.out.println("Sending request to localhost...");

        try {
            EmptyHttpRequest request = new EmptyHttpRequest(server.getHttpsEndpoint(), HttpMethodName.GET);
            httpClient.requestExecutionBuilder()
                      .request(request)
                      .originalRequest(NoopTestAwsRequest.builder().build())
                      .executionContext(executionContext(SdkHttpFullRequestAdapter.toHttpFullRequest(request)))
                      .errorResponseHandler(new NullErrorResponseHandler())
                      .execute();
            fail("Client-side socket read timeout is expected!");

        } catch (SdkClientException e) {
            /**
             * Http client catches the SocketTimeoutException and throws a
             * ConnectTimeoutException.
             * {@link DefaultHttpClientConnectionOperator#connect(ManagedHttpClientConnection, HttpHost,
             * InetSocketAddress, int, SocketConfig, HttpContext)}
             */
            Assert.assertTrue(e.getCause() instanceof ConnectTimeoutException);

            ConnectTimeoutException cte = (ConnectTimeoutException) e.getCause();
            Assert.assertThat(cte.getMessage(), org.hamcrest.Matchers
                    .containsString("Read timed out"));
        }
    }
}
