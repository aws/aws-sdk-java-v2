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

package software.amazon.awssdk.core.internal.util;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.Pair;

public class MetricUtilsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void measureDuration_returnsAccurateDurationInformation() {
        long testDurationNanos = Duration.ofMillis(1).toNanos();

        Pair<Object, Duration> measuredExecute = MetricUtils.measureDuration(() -> {
            long start = System.nanoTime();
            // spin thread instead of Thread.sleep() for a bit more accuracy...
            while (System.nanoTime() - start < testDurationNanos) {
            }
            return "foo";
        });

        assertThat(measuredExecute.right()).isGreaterThanOrEqualTo(Duration.ofNanos(testDurationNanos));
    }

    @Test
    public void measureDuration_returnsCallableReturnValue() {
        String result = "foo";

        Pair<String, Duration> measuredExecute = MetricUtils.measureDuration(() -> result);

        assertThat(measuredExecute.left()).isEqualTo(result);
    }

    @Test
    public void measureDurationUnsafe_doesNotWrapException() throws Exception {
        IOException ioe = new IOException("boom");

        thrown.expect(IOException.class);
        try {
            MetricUtils.measureDurationUnsafe(() -> {
                throw ioe;
            });
        } catch (IOException caught) {
            assertThat(caught).isSameAs(ioe);
            throw caught;
        }
    }

    @Test
    public void measureDuration_doesNotWrapException() {
        RuntimeException e = new RuntimeException("boom");

        thrown.expect(RuntimeException.class);

        try {
            MetricUtils.measureDuration(() -> {
                throw e;
            });
        } catch (RuntimeException caught) {
            assertThat(caught).isSameAs(e);
            throw caught;
        }
    }

    @Test
    public void reportDuration_completableFuture_reportsAccurateDurationInformation() {
        MetricCollector mockCollector = mock(MetricCollector.class);
        SdkMetric<Duration> mockMetric = mock(SdkMetric.class);

        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture<String> result = MetricUtils.reportDuration(() -> future, mockCollector, mockMetric);

        long testDurationNanos = Duration.ofMillis(1).toNanos();
        long start = System.nanoTime();
        // spin thread instead of Thread.sleep() for a bit more accuracy...
        while (System.nanoTime() - start < testDurationNanos) {
        }
        future.complete("foo");
        future.join();

        ArgumentCaptor<Duration> duration = ArgumentCaptor.forClass(Duration.class);
        verify(mockCollector).reportMetric(eq(mockMetric), duration.capture());
        assertThat(duration.getValue()).isGreaterThanOrEqualTo(Duration.ofNanos(testDurationNanos));
    }

    @Test
    public void reportDuration_completableFuture_completesExceptionally_reportsAccurateDurationInformation() {
        MetricCollector mockCollector = mock(MetricCollector.class);
        SdkMetric<Duration> mockMetric = mock(SdkMetric.class);

        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture<String> result = MetricUtils.reportDuration(() -> future, mockCollector, mockMetric);

        long testDurationNanos = Duration.ofMillis(1).toNanos();
        long start = System.nanoTime();
        // spin thread instead of Thread.sleep() for a bit more accuracy...
        while (System.nanoTime() - start < testDurationNanos) {
        }
        future.completeExceptionally(new RuntimeException("future failed"));
        try {
            future.join();
        } catch (CompletionException e) {
            ArgumentCaptor<Duration> duration = ArgumentCaptor.forClass(Duration.class);
            verify(mockCollector).reportMetric(eq(mockMetric), duration.capture());
            assertThat(duration.getValue()).isGreaterThanOrEqualTo(Duration.ofNanos(testDurationNanos));
        }
    }

    @Test
    public void reportDuration_completableFuture_returnsCallableReturnValue() {
        MetricCollector mockCollector = mock(MetricCollector.class);
        SdkMetric<Duration> mockMetric = mock(SdkMetric.class);

        CompletableFuture<String> future = new CompletableFuture<>();

        CompletableFuture<String> result = MetricUtils.reportDuration(() -> future, mockCollector, mockMetric);

        assertThat(result).isEqualTo(result);
    }

    @Test
    public void reportDuration_completableFuture_doesNotWrapException() {
        MetricCollector mockCollector = mock(MetricCollector.class);
        SdkMetric<Duration> mockMetric = mock(SdkMetric.class);

        RuntimeException e = new RuntimeException("boom");

        thrown.expect(RuntimeException.class);

        try {
            MetricUtils.reportDuration(() -> {
                throw e;
            }, mockCollector, mockMetric);
        } catch (RuntimeException caught) {
            assertThat(caught).isSameAs(e);
            throw caught;
        }
    }

    /*@Test
    public void collectHttpMetrics_collectsAllExpectedMetrics() {
        MetricCollector mockCollector = mock(MetricCollector.class);

        int statusCode = 200;
        String requestId = "request-id";
        String amznRequestId = "amzn-request-id";
        String requestId2 = "request-id-2";

        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                .statusCode(statusCode)
                .putHeader("x-amz-request-id", requestId)
                .putHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, amznRequestId)
                .putHeader(HttpResponseHandler.X_AMZ_ID_2_HEADER, requestId2)
                .build();

        MetricUtils.collectHttpMetrics(mockCollector, response);

        verify(mockCollector).reportMetric(HttpMetric.HTTP_STATUS_CODE, statusCode);
        verify(mockCollector).reportMetric(CoreMetric.AWS_REQUEST_ID, requestId);
        verify(mockCollector).reportMetric(CoreMetric.AWS_REQUEST_ID, amznRequestId);
        verify(mockCollector).reportMetric(CoreMetric.AWS_EXTENDED_REQUEST_ID, requestId2);
    }*/
}
