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

package software.amazon.awssdk.awscore.metrics;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * The set of metrics collected for all SDK clients.
 */
@SdkPublicApi
public final class AwsCoreMetric {

    /**
     * The unique ID for the service. This is present for all API call metrics.
     */
    public static final SdkMetric<String> SERVICE_ID = metric("ServiceName", String.class);

    /**
     * The name of the service operation being invoked. This is present for all
     * API call metrics.
     */
    public static final SdkMetric<String> OPERATION_NAME = metric("OperationName", String.class);

    private AwsCoreMetric() {
    }

    private static <T> SdkMetric<T> metric(String name, Class<T> clzz) {
        return SdkMetric.create(name, clzz, MetricCategory.DEFAULT);
    }
}
