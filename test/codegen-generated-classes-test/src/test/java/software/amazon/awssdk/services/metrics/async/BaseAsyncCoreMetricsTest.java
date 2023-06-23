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

package software.amazon.awssdk.services.metrics.async;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.metrics.SdkErrorType;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.protocolrestjson.model.EmptyModeledException;

@RunWith(MockitoJUnitRunner.class)
public abstract class BaseAsyncCoreMetricsTest {
    private static final String SERVICE_ID = "AmazonProtocolRestJson";
    private static final String REQUEST_ID = "req-id";
    private static final String EXTENDED_REQUEST_ID = "extended-id";
    static final int MAX_RETRIES = 2;
    static final int MAX_ATTEMPTS = MAX_RETRIES + 1;
    public static final Duration FIXED_DELAY = Duration.ofMillis(500);

    @Test
    public void apiCall_operationSuccessful_addsMetrics() {
        stubSuccessfulResponse();
        callable().get().join();
        addDelayIfNeeded();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(publisher()).publish(collectionCaptor.capture());
        MetricCollection capturedCollection = collectionCaptor.getValue();

        verifySuccessfulApiCallCollection(capturedCollection);

        assertThat(capturedCollection.children()).hasSize(1);
        MetricCollection attemptCollection = capturedCollection.children().get(0);

        assertThat(attemptCollection.name()).isEqualTo("ApiCallAttempt");

        verifySuccessfulApiCallAttemptCollection(attemptCollection);
        assertThat(attemptCollection.metricValues(CoreMetric.SERVICE_CALL_DURATION).get(0))
            .isGreaterThanOrEqualTo(FIXED_DELAY);
    }

    @Test
    public void apiCall_allRetryAttemptsFailedOf500() {
        stubErrorResponse();
        assertThatThrownBy(() -> callable().get().join()).hasCauseInstanceOf(EmptyModeledException.class);
        addDelayIfNeeded();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(publisher()).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();
        verifyFailedApiCallCollection(capturedCollection);
        assertThat(capturedCollection.children()).hasSize(MAX_RETRIES + 1);

        capturedCollection.children().forEach(this::verifyFailedApiCallAttemptCollection);
    }

    @Test
    public void apiCall_allRetryAttemptsFailedOfNetworkError() {
        stubNetworkError();
        assertThatThrownBy(() -> callable().get().join()).hasCauseInstanceOf(SdkClientException.class);
        addDelayIfNeeded();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(publisher()).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();
        verifyFailedApiCallCollection(capturedCollection);
        assertThat(capturedCollection.children()).hasSize(MAX_ATTEMPTS);
        WireMock.verify(MAX_ATTEMPTS, anyRequestedFor(anyUrl()));

        capturedCollection.children().forEach(requestMetrics -> {
            assertThat(requestMetrics.metricValues(HttpMetric.HTTP_STATUS_CODE))
                .isEmpty();
            assertThat(requestMetrics.metricValues(CoreMetric.AWS_REQUEST_ID))
                .isEmpty();
            assertThat(requestMetrics.metricValues(CoreMetric.AWS_EXTENDED_REQUEST_ID))
                .isEmpty();
            assertThat(requestMetrics.metricValues(CoreMetric.SERVICE_CALL_DURATION).get(0))
                .isGreaterThanOrEqualTo(FIXED_DELAY);
            assertThat(requestMetrics.metricValues(CoreMetric.ERROR_TYPE)).containsExactly(SdkErrorType.IO.toString());
        });
    }

