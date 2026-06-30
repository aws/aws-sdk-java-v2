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

package software.amazon.awssdk.core.internal.crac;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Resolves the endpoint used by the CRaC HTTP-client warm-up. The warm-up targets a single predetermined service; that service
 * is currently STS, so this builds the regional STS endpoint ({@code https://sts.<region>.amazonaws.com/}).
 *
 * <p>The region is taken from {@link SdkSystemSetting#AWS_REGION} (the {@code aws.region} system property or {@code AWS_REGION}
 * environment variable), falling back to {@value #DEFAULT_REGION} when it is unset or not a valid hostname component. This
 * matches how the SDK resolves a region from system settings.
 *
 * <p>Only system properties and environment variables are read. The full SDK region-resolution chain (IMDS, profile file) is
 * avoided during priming because those add network or filesystem calls that may fail or time out. The endpoint host always
 * uses the {@code amazonaws.com} suffix, which is incorrect for the China, GovCloud, and ISO partitions; in those partitions
 * the warm-up request simply fails and is ignored, since it is best-effort.
 */
@SdkInternalApi
public final class RegionEndpointResolver {

    static final String DEFAULT_REGION = "us-east-1";

    private static final Logger log = Logger.loggerFor(RegionEndpointResolver.class);

    private RegionEndpointResolver() {
    }

    public static RegionEndpointResolver create() {
        return new RegionEndpointResolver();
    }

    /**
     * @return the regional STS endpoint URI for the resolved region; never null.
     */
    public URI endpoint() {
        return URI.create("https://sts." + resolveRegion() + ".amazonaws.com/");
    }

    private String resolveRegion() {
        // trimToNull turns blank/empty into null so a blank AWS_REGION falls through to the default.
        String awsRegion = SdkSystemSetting.AWS_REGION.getStringValue()
                                                      .map(StringUtils::trimToNull)
                                                      .orElse(null);
        if (awsRegion == null) {
            return DEFAULT_REGION;
        }
        // A real region is a hostname-compliant token. Reject anything else so it cannot alter the endpoint host, and fall
        // back to the default so the best-effort warm-up still runs.
        try {
            HostnameValidator.validateHostnameCompliant(awsRegion, "region", "AWS_REGION");
            return awsRegion;
        } catch (IllegalArgumentException e) {
            log.debug(() -> "Configured region is not a valid hostname component; using " + DEFAULT_REGION + " for warm-up.", e);
            return DEFAULT_REGION;
        }
    }
}
