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

package software.amazon.awssdk.services.s3control.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3control.S3ControlConfiguration;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class HandlerUtils {
    public static final String X_AMZ_ACCOUNT_ID = "x-amz-account-id";
    public static final String ENDPOINT_PREFIX = "s3-control";
    public static final String S3_OUTPOSTS = "s3-outposts";

    private HandlerUtils() {
    }

    public static boolean isDualstackEnabled(S3ControlConfiguration configuration) {
        return configuration != null && configuration.dualstackEnabled();
    }

    public static boolean isFipsEnabledInClientConfig(S3ControlConfiguration configuration) {
        return configuration != null && configuration.fipsModeEnabled();
    }

    public static boolean isUseArnRegionEnabledInClientConfig(S3ControlConfiguration configuration) {
        return configuration != null && configuration.useArnRegionEnabled();
    }

    /**
     * Returns whether a FIPS pseudo region is provided.
     */
    public static boolean isFipsRegionProvided(String clientRegion, String arnRegion, boolean useArnRegion) {
        if (useArnRegion) {
            return isFipsRegion(arnRegion);
        }

        return isFipsRegion(clientRegion);
    }

    /**
     * Returns whether a region is a FIPS pseudo region.
     */
    public static boolean isFipsRegion(String regionName) {
        if (StringUtils.isEmpty(regionName)) {
            return false;
        }

        return regionName.startsWith("fips-") || regionName.endsWith("-fips");
    }
}