    @Test
    public void apiCall_firstAttemptFailedRetrySucceeded() {
        stubSuccessfulRetry();
        callable().get().join();
        addDelayIfNeeded();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(publisher()).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();
        verifyApiCallCollection(capturedCollection);
        assertThat(capturedCollection.metricValues(CoreMetric.RETRY_COUNT)).containsExactly(1);
        assertThat(capturedCollection.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(true);

        assertThat(capturedCollection.children()).hasSize(2);

        MetricCollection failedAttempt = capturedCollection.children().get(0);
        verifyFailedApiCallAttemptCollection(failedAttempt);

        MetricCollection successfulAttempt = capturedCollection.children().get(1);
        verifySuccessfulApiCallAttemptCollection(successfulAttempt);
    }

    /**
     * Adds delay after calling CompletableFuture.join to wait for publisher to get metrics.
     */
    void addDelayIfNeeded() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    abstract String operationName();

    abstract Supplier<CompletableFuture<?>> callable();

    abstract MetricPublisher publisher();

    private void verifyFailedApiCallAttemptCollection(MetricCollection requestMetrics) {
        assertThat(requestMetrics.metricValues(HttpMetric.HTTP_STATUS_CODE))
            .containsExactly(500);
        assertThat(requestMetrics.metricValues(CoreMetric.AWS_REQUEST_ID))
            .containsExactly(REQUEST_ID);
        assertThat(requestMetrics.metricValues(CoreMetric.AWS_EXTENDED_REQUEST_ID))
            .containsExactly(EXTENDED_REQUEST_ID);
        assertThat(requestMetrics.metricValues(CoreMetric.BACKOFF_DELAY_DURATION).size())
            .isGreaterThan(0);
        assertThat(requestMetrics.metricValues(CoreMetric.BACKOFF_DELAY_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(requestMetrics.metricValues(CoreMetric.SERVICE_CALL_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(requestMetrics.metricValues(CoreMetric.ERROR_TYPE)).containsExactly(SdkErrorType.SERVER_ERROR.toString());
    }

    private void verifySuccessfulApiCallAttemptCollection(MetricCollection attemptCollection) {
        assertThat(attemptCollection.metricValues(HttpMetric.HTTP_STATUS_CODE))
            .containsExactly(200);
        assertThat(attemptCollection.metricValues(CoreMetric.AWS_REQUEST_ID))
            .containsExactly(REQUEST_ID);
        assertThat(attemptCollection.metricValues(CoreMetric.AWS_EXTENDED_REQUEST_ID))
            .containsExactly(EXTENDED_REQUEST_ID);
        assertThat(attemptCollection.metricValues(CoreMetric.BACKOFF_DELAY_DURATION).size())
            .isGreaterThanOrEqualTo(1);
        assertThat(attemptCollection.metricValues(CoreMetric.BACKOFF_DELAY_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(attemptCollection.metricValues(CoreMetric.SIGNING_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
    }

    private void verifyFailedApiCallCollection(MetricCollection capturedCollection) {
        verifyApiCallCollection(capturedCollection);
        assertThat(capturedCollection.metricValues(CoreMetric.RETRY_COUNT)).containsExactly(MAX_RETRIES);
        assertThat(capturedCollection.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);
    }

    private void verifySuccessfulApiCallCollection(MetricCollection capturedCollection) {
        verifyApiCallCollection(capturedCollection);
        assertThat(capturedCollection.metricValues(CoreMetric.RETRY_COUNT)).containsExactly(0);
        assertThat(capturedCollection.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(true);
    }

    private void verifyApiCallCollection(MetricCollection capturedCollection) {
        assertThat(capturedCollection.name()).isEqualTo("ApiCall");
        assertThat(capturedCollection.metricValues(CoreMetric.SERVICE_ID))
            .containsExactly(SERVICE_ID);
        assertThat(capturedCollection.metricValues(CoreMetric.OPERATION_NAME))
            .containsExactly(operationName());
        assertThat(capturedCollection.metricValues(CoreMetric.CREDENTIALS_FETCH_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.MARSHALLING_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.API_CALL_DURATION).get(0))
            .isGreaterThan(FIXED_DELAY);
    }

    void stubSuccessfulResponse() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("x-amz-request-id", REQUEST_ID)
                                           .withFixedDelay((int) FIXED_DELAY.toMillis())
                                           .withHeader("x-amz-id-2", EXTENDED_REQUEST_ID)
                                           .withBody("{}")));
    }

    void stubErrorResponse() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500)
                                           .withHeader("x-amz-request-id", REQUEST_ID)
                                           .withHeader("x-amz-id-2", EXTENDED_REQUEST_ID)
                                           .withFixedDelay((int) FIXED_DELAY.toMillis())
                                           .withHeader("X-Amzn-Errortype", "EmptyModeledException")
                                           .withBody("{}")));
    }

    void stubNetworkError() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)
                                           .withFixedDelay((int) FIXED_DELAY.toMillis())
                    ));
    }

    void stubSuccessfulRetry() {
        stubFor(post(anyUrl())
                    .inScenario("retry at 500")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", REQUEST_ID)
                                    .withHeader("x-amz-id-2", EXTENDED_REQUEST_ID)
                                    .withFixedDelay((int) FIXED_DELAY.toMillis())
                                    .withHeader("X-Amzn-Errortype", "EmptyModeledException")
                                    .withStatus(500)));

        stubFor(post(anyUrl())
                    .inScenario("retry at 500")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("x-amz-request-id", REQUEST_ID)
                                    .withHeader("x-amz-id-2", EXTENDED_REQUEST_ID)
                                    .withFixedDelay((int) FIXED_DELAY.toMillis())
                                    .withBody("{}")));
    }
}
