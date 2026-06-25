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
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * Resolves the regional STS endpoint that the CRaC HTTP-client warm-up sends its GET to.
 *
 * <p>Region precedence: {@code aws.region} system property, {@code AWS_REGION} env, {@code AWS_DEFAULT_REGION} env, then
 * {@value #DEFAULT_REGION}. Tiers are resolved independently so a blank value falls through; the combined
 * {@code SdkSystemSetting.AWS_REGION.getStringValue()} cannot be used because a blank {@code aws.region} would shadow the env
 * vars.
 *
 * <p>Reads only system properties and env vars: no IMDS, profile-file, or credential-provider lookups, which would slow
 * priming. The host is partition-naive ({@code sts.<region>.amazonaws.com}); the warm-up only JIT-compiles DNS/TLS/cert-chain
 * against any reachable AWS host and is best-effort, so a wrong host in cn/gov/iso partitions fails and is swallowed.
 */
@SdkInternalApi
public final class RegionEndpointResolver {

    static final String DEFAULT_REGION = "us-east-1";

    private static final String AWS_REGION_PROPERTY = "aws.region";
    private static final String AWS_REGION_ENV_VAR = "AWS_REGION";
    private static final String AWS_DEFAULT_REGION_ENV_VAR = "AWS_DEFAULT_REGION";

    // Property-only SystemSetting (null env var) so aws.region is read as a tier distinct from the AWS_REGION env var,
    // without a direct System.getProperty call (Checkstyle-banned outside the system-setting utilities).
    private static final SystemSetting AWS_REGION_PROPERTY_SETTING = new SystemSetting() {
        @Override
        public String property() {
            return AWS_REGION_PROPERTY;
        }

        @Override
        public String environmentVariable() {
            return null;
        }

        @Override
        public String defaultValue() {
            return null;
        }
    };

    private RegionEndpointResolver() {
    }

    public static RegionEndpointResolver create() {
        return new RegionEndpointResolver();
    }

    /**
     * @return the regional STS endpoint URI for the resolved region; never null.
     */
    public URI stsEndpoint() {
        return URI.create("https://sts." + resolveRegion() + ".amazonaws.com/");
    }

    private String resolveRegion() {
        return trimmed(AWS_REGION_PROPERTY_SETTING.getStringValue())
            .orElseGet(() -> trimmed(SystemSetting.getStringValueFromEnvironmentVariable(AWS_REGION_ENV_VAR))
                .orElseGet(() -> trimmed(SystemSetting.getStringValueFromEnvironmentVariable(AWS_DEFAULT_REGION_ENV_VAR))
                    .orElse(DEFAULT_REGION)));
    }

    private static Optional<String> trimmed(Optional<String> value) {
        // trimToNull returns null for blank/empty input, so Optional.map collapses those to an empty Optional.
        return value.map(StringUtils::trimToNull);
    }
}
