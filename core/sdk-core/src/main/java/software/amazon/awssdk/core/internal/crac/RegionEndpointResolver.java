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
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.SystemSetting;
import software.amazon.awssdk.utils.http.SdkHttpUtils;
import software.amazon.awssdk.utils.internal.SystemSettingUtils;

/**
 * Resolves the regional STS endpoint ({@code https://sts.<region>.amazonaws.com/}) used by the CRaC HTTP-client warm-up.
 *
 * <p>The region is taken from the first of: {@link SdkSystemSetting#AWS_REGION} (the {@code aws.region} system property or
 * {@code AWS_REGION} environment variable), the {@code AWS_DEFAULT_REGION} environment variable, or {@value #DEFAULT_REGION}.
 *
 * <p>Only system properties and environment variables are read. The full SDK region-resolution chain (IMDS, profile file) is
 * avoided during priming because those add network or filesystem calls that may fail or time out. The endpoint host always
 * uses the {@code amazonaws.com} suffix, which is incorrect for the China, GovCloud, and ISO partitions; in those partitions
 * the warm-up request simply fails and is ignored, since it is best-effort.
 */
@SdkInternalApi
public final class RegionEndpointResolver {

    static final String DEFAULT_REGION = "us-east-1";

    private RegionEndpointResolver() {
    }

    public static RegionEndpointResolver create() {
        return new RegionEndpointResolver();
    }

    /**
     * @return the regional STS endpoint URI for the resolved region; never null.
     */
    public URI stsEndpoint() {
        // URL-encode the region before putting it in the host, same as Region.of(String).
        return URI.create("https://sts." + SdkHttpUtils.urlEncode(resolveRegion()) + ".amazonaws.com/");
    }

    private String resolveRegion() {
        Optional<String> awsRegion = trimmed(SdkSystemSetting.AWS_REGION.getStringValue());
        return OptionalUtils.firstPresent(awsRegion, RegionEndpointResolver::awsDefaultRegion)
                            .orElse(DEFAULT_REGION);
    }

    private static Optional<String> awsDefaultRegion() {
        return trimmed(SystemSettingUtils.resolveEnvironmentVariable(new AwsDefaultRegionEnvVar()));
    }

    private static Optional<String> trimmed(Optional<String> value) {
        // trimToNull returns null for blank/empty input, so Optional.map collapses those to an empty Optional.
        return value.map(StringUtils::trimToNull);
    }

    // AWS_DEFAULT_REGION is an environment-variable-only fallback with no system-property equivalent.
    private static final class AwsDefaultRegionEnvVar implements SystemSetting {
        @Override
        public String property() {
            return null;
        }

        @Override
        public String environmentVariable() {
            return "AWS_DEFAULT_REGION";
        }

        @Override
        public String defaultValue() {
            return null;
        }
    }
}
