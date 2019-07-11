/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.metrics.meter.Metric;

/**
 * A enum representing the metrics supported by the SDK. All metrics collected by the SDK will be declared in this
 * enum class along with the metric's corresponding {@link MetricCategory}.
 */
// TODO List is not complete
@SdkProtectedApi
public enum SdkMetrics implements Metric {

    /**
     * Service ID of the AWS service that the API request is made against
     */
    Service("Service", MetricCategory.Default),

    /**
     * The name of the AWS operation the request is made to
     */
    Api("Api", MetricCategory.Default),

    /**
     * The total time taken to finish (success or fail) a request (inclusive of all retries)
     */
    ApiCallLatency("ApiCallLatency", MetricCategory.Default),

    /**
     * The time taken to marshall the request
     */
    MarshallingLatency("MarshallingLatency", MetricCategory.Default),

    /**
     * Total number of attempts that were made by the service client to fulfill this request before succeeding or failing.
     * (This value would be 1 if there are no retries)
     */
    ApiCallAttemptCount("ApiCallAttemptCount", MetricCategory.Default),

    /**
     * The time taken to sign the request
     */
    SigningLatency("SigningLatency", MetricCategory.Default),

    /**
     * The time taken by the underlying http client to start the Api call attempt and return the response
     */
    HttpRequestRoundTripLatency("HttpRequestRoundTripLatency", MetricCategory.Default),

    /**
     * The time taken to unmarshall the response (either successful and failed response)
     */
    UnmarshallingLatency("UnmarshallingLatency", MetricCategory.Default),

    /**
     * The total time taken for an Api call attempt
     */
    ApiCallAttemptLatency("ApiCallAttemptLatency", MetricCategory.Default),

    /**
     * The http status code returned in the response
     */
    HttpStatusCode("HttpStatusCode", MetricCategory.Default),

    /**
     * The request Id for the request. Represented by x-amz-request-id header in response
     */
    AwsRequestId("AwsRequestId", MetricCategory.Default),

    /**
     * The extended request Id for the request. Represented by x-amz-id-2 header in response
     */
    ExtendedRequestId("ExtendedRequestId", MetricCategory.Default),

    /**
     * Maximum number of streams allowed on a http2 connection
     */
    MaxStreamCount("MaxStreamCount", MetricCategory.Default, MetricCategory.HttpClient)

    ;

    private final String value;

    private final Set<MetricCategory> categories;

    SdkMetrics(String value, MetricCategory... categories) {
        this.value = value;
        this.categories = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(categories)));
    }

    public String value() {
        return value;
    }

    @Override
    public Set<MetricCategory> categories() {
        return categories;
    }

    @Override
    public String toString() {
        return "{" +
               "value='" + value + '\'' +
               ", categories=" + categories +
               '}';
    }
}
