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

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicy;
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

    private CoreMetric() {
    }

    private static <T> SdkMetric<T> metric(String name, Class<T> clzz, MetricLevel level) {
        return SdkMetric.create(name, clzz, level, MetricCategory.CORE);
    }
}
