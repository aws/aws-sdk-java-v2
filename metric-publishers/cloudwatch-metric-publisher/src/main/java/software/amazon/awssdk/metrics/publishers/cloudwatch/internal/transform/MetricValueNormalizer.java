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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal.transform;

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
class MetricValueNormalizer {
    /**
     * Really small values (close to 0) result in CloudWatch failing with an "unsupported value" error. Make sure that we floor
     * those values to 0 to prevent that error.
     */
    private static final double ZERO_THRESHOLD = 0.0001;

    private MetricValueNormalizer() {
    }

    /**
     * Normalizes a metric value so that it won't upset CloudWatch when it is uploaded.
     */
    public static double normalize(double value) {
        if (value > ZERO_THRESHOLD) {
            return value;
        }

        if (value < -ZERO_THRESHOLD) {
            return value;
        }

        return 0;
    }
}