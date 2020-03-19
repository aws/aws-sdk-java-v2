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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting.SDK_RETRY_INFO_HEADER;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;

import org.junit.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import utils.HttpTestUtils;
import utils.http.WireMockTestBase;
import utils.retry.SimpleArrayBackoffStrategy;

public class RetryCountInUserAgentTest extends WireMockTestBase {

    private static final int[] BACKOFF_VALUES = new int[]{0, 10, 20};

    private static final String RESOURCE_PATH = "/user-agent";

    @Test
    public void retriedRequest_AppendsCorrectRetryCountInUserAgent() throws Exception {
        stubFor(get(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse().withStatus(500)));

        executeRequest(false);

        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(SDK_RETRY_INFO_HEADER, equalTo("0/0/")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(SDK_RETRY_INFO_HEADER, equalTo("1/0/")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(SDK_RETRY_INFO_HEADER, equalTo("2/10/")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(SDK_RETRY_INFO_HEADER, equalTo("3/20/")));
    }

    @Test
    public void retriedRequest_AppendsCorrectRetryCountInUserAgent_throttlingEnabled() throws Exception {
        stubFor(get(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse().withStatus(500)));

        executeRequest(true);

        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(SDK_RETRY_INFO_HEADER, equalTo("0/0/")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(SDK_RETRY_INFO_HEADER, equalTo("1/0/495")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(SDK_RETRY_INFO_HEADER, equalTo("2/10/490")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(SDK_RETRY_INFO_HEADER, equalTo("3/20/485")));
    }

    private void executeRequest(boolean throttlingEnabled) throws Exception {
        RetryPolicy policy = RetryPolicy.builder()
                                        .backoffStrategy(new SimpleArrayBackoffStrategy(BACKOFF_VALUES))
                                        .applyMutation(b -> {
                                            if (!throttlingEnabled) {
                                                b.retryCapacityCondition(null);
                                            }
                                        })
                                        .build();

        SdkClientConfiguration config = HttpTestUtils.testClientConfiguration().toBuilder()
                                                     .option(SdkClientOption.SYNC_HTTP_CLIENT, HttpTestUtils.testSdkHttpClient())
                                                     .option(SdkClientOption.RETRY_POLICY, policy)
                                                     .build();

        AmazonSyncHttpClient httpClient = new AmazonSyncHttpClient(config);
        try {
            SdkHttpFullRequest request = newGetRequest(RESOURCE_PATH).build();
            httpClient.requestExecutionBuilder()
                      .request(request)
                      .originalRequest(NoopTestRequest.builder().build())
                      .executionContext(ClientExecutionAndRequestTimerTestUtils.executionContext(request))
                      .execute(combinedSyncResponseHandler(null, stubErrorHandler()));
            fail("Expected exception");
        } catch (SdkServiceException expected) {
            // Ignored or expected.
        }
    }
}
