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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpManagerMetrics;
import software.amazon.awssdk.crt.s3.S3RequestMetrics;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;

/**
 * Unit tests for {@link S3CrtResponseHandlerAdapter#onTelemetry(S3RequestMetrics)} - the mapping of CRT native request
 * telemetry onto the {@code ApiCall -> ApiCallAttempt -> HttpClient} {@link MetricCollection} that is published to the
 * configured publishers. CRT invokes {@code onTelemetry} once per underlying request attempt, so each call is published
 * as its own top-level {@code ApiCall} collection.
 */
public class S3CrtResponseHandlerAdapterMetricsTest {

    private static final int AWS_ERROR_S3_METRIC_DATA_NOT_AVAILABLE = 14358;
    private static final long API_CALL_NANOS = 98_000_000L;
    private static final long SIGNING_NANOS = 1_000_000L;
    private static final long SERVICE_CALL_NANOS = 35_000_000L;
    private static final long TIME_TO_FIRST_BYTE_NANOS = 40_000_000L;
    private static final long TIME_TO_LAST_BYTE_NANOS = 90_000_000L;
    private static final long CONNECTION_ACQUISITION_NANOS = 2_000_000L;
    private static final String SERVICE_ENDPOINT = "https://s3.us-west-2.amazonaws.com";
    private static final int MAX_CONCURRENCY = 10;

