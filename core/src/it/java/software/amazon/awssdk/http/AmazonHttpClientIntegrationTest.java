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

package software.amazon.awssdk.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.optionsRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static utils.HttpTestUtils.builderWithDefaultClient;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Request;
import utils.http.WireMockTestBase;

public class AmazonHttpClientIntegrationTest extends WireMockTestBase {
    private static final String OPERATION = "/some-operation";
    private static final String HEADER = "Some-Header";
    private static final String CONFIG_HEADER_VALUE = "client config header value";
    private static final String REQUEST_HEADER_VALUE = "request header value";

    @Before
    public void setUp() {
        stubFor(any(urlPathEqualTo(OPERATION)).willReturn(aResponse()));
    }

    @Test
    public void headersSpecifiedInClientConfigurationArePutOnRequest() throws Exception {
        Request<?> request = newGetRequest(OPERATION);

        AmazonHttpClient sut = createClient(HEADER, CONFIG_HEADER_VALUE);
        sut.requestExecutionBuilder().request(request).execute();

        verify(getRequestedFor(urlPathEqualTo(OPERATION)).withHeader(HEADER, matching(CONFIG_HEADER_VALUE)));
    }

    @Test
    public void headersOnRequestsWinOverClientConfigurationHeaders() throws Exception {
        Request<?> request = newGetRequest(OPERATION);
        request.getOriginalRequest().putCustomRequestHeader(HEADER, REQUEST_HEADER_VALUE);

        AmazonHttpClient sut = createClient(HEADER, CONFIG_HEADER_VALUE);

        sut.requestExecutionBuilder().request(request).execute();

        verify(getRequestedFor(urlPathEqualTo(OPERATION)).withHeader(HEADER, matching(REQUEST_HEADER_VALUE)));
    }

    @Test
    public void canHandleOptionsRequest() throws Exception {
        Request<?> request = newRequest(OPERATION);
        request.setHttpMethod(HttpMethodName.OPTIONS);

        AmazonHttpClient sut = builderWithDefaultClient()
                .clientConfiguration(new LegacyClientConfiguration())
                .build();
        sut.requestExecutionBuilder().request(request).execute();

        verify(optionsRequestedFor(urlPathEqualTo(OPERATION)));
    }

    private AmazonHttpClient createClient(String headerName, String headerValue) {
        LegacyClientConfiguration clientConfiguration = new LegacyClientConfiguration().withHeader(headerName, headerValue);
        return builderWithDefaultClient()
                .clientConfiguration(clientConfiguration)
                .build();
    }
}
