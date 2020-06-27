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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Pair;

public class MetricUtilsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testMeasureDuration_returnsAccurateDurationInformation() {
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
    public void testMeasureDuration_returnsCallableReturnValue() {
        String result = "foo";

        Pair<String, Duration> measuredExecute = MetricUtils.measureDuration(() -> result);

        assertThat(measuredExecute.left()).isEqualTo(result);
    }

    @Test
    public void testMeasureDurationUnsafe_doesNotWrapException() throws Exception {
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
    public void testMeasureDuration_doesNotWrapException() {
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
    public void testCollectHttpMetrics_collectsAllExpectedMetrics() {
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
    }
}
