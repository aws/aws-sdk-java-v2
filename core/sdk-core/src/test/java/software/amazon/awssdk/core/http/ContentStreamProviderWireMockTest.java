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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.executionContext;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.response.NullErrorResponseHandler;
import software.amazon.awssdk.core.io.SdkFilterInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import utils.HttpTestUtils;
import utils.http.WireMockTestBase;

/**
 * WireMock tests related to {@link ContentStreamProvider} usage.
 */
public class ContentStreamProviderWireMockTest extends WireMockTestBase {
    private static final String OPERATION = "/some-operation";

    @Test
    public void closesAllCreatedInputStreamsFromProvider() {
        stubFor(any(urlPathEqualTo(OPERATION)).willReturn(aResponse().withStatus(500)));

        TestContentStreamProvider provider = new TestContentStreamProvider();
        SdkHttpFullRequest request = newRequest(OPERATION)
                .contentStreamProvider(provider)
                .method(SdkHttpMethod.PUT)
                .build();

        AmazonSyncHttpClient testClient = HttpTestUtils.testAmazonHttpClient();
        try {
            sendRequest(request, testClient);
            fail("Should have thrown SdkServiceException");
        } catch (SdkServiceException ignored) {
        }

        // The test client uses the default retry policy so there should be 4
        // total attempts and an equal number created streams
        assertThat(provider.getCreatedStreams().size()).isEqualTo(4);
        for (CloseTrackingInputStream is : provider.getCreatedStreams()) {
            assertThat(is.isClosed()).isTrue();
        }
    }

    private void sendRequest(SdkHttpFullRequest request, AmazonSyncHttpClient sut) {
        sut.requestExecutionBuilder()
                .request(request)
                .originalRequest(NoopTestRequest.builder().build())
                .executionContext(executionContext(request))
                .execute(combinedSyncResponseHandler(null, new NullErrorResponseHandler()));
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
