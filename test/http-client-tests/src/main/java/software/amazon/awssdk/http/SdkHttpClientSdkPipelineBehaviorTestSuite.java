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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.optionsRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static utils.HttpTestUtils.executionContext;
import static utils.HttpTestUtils.testClientBuilder;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyTransactionIdStage;
import software.amazon.awssdk.core.internal.http.response.NullErrorResponseHandler;
import software.amazon.awssdk.core.io.SdkFilterInputStream;
import utils.http.WireMockTestBase;

/**
 * Reusable suite that exercises sdk-core's {@link AmazonSyncHttpClient} execution pipeline (client-configuration header
 * injection, request-header precedence, OPTIONS pass-through, transaction-id consistency across retries, and closing of
 * every {@link ContentStreamProvider}-created stream) over a real {@link SdkHttpClient}. Each concrete HTTP client module
 * subclasses this and provides its client via {@link #createSdkHttpClient()}.
 */
public abstract class SdkHttpClientSdkPipelineBehaviorTestSuite extends WireMockTestBase {

    private static final String OPERATION = "/some-operation";
    private static final String HEADER = "Some-Header";
    private static final String CONFIG_HEADER_VALUE = "client config header value";
    private static final String REQUEST_HEADER_VALUE = "request header value";
    private static final String RESOURCE_PATH = "/transaction-id/";

    protected abstract SdkHttpClient createSdkHttpClient();

    @Test
    public void headersSpecifiedInClientConfigurationArePutOnRequest() {
        stubFor(any(urlPathEqualTo(OPERATION)).willReturn(aResponse()));
        SdkHttpFullRequest request = newGetRequest(OPERATION).build();

        AmazonSyncHttpClient sut = createClient(HEADER, CONFIG_HEADER_VALUE);
        sendRequest(request, sut);

        verify(getRequestedFor(urlPathEqualTo(OPERATION)).withHeader(HEADER, matching(CONFIG_HEADER_VALUE)));
    }

    @Test
    public void headersOnRequestsWinOverClientConfigurationHeaders() {
        stubFor(any(urlPathEqualTo(OPERATION)).willReturn(aResponse()));
        SdkHttpFullRequest request = newGetRequest(OPERATION)
            .putHeader(HEADER, REQUEST_HEADER_VALUE)
            .build();

        AmazonSyncHttpClient sut = createClient(HEADER, CONFIG_HEADER_VALUE);
        sendRequest(request, sut);

        verify(getRequestedFor(urlPathEqualTo(OPERATION)).withHeader(HEADER, matching(REQUEST_HEADER_VALUE)));
    }

    @Test
    public void canHandleOptionsRequest() {
        stubFor(any(urlPathEqualTo(OPERATION)).willReturn(aResponse()));
        SdkHttpFullRequest request = newRequest(OPERATION)
            .method(SdkHttpMethod.OPTIONS)
            .build();

        AmazonSyncHttpClient sut = amazonSyncHttpClient();
        sendRequest(request, sut);

        verify(optionsRequestedFor(urlPathEqualTo(OPERATION)));
    }

    @Test
    public void retriedRequest_HasSameTransactionIdForAllRetries() throws Exception {
        stubFor(get(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse().withStatus(500)));
        executeRequest();
        assertTransactionIdIsUnchangedAcrossRetries();
    }

    @Test
    public void closesAllCreatedInputStreamsFromProvider() {
        stubFor(any(urlPathEqualTo(OPERATION)).willReturn(aResponse().withStatus(500)));

        TestContentStreamProvider provider = new TestContentStreamProvider();
        SdkHttpFullRequest request = newRequest(OPERATION)
                .contentStreamProvider(provider)
                .method(SdkHttpMethod.PUT)
                .build();

        AmazonSyncHttpClient testClient = amazonSyncHttpClient();
        try {
            sendRequest(request, testClient);
            fail("Should have thrown SdkServiceException");
        } catch (SdkServiceException ignored) {
            // Ignored or expected.
        }

        // The test client uses the default retry policy so there should be 4
        // total attempts and an equal number created streams
        assertThat(provider.getCreatedStreams().size()).isEqualTo(4);
        for (CloseTrackingInputStream is : provider.getCreatedStreams()) {
            assertThat(is.isClosed()).isTrue();
        }
    }

    private void executeRequest() throws Exception {
        AmazonSyncHttpClient httpClient = amazonSyncHttpClient();
        try {
            SdkHttpFullRequest request = newGetRequest(RESOURCE_PATH).build();
            httpClient.requestExecutionBuilder()
                      .request(request)
                      .originalRequest(NoopTestRequest.builder().build())
                      .executionContext(executionContext(request))
                      .execute(combinedSyncResponseHandler(null, stubErrorHandler()));
            fail("Expected exception");
        } catch (SdkServiceException expected) {
            // Ignored or expected.
        }
    }

    private void assertTransactionIdIsUnchangedAcrossRetries() {
        String previousTransactionId = null;
        for (LoggedRequest request : findAll(getRequestedFor(urlEqualTo(RESOURCE_PATH)))) {
            String currentTransactionId = request.getHeader(ApplyTransactionIdStage.HEADER_SDK_TRANSACTION_ID);
            // Transaction ID should always be set
            assertNotNull(currentTransactionId);
            // Transaction ID should be the same across retries
            if (previousTransactionId != null) {
                assertEquals(previousTransactionId, currentTransactionId);
            }
            previousTransactionId = currentTransactionId;
        }
    }

    private void sendRequest(SdkHttpFullRequest request, AmazonSyncHttpClient sut) {
        sut.requestExecutionBuilder()
            .request(request)
            .originalRequest(NoopTestRequest.builder().build())
            .executionContext(executionContext(request))
            .execute(combinedSyncResponseHandler(null, new NullErrorResponseHandler()));
    }

    private AmazonSyncHttpClient createClient(String headerName, String headerValue) {
        return testClientBuilder().httpClient(createSdkHttpClient()).additionalHeader(headerName, headerValue).build();
    }

    private AmazonSyncHttpClient amazonSyncHttpClient() {
        return testClientBuilder().httpClient(createSdkHttpClient()).build();
    }

    private static class TestContentStreamProvider implements ContentStreamProvider {
        private static final byte[] CONTENT_BYTES = "Hello".getBytes(StandardCharsets.UTF_8);
        private List<CloseTrackingInputStream> createdStreams = new ArrayList<>();

        @Override
        public InputStream newStream() {
            closeCurrentStream();
            CloseTrackingInputStream s = newContentStream();
            createdStreams.add(s);
            return s;
        }

        List<CloseTrackingInputStream> getCreatedStreams() {
            return createdStreams;
        }

        private CloseTrackingInputStream newContentStream() {
            return new CloseTrackingInputStream(new ByteArrayInputStream(CONTENT_BYTES));
        }

        private void closeCurrentStream() {
            if (createdStreams.isEmpty()) {
                return;
            }
            invokeSafely(() -> createdStreams.get(createdStreams.size() - 1).close());
        }
    }

    private static class CloseTrackingInputStream extends SdkFilterInputStream {
        private boolean isClosed = false;

        CloseTrackingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            super.close();
            isClosed = true;
        }

        boolean isClosed() {
            return isClosed;
        }
    }
}
