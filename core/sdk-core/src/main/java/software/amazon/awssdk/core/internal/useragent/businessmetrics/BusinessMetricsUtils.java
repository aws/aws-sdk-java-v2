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

package software.amazon.awssdk.core.internal.useragent.businessmetrics;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;

@SdkInternalApi
public final class BusinessMetricsUtils {
    private BusinessMetricsUtils() {
    }

    public static Optional<String> resolveRetryMode(RetryPolicy retryPolicy, RetryStrategy retryStrategy) {
        if (retryPolicy != null) {
            RetryMode retryMode = retryPolicy.retryMode();
            if (retryMode == RetryMode.STANDARD) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_STANDARD.value());
            }
            if (retryMode == RetryMode.LEGACY) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_LEGACY.value());
            }
            if (retryMode == RetryMode.ADAPTIVE || retryMode == RetryMode.ADAPTIVE_V2) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_ADAPTIVE.value());
            }
        } else {
            if (retryStrategy instanceof StandardRetryStrategy) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_STANDARD.value());
            }
            if (retryStrategy instanceof LegacyRetryStrategy) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_LEGACY.value());
            }
            if (retryStrategy instanceof AdaptiveRetryStrategy) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_ADAPTIVE.value());
            }
        }
        return Optional.empty();
    }
}
