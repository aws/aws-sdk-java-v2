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

package software.amazon.awssdk.core.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.core.retry.RetryHandler.HEADER_SDK_RETRY_INFO;

import org.junit.Test;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.config.MutableClientConfiguration;
import software.amazon.awssdk.core.config.defaults.GlobalClientConfigurationDefaults;
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

        executeRequest();

        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(HEADER_SDK_RETRY_INFO, containing("0/0/")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(HEADER_SDK_RETRY_INFO, containing("1/0/")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(HEADER_SDK_RETRY_INFO, containing("2/10/")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(HEADER_SDK_RETRY_INFO, containing("3/20/")));
    }

    @Test
    public void retriedRequest_AppendsCorrectRetryCountInUserAgent_throttlingEnabled() throws Exception {
        stubFor(get(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse().withStatus(500)));

        executeRequest();

        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(HEADER_SDK_RETRY_INFO, containing("0/0/500")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(HEADER_SDK_RETRY_INFO, containing("1/0/495")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(HEADER_SDK_RETRY_INFO, containing("2/10/490")));
        verify(1, getRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(HEADER_SDK_RETRY_INFO, containing("3/20/485")));
    }

    private void executeRequest() throws Exception {
        RetryPolicy policy = RetryPolicy.builder().backoffStrategy(new SimpleArrayBackoffStrategy(BACKOFF_VALUES)).build();

        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder().retryPolicy(policy).build();
        MutableClientConfiguration clientConfiguration = new MutableClientConfiguration()
            .overrideConfiguration(overrideConfig)
            .httpClient(HttpTestUtils.testSdkHttpClient());

        new GlobalClientConfigurationDefaults().applySyncDefaults(clientConfiguration);

        AmazonHttpClient httpClient = new AmazonHttpClient(clientConfiguration);
        try {
            SdkHttpFullRequest request = SdkHttpFullRequestAdapter.toHttpFullRequest(newGetRequest(RESOURCE_PATH));
            httpClient.requestExecutionBuilder()
                      .request(request)
                      .executionContext(ClientExecutionAndRequestTimerTestUtils.executionContext(request))
                      .errorResponseHandler(stubErrorHandler())
                      .execute();
            fail("Expected exception");
        } catch (AmazonServiceException expected) {
            // Ignored or expected.
        }
    }
}
