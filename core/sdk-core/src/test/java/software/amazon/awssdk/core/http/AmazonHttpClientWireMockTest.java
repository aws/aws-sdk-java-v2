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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.optionsRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.executionContext;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.response.NullErrorResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import utils.HttpTestUtils;
import utils.http.WireMockTestBase;

public class AmazonHttpClientWireMockTest extends WireMockTestBase {
    private static final String OPERATION = "/some-operation";
    private static final String OPERATION_WITH_ERROR = "/some-operation-with-error";
    private static final String HEADER = "Some-Header";
    private static final String CONFIG_HEADER_VALUE = "client config header value";
    private static final String REQUEST_HEADER_VALUE = "request header value";

    @Before
    public void setUp() {
        stubFor(any(urlPathEqualTo(OPERATION)).willReturn(aResponse()));
        stubFor(any(urlPathEqualTo(OPERATION_WITH_ERROR)).willReturn(aResponse().withStatus(500)));
    }

    @Test
    public void headersSpecifiedInClientConfigurationArePutOnRequest() {
        SdkHttpFullRequest request = newGetRequest(OPERATION).build();

        AmazonSyncHttpClient sut = createClient(HEADER, CONFIG_HEADER_VALUE);
        sendRequest(request, sut, null, new NullErrorResponseHandler());

        verify(getRequestedFor(urlPathEqualTo(OPERATION)).withHeader(HEADER, matching(CONFIG_HEADER_VALUE)));
    }

    @Test
    public void headersOnRequestsWinOverClientConfigurationHeaders() {
        SdkHttpFullRequest request = newGetRequest(OPERATION)
            .putHeader(HEADER, REQUEST_HEADER_VALUE)
            .build();

        AmazonSyncHttpClient sut = createClient(HEADER, CONFIG_HEADER_VALUE);
        sendRequest(request, sut, null, new NullErrorResponseHandler());

        verify(getRequestedFor(urlPathEqualTo(OPERATION)).withHeader(HEADER, matching(REQUEST_HEADER_VALUE)));
    }

    @Test
    public void canHandleOptionsRequest() {
        SdkHttpFullRequest request = newRequest(OPERATION)
            .method(SdkHttpMethod.OPTIONS)
            .build();

        AmazonSyncHttpClient sut = HttpTestUtils.testAmazonHttpClient();
        sendRequest(request, sut, null, new NullErrorResponseHandler());

        verify(optionsRequestedFor(urlPathEqualTo(OPERATION)));
    }

    @Test
    public void handleSuccessResponse_uncheckedIOException_shouldNotBeWrappedWithSdkClientException() {
        SdkHttpFullRequest request = newGetRequest(OPERATION).build();

        AmazonSyncHttpClient sut = createClient(HEADER, CONFIG_HEADER_VALUE);

        assertThatThrownBy(() -> sendRequest(request, sut, uncheckedIOExceptionSuccessResponseHandler(), null))
            .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    public void handleErrorResponse_uncheckedIOException_shouldNotBeWrappedWithSdkClientException() {
        SdkHttpFullRequest request = newGetRequest(OPERATION_WITH_ERROR).build();

        AmazonSyncHttpClient sut = createClient(HEADER, CONFIG_HEADER_VALUE);


        assertThatThrownBy(() -> sendRequest(request, sut, null, uncheckedIOExceptionErrorResponseHandler()))
            .isInstanceOf(UncheckedIOException.class);
    }

    private void sendRequest(SdkHttpFullRequest request,
                             AmazonSyncHttpClient sut,
                             HttpResponseHandler<Response<?>> successResponseHandler,
                             HttpResponseHandler<? extends SdkException> failureResponseHandler) {
        sut.requestExecutionBuilder()
           .request(request)
           .originalRequest(NoopTestRequest.builder().build())
           .executionContext(executionContext(request))
           .execute(combinedSyncResponseHandler(successResponseHandler, failureResponseHandler));
    }

    private AmazonSyncHttpClient createClient(String headerName, String headerValue) {
        return HttpTestUtils.testClientBuilder().additionalHeader(headerName, headerValue).build();
    }

    private static <T> HttpResponseHandler<T> uncheckedIOExceptionSuccessResponseHandler() {
        return (response, executionAttributes) -> {
            throw new UncheckedIOException(new IOException("Should not be wrapped with SdkClientException"));
        };
    }

    private static HttpResponseHandler<? extends SdkException> uncheckedIOExceptionErrorResponseHandler() {
        return (response, executionAttributes) -> {
            throw new UncheckedIOException(new IOException("Should not be wrapped with SdkClientException"));
        };
    }
}