    @Test
    public void onTelemetry_publishesApiCallTreeWithMappedValues() throws Exception {
        CapturingPublisher publisher = new CapturingPublisher();
        adapterPublishingTo(publisher).onTelemetry(successfulGetObjectMetrics());

        assertThat(publisher.collections).hasSize(1);
        MetricCollection apiCall = publisher.collections.get(0);
        assertThat(apiCall.name()).isEqualTo("ApiCall");
        assertThat(apiCall.metricValues(CoreMetric.SERVICE_ID)).containsExactly("S3");
        assertThat(apiCall.metricValues(CoreMetric.OPERATION_NAME)).containsExactly("GetObject");
        assertThat(apiCall.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(true);
        assertThat(apiCall.metricValues(CoreMetric.RETRY_COUNT)).containsExactly(0);
        assertThat(apiCall.metricValues(CoreMetric.API_CALL_DURATION)).containsExactly(Duration.ofNanos(API_CALL_NANOS));
        assertThat(apiCall.metricValues(CoreMetric.SERVICE_ENDPOINT)).containsExactly(URI.create(SERVICE_ENDPOINT));

        MetricCollection attempt = childNamed(apiCall, "ApiCallAttempt");
        assertThat(attempt.metricValues(CoreMetric.SIGNING_DURATION)).containsExactly(Duration.ofNanos(SIGNING_NANOS));
        assertThat(attempt.metricValues(CoreMetric.SERVICE_CALL_DURATION)).containsExactly(Duration.ofNanos(SERVICE_CALL_NANOS));
        assertThat(attempt.metricValues(CoreMetric.BACKOFF_DELAY_DURATION)).containsExactly(Duration.ofNanos(0));
        assertThat(attempt.metricValues(CoreMetric.AWS_REQUEST_ID)).containsExactly("REQ-123");
        assertThat(attempt.metricValues(CoreMetric.AWS_EXTENDED_REQUEST_ID)).containsExactly("EXT-456");
        assertThat(attempt.metricValues(CoreMetric.TIME_TO_FIRST_BYTE))
            .containsExactly(Duration.ofNanos(TIME_TO_FIRST_BYTE_NANOS));
        assertThat(attempt.metricValues(CoreMetric.TIME_TO_LAST_BYTE))
            .containsExactly(Duration.ofNanos(TIME_TO_LAST_BYTE_NANOS));
        assertThat(attempt.metricValues(HttpMetric.HTTP_STATUS_CODE)).containsExactly(200);

        MetricCollection httpClient = childNamed(attempt, "HttpClient");
        assertThat(httpClient.metricValues(HttpMetric.HTTP_CLIENT_NAME)).containsExactly("s3crt");
        assertThat(httpClient.metricValues(HttpMetric.MAX_CONCURRENCY)).containsExactly(MAX_CONCURRENCY);
        assertThat(httpClient.metricValues(HttpMetric.CONCURRENCY_ACQUIRE_DURATION))
            .containsExactly(Duration.ofNanos(CONNECTION_ACQUISITION_NANOS));
        assertThat(httpClient.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(4);
        assertThat(httpClient.metricValues(HttpMetric.LEASED_CONCURRENCY)).containsExactly(2);
        assertThat(httpClient.metricValues(HttpMetric.PENDING_CONCURRENCY_ACQUIRES)).containsExactly(1);
    }

    @Test
    public void onTelemetry_unavailableMetric_isSkipped_othersStillPublished() throws Exception {
        S3RequestMetrics metrics = successfulGetObjectMetrics();
        // CRT throws this when a datum is not available for the request; only that one metric should be skipped.
        when(metrics.getBackoffDelayDurationNs()).thenThrow(new CrtRuntimeException(AWS_ERROR_S3_METRIC_DATA_NOT_AVAILABLE));

        CapturingPublisher publisher = new CapturingPublisher();
        adapterPublishingTo(publisher).onTelemetry(metrics);

        assertThat(publisher.collections).hasSize(1);
        MetricCollection attempt = childNamed(publisher.collections.get(0), "ApiCallAttempt");
        assertThat(attempt.metricValues(CoreMetric.BACKOFF_DELAY_DURATION)).isEmpty();
        assertThat(attempt.metricValues(CoreMetric.SIGNING_DURATION)).isNotEmpty();
        assertThat(attempt.metricValues(CoreMetric.AWS_REQUEST_ID)).isNotEmpty();
    }

    @Test
    public void onTelemetry_optionalHttpMetricsUnavailable_areSkipped() throws Exception {
        S3RequestMetrics metrics = successfulGetObjectMetrics();
        when(metrics.getTimeToFirstByte()).thenThrow(new CrtRuntimeException(AWS_ERROR_S3_METRIC_DATA_NOT_AVAILABLE));
        when(metrics.getConnectionAcquisitionDurationNs())
            .thenThrow(new CrtRuntimeException(AWS_ERROR_S3_METRIC_DATA_NOT_AVAILABLE));
        when(metrics.getResponseStatus()).thenReturn(-1);
        when(metrics.getHttpManagerMetrics()).thenReturn(null);

        CapturingPublisher publisher = new CapturingPublisher();
        adapterPublishingTo(publisher).onTelemetry(metrics);

        assertThat(publisher.collections).hasSize(1);
        MetricCollection attempt = childNamed(publisher.collections.get(0), "ApiCallAttempt");
        assertThat(attempt.metricValues(CoreMetric.TIME_TO_FIRST_BYTE)).isEmpty();
        assertThat(attempt.metricValues(CoreMetric.TIME_TO_LAST_BYTE)).isNotEmpty();
        assertThat(attempt.metricValues(HttpMetric.HTTP_STATUS_CODE)).isEmpty();

        MetricCollection httpClient = childNamed(attempt, "HttpClient");
        assertThat(httpClient.metricValues(HttpMetric.HTTP_CLIENT_NAME)).containsExactly("s3crt");
        assertThat(httpClient.metricValues(HttpMetric.CONCURRENCY_ACQUIRE_DURATION)).isEmpty();
        assertThat(httpClient.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).isEmpty();
        assertThat(httpClient.metricValues(HttpMetric.LEASED_CONCURRENCY)).isEmpty();
        assertThat(httpClient.metricValues(HttpMetric.PENDING_CONCURRENCY_ACQUIRES)).isEmpty();
    }

    @Test
    public void onTelemetry_failedAttempt_reportsApiCallUnsuccessful() throws Exception {
        S3RequestMetrics metrics = successfulGetObjectMetrics();
        when(metrics.isApiCallSuccessful()).thenReturn(false);

        CapturingPublisher publisher = new CapturingPublisher();
        adapterPublishingTo(publisher).onTelemetry(metrics);

        MetricCollection apiCall = publisher.collections.get(0);
        assertThat(apiCall.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);
        assertThat(apiCall.metricValues(CoreMetric.API_CALL_DURATION)).isNotEmpty();
    }

    @Test
    public void onTelemetry_noPublishers_isNoOp_andReadsNoMetrics() {
        S3RequestMetrics metrics = mock(S3RequestMetrics.class);
        adapterPublishingTo().onTelemetry(metrics);
        // The empty-publisher early-return happens before any collection is built or any native getter is read.
        verifyNoInteractions(metrics);
    }

    @Test
    public void onTelemetry_calledPerAttempt_publishesOneCollectionEach() throws Exception {
        CapturingPublisher publisher = new CapturingPublisher();
        S3CrtResponseHandlerAdapter adapter = adapterPublishingTo(publisher);
        S3RequestMetrics metrics = successfulGetObjectMetrics();

        adapter.onTelemetry(metrics);
        adapter.onTelemetry(metrics);
        adapter.onTelemetry(metrics);

        assertThat(publisher.collections).hasSize(3);
    }

    @Test
    public void onTelemetry_retriedRequest_publishesSeparateCollectionsWithIncreasingRetryCount() throws Exception {
        CapturingPublisher publisher = new CapturingPublisher();
        S3CrtResponseHandlerAdapter adapter = adapterPublishingTo(publisher);

        // First attempt fails (retry count 0), the retry succeeds (retry count 1). CRT delivers these as two separate
        // onTelemetry callbacks, so we publish two top-level ApiCall collections - not one ApiCall with two attempts.
        S3RequestMetrics firstAttempt = successfulGetObjectMetrics();
        when(firstAttempt.isApiCallSuccessful()).thenReturn(false);
        when(firstAttempt.getRetryCount()).thenReturn(0);
        adapter.onTelemetry(firstAttempt);

        S3RequestMetrics retryAttempt = successfulGetObjectMetrics();
        when(retryAttempt.getRetryCount()).thenReturn(1);
        adapter.onTelemetry(retryAttempt);

        assertThat(publisher.collections).hasSize(2);
        assertThat(publisher.collections.get(0).metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);
        assertThat(publisher.collections.get(0).metricValues(CoreMetric.RETRY_COUNT)).containsExactly(0);
        assertThat(publisher.collections.get(1).metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(true);
        assertThat(publisher.collections.get(1).metricValues(CoreMetric.RETRY_COUNT)).containsExactly(1);
    }

    private static S3RequestMetrics successfulGetObjectMetrics() throws Exception {
        S3RequestMetrics metrics = mock(S3RequestMetrics.class);
        when(metrics.getServiceId()).thenReturn("S3");
        when(metrics.getServiceEndpoint()).thenReturn(SERVICE_ENDPOINT);
        when(metrics.getOperationName()).thenReturn("GetObject");
        when(metrics.isApiCallSuccessful()).thenReturn(true);
        when(metrics.getRetryCount()).thenReturn(0);
        when(metrics.getApiCallDurationNs()).thenReturn(API_CALL_NANOS);
        when(metrics.getSigningDurationNs()).thenReturn(SIGNING_NANOS);
        when(metrics.getServiceCallDurationNs()).thenReturn(SERVICE_CALL_NANOS);
        when(metrics.getBackoffDelayDurationNs()).thenReturn(0L);
        when(metrics.getAwsRequestId()).thenReturn("REQ-123");
        when(metrics.getAwsExtendedRequestId()).thenReturn("EXT-456");
        when(metrics.getTimeToFirstByte()).thenReturn(TIME_TO_FIRST_BYTE_NANOS);
        when(metrics.getTimeToLastByte()).thenReturn(TIME_TO_LAST_BYTE_NANOS);
        when(metrics.getResponseStatus()).thenReturn(200);
        when(metrics.getConnectionAcquisitionDurationNs()).thenReturn(CONNECTION_ACQUISITION_NANOS);

        HttpManagerMetrics managerMetrics = mock(HttpManagerMetrics.class);
        when(managerMetrics.getAvailableConcurrency()).thenReturn(4L);
        when(managerMetrics.getLeasedConcurrency()).thenReturn(2L);
        when(managerMetrics.getPendingConcurrencyAcquires()).thenReturn(1L);
        when(metrics.getHttpManagerMetrics()).thenReturn(managerMetrics);
        return metrics;
    }

    private static S3CrtResponseHandlerAdapter adapterPublishingTo(MetricPublisher... publishers) {
        SdkHttpExecutionAttributes attributes =
            SdkHttpExecutionAttributes.builder()
                                      .put(S3InternalSdkHttpExecutionAttribute.METRIC_PUBLISHERS, Arrays.asList(publishers))
                                      .put(S3InternalSdkHttpExecutionAttribute.CRT_MAX_CONCURRENCY, MAX_CONCURRENCY)
                                      .build();
        return new S3CrtResponseHandlerAdapter(new CompletableFuture<>(),
                                               mock(SdkAsyncHttpResponseHandler.class),
                                               attributes,
                                               new CompletableFuture<>());
    }

    private static MetricCollection childNamed(MetricCollection parent, String name) {
        return parent.childrenWithName(name).findFirst().orElseThrow(() -> new AssertionError("missing child: " + name));
    }

    private static final class CapturingPublisher implements MetricPublisher {
        private final List<MetricCollection> collections = new ArrayList<>();

        @Override
        public void publish(MetricCollection metricCollection) {
            collections.add(metricCollection);
        }

        @Override
        public void close() {
        }
    }
}
