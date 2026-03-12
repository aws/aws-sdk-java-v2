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

package software.amazon.awssdk.awscore.internal.useragent;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;

/**
 * Implementation notes: this class should've been outside internal package,
 * but we can't fix it due to backwards compatibility reasons.
 */
@SdkProtectedApi
public final class BusinessMetricsUtils {
    private BusinessMetricsUtils() {
    }

    public static Optional<String> resolveAccountIdEndpointModeMetric(AccountIdEndpointMode accountIdEndpointMode) {
        if (accountIdEndpointMode == AccountIdEndpointMode.PREFERRED) {
            return Optional.of(BusinessMetricFeatureId.ACCOUNT_ID_MODE_PREFERRED.value());
        }
        if (accountIdEndpointMode == AccountIdEndpointMode.REQUIRED) {
            return Optional.of(BusinessMetricFeatureId.ACCOUNT_ID_MODE_REQUIRED.value());
        }
        if (accountIdEndpointMode == AccountIdEndpointMode.DISABLED) {
            return Optional.of(BusinessMetricFeatureId.ACCOUNT_ID_MODE_DISABLED.value());
        }
        return Optional.empty();
    }
}
