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

package software.amazon.awssdk.core.metrics;

import java.net.URI;
import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;

@SdkPublicApi
public final class CoreMetric {
    /**
     * The unique ID for the service. This is present for all API call metrics.
     */
    public static final SdkMetric<String> SERVICE_ID =
        metric("ServiceId", String.class, MetricLevel.ERROR);

    /**
     * The name of the service operation being invoked. This is present for all
     * API call metrics.
     */
    public static final SdkMetric<String> OPERATION_NAME =
        metric("OperationName", String.class, MetricLevel.ERROR);

    /**
     * True if the API call succeeded, false otherwise.
     */
    public static final SdkMetric<Boolean> API_CALL_SUCCESSFUL =
        metric("ApiCallSuccessful", Boolean.class, MetricLevel.ERROR);

    /**
     * The number of retries that the SDK performed in the execution of the request. 0 implies that the request worked the first
     * time, and no retries were attempted.
     */
    public static final SdkMetric<Integer> RETRY_COUNT =
        metric("RetryCount", Integer.class, MetricLevel.ERROR);

    /**
     * The endpoint for the service.
     */
    public static final SdkMetric<URI> SERVICE_ENDPOINT =
        metric("ServiceEndpoint", URI.class, MetricLevel.ERROR);

    /**
     * The duration of the API call. This includes all call attempts made.
     *
     * <p>{@code API_CALL_DURATION ~= CREDENTIALS_FETCH_DURATION + MARSHALLING_DURATION + SUM_ALL(BACKOFF_DELAY_DURATION) +
     * SUM_ALL(SIGNING_DURATION) + SUM_ALL(SERVICE_CALL_DURATION) + SUM_ALL(UNMARSHALLING_DURATION)}
     */
    public static final SdkMetric<Duration> API_CALL_DURATION =
        metric("ApiCallDuration", Duration.class, MetricLevel.INFO);

    /**
     * The duration of time taken to fetch signing credentials for the API call.
     */
    public static final SdkMetric<Duration> CREDENTIALS_FETCH_DURATION =
        metric("CredentialsFetchDuration", Duration.class, MetricLevel.INFO);

    /**
     * The duration of time taken to fetch signing credentials for the API call.
     */
    public static final SdkMetric<Duration> TOKEN_FETCH_DURATION =
        metric("TokenFetchDuration", Duration.class, MetricLevel.INFO);

    /**
     * The duration of time that the SDK has waited before this API call attempt, based on the
     * {@link RetryPolicy#backoffStrategy()}.
     */
    public static final SdkMetric<Duration> BACKOFF_DELAY_DURATION =
        metric("BackoffDelayDuration", Duration.class, MetricLevel.INFO);

    /**
     * The duration of time taken to marshall the SDK request to an HTTP request.
     */
    public static final SdkMetric<Duration> MARSHALLING_DURATION =
        metric("MarshallingDuration", Duration.class, MetricLevel.INFO);

    /**
     * The duration of time taken to sign the HTTP request.
     */
    public static final SdkMetric<Duration> SIGNING_DURATION =
        metric("SigningDuration", Duration.class, MetricLevel.INFO);

    /**
     * The duration of time  taken to connect to the service (or acquire a connection from the connection pool), send the
     * serialized request and receive the initial response (e.g. HTTP status code and headers). This DOES NOT include the time
     * taken to read the entire response from the service.
     */
    public static final SdkMetric<Duration> SERVICE_CALL_DURATION =
        metric("ServiceCallDuration", Duration.class, MetricLevel.INFO);

    /**
     * The duration of time taken to unmarshall the HTTP response to an SDK response.
     *
     * <p>Note: For streaming operations, this does not include the time to read the response payload.
     */
    public static final SdkMetric<Duration> UNMARSHALLING_DURATION =
        metric("UnmarshallingDuration", Duration.class, MetricLevel.INFO);

    /**
     * The request ID of the service request.
     */
    public static final SdkMetric<String> AWS_REQUEST_ID =
        metric("AwsRequestId", String.class, MetricLevel.INFO);

    /**
     * The extended request ID of the service request.
     */
    public static final SdkMetric<String> AWS_EXTENDED_REQUEST_ID =
        metric("AwsExtendedRequestId", String.class, MetricLevel.INFO);

    /**
     * The duration of time between from sending the HTTP request (including acquiring a connection) to the service, and
     * receiving the first byte of the headers in the response.
     */
    // Note: SERVICE_CALL_DURATION matches TTFB for sync, but *NOT* async. This appears to be a bug.
    public static final SdkMetric<Duration> TIME_TO_FIRST_BYTE =
        metric("TimeToFirstByte", Duration.class, MetricLevel.TRACE);

    /**
     * The duration of time between from sending the HTTP request (including acquiring a connection) to the service, and
     * receiving the last byte of the response.
     * <p>
     * Note that for APIs that return streaming responses, this metric spans the time until the {@link ResponseTransformer} or
     * {@link AsyncResponseTransformer} completes.
     */
    public static final SdkMetric<Duration> TIME_TO_LAST_BYTE =
        metric("TimeToLastByte", Duration.class, MetricLevel.TRACE);

    /**
     * The read throughput of the client, defined as {@code NumberOfResponseBytesRead / (TTLB - TTFB)}. This value is in bytes per
     * second.
     * <p>
     * Note that this metric only measures the bytes read from within the {@link ResponseTransformer} or
     * {@link AsyncResponseTransformer}. Data that is read outside the transformer (e.g. when the response stream is returned as
     * the result of the transformer) is not included in the calculation.
     */
    public static final SdkMetric<Double> READ_THROUGHPUT =
        metric("ReadThroughput", Double.class, MetricLevel.TRACE);

    /**
     * The duration of time it took to resolve the endpoint used for the API call.
     */
    public static final SdkMetric<Duration> ENDPOINT_RESOLVE_DURATION =
        metric("EndpointResolveDuration", Duration.class, MetricLevel.INFO);


    /**
     * The type of error that occurred for a call attempt.
     * <p>
     * The following are possible values:
     * <ul>
     * <li>Throttling - The service responded with a throttling error.</li>
     * <li>ServerError - The service responded with an error other than throttling.</li>
     * <li>ClientTimeout - A client timeout occurred, either at the API call level, or API call attempt level.</li>
     * <li>IO - An I/O error occurred.</li>
     * <li>Other - Catch-all for other errors that don't fall into the above categories.</li>
     * </ul>
     * <p>
     */
    public static final SdkMetric<String> ERROR_TYPE =
        metric("ErrorType", String.class, MetricLevel.INFO);

    private CoreMetric() {
    }

    private static <T> SdkMetric<T> metric(String name, Class<T> clzz, MetricLevel level) {
        return SdkMetric.create(name, clzz, level, MetricCategory.CORE);
    }
}
