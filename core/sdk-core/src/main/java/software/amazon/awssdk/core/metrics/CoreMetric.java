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
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.SdkMetric;

@SdkPublicApi
public final class CoreMetric {
    /**
     * The unique ID for the service. This is present for all API call metrics.
     */
    public static final SdkMetric<String> SERVICE_ID = metric("ServiceId", String.class);

    /**
     * The name of the service operation being invoked. This is present for all
     * API call metrics.
     */
    public static final SdkMetric<String> OPERATION_NAME = metric("OperationName", String.class);

    /**
     * The duration of the API call. This includes all call attempts made.
     */
    public static final SdkMetric<Duration> API_CALL_DURATION = metric("ApiCallDuration", Duration.class);

    /**
     * The duration of time taken to marshall the SDK request to an HTTP
     * request.
     */
    public static final SdkMetric<Duration> MARSHALLING_DURATION = metric("MarshallingDuration", Duration.class);

    /**
     * The duration of time taken to fetch signing credentials for the request.
     */
    public static final SdkMetric<Duration> CREDENTIALS_FETCH_DURATION = metric("CredentialsFetchDuration", Duration.class);

    /**
     * The duration fo time taken to sign the HTTP request.
     */
    public static final SdkMetric<Duration> SIGNING_DURATION = metric("SigningDuration", Duration.class);

    /**
     * The total time take to send a HTTP request and receive the response.
     */
    public static final SdkMetric<Duration> HTTP_REQUEST_ROUND_TRIP_TIME = metric("HttpRequestRoundTripTime", Duration.class);

    /**
     * The status code of the HTTP response.
     */
    public static final SdkMetric<Integer> HTTP_STATUS_CODE = metric("HttpStatusCode", Integer.class);

    /**
     * The request ID of the service request.
     */
    public static final SdkMetric<String> AWS_REQUEST_ID = metric("AwsRequestId", String.class);

    /**
     * The extended request ID of the service request.
     */
    public static final SdkMetric<String> AWS_EXTENDED_REQUEST_ID = metric("AwsExtendedRequestId", String.class);

    /**
     * The exception thrown during request execution. Note this may be a service
     * error that has been unmarshalled, or a client side exception.
     */
    public static final SdkMetric<Throwable> EXCEPTION = metric("Exception", Throwable.class);

    private CoreMetric() {
    }

    private static <T> SdkMetric<T> metric(String name, Class<T> clzz) {
        return SdkMetric.create(name, clzz, MetricCategory.DEFAULT);
    }
}
