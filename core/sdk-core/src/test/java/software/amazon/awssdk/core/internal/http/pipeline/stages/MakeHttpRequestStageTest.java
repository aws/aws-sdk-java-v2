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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkClientOption.SYNC_HTTP_CLIENT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.core.internal.io.SdkLengthAwareInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.MetricCollector;
import utils.ValidSdkObjects;

public class MakeHttpRequestStageTest {

    private SdkHttpClient mockClient;

    private MakeHttpRequestStage stage;

    @BeforeEach
    public void setup() throws IOException {
        mockClient = mock(SdkHttpClient.class);
        SdkClientConfiguration config = SdkClientConfiguration.builder().option(SYNC_HTTP_CLIENT, mockClient).build();
        stage = new MakeHttpRequestStage(HttpClientDependencies.builder().clientConfiguration(config).build());
    }

    @Test
    public void testExecute_contextContainsMetricCollector_addsChildToExecuteRequest() {
        SdkHttpFullRequest sdkRequest = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.GET)
                .host("mybucket.s3.us-west-2.amazonaws.com")
                .protocol("https")
                .build();

        MetricCollector mockCollector = mock(MetricCollector.class);
        MetricCollector childCollector = mock(MetricCollector.class);

        when(mockCollector.createChild(any(String.class))).thenReturn(childCollector);

        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .executionAttributes(new ExecutionAttributes())
                                                            .build();

        RequestExecutionContext context = RequestExecutionContext.builder()
                .originalRequest(ValidSdkObjects.sdkRequest())
                .executionContext(executionContext)
                .build();

        context.attemptMetricCollector(mockCollector);
        context.apiCallAttemptTimeoutTracker(mock(TimeoutTracker.class));
        context.apiCallTimeoutTracker(mock(TimeoutTracker.class));

        try {
            stage.execute(sdkRequest, context);
        } catch (Exception e) {
            // ignored, don't really care about successful execution of the stage in this case
        } finally {
            ArgumentCaptor<HttpExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);

            verify(mockCollector).createChild(eq("HttpClient"));
            verify(mockClient).prepareRequest(httpRequestCaptor.capture());
            assertThat(httpRequestCaptor.getValue().metricCollector()).contains(childCollector);
        }
    }

    @ParameterizedTest
    @MethodSource("contentLengthVerificationInputs")
    public void execute_testLengthChecking(String description,
                                           ContentStreamProvider provider,
                                           Long contentLength,
                                           boolean expectLengthAware) {
        SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder()
                                                                      .method(SdkHttpMethod.PUT)
                                                          .host("mybucket.s3.us-west-2.amazonaws.com")
                                                          .protocol("https");

        if (provider != null) {
            requestBuilder.contentStreamProvider(provider);
        }

        if (contentLength != null) {
            requestBuilder.putHeader("Content-Length", String.valueOf(contentLength));
        }

        when(mockClient.prepareRequest(any()))
            .thenThrow(new RuntimeException("BOOM"));

        assertThatThrownBy(() -> stage.execute(requestBuilder.build(), createContext())).hasMessage("BOOM");

        ArgumentCaptor<HttpExecuteRequest> requestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);

        verify(mockClient).prepareRequest(requestCaptor.capture());

        HttpExecuteRequest capturedRequest = requestCaptor.getValue();

        if (provider != null) {
            InputStream requestContentStream = capturedRequest.contentStreamProvider().get().newStream();

            if (expectLengthAware) {
                assertThat(requestContentStream).isInstanceOf(SdkLengthAwareInputStream.class);
            } else {
                assertThat(requestContentStream).isNotInstanceOf(SdkLengthAwareInputStream.class);
            }
        } else {
            assertThat(capturedRequest.contentStreamProvider()).isEmpty();
        }
    }

    private static Stream<Arguments> contentLengthVerificationInputs() {
        return Stream.of(
            Arguments.of(
                "Provider present, ContentLength present",
                (ContentStreamProvider) () -> new ByteArrayInputStream(new byte[16]),
                16L,
                true
            ),
            Arguments.of(
                "Provider present, ContentLength not present",
                (ContentStreamProvider) () -> new ByteArrayInputStream(new byte[16]),
                null,
                false
            ),
            Arguments.of(
                "Provider not present, ContentLength present",
                null,
                16L,
                false
            ),
            Arguments.of(
                "Provider not present, ContentLength not present",
                null,
                null,
                false
            )
        );
    }

    private static RequestExecutionContext createContext() {
        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .executionAttributes(new ExecutionAttributes())
                                                            .build();

        RequestExecutionContext context = RequestExecutionContext.builder()
                                                                 .originalRequest(ValidSdkObjects.sdkRequest())
                                                                 .executionContext(executionContext)
                                                                 .build();

        context.apiCallAttemptTimeoutTracker(mock(TimeoutTracker.class));
        context.apiCallTimeoutTracker(mock(TimeoutTracker.class));

        return context;
    }
}
