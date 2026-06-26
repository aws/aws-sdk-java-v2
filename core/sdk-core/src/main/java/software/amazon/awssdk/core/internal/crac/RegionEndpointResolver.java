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
 * Resolves the regional STS endpoint ({@code https://sts.<region>.amazonaws.com/}) used by the CRaC HTTP-client warm-up.
 *
 * <p>The region is taken from the first of: {@code aws.region} system property, {@code AWS_REGION} environment variable,
 * {@code AWS_DEFAULT_REGION} environment variable, or {@value #DEFAULT_REGION}. A blank value at one source is ignored and the
 * next is tried.
 *
 * <p>Only system properties and environment variables are read. The full SDK region-resolution chain (IMDS, profile file) is
 * avoided during priming because those add network or filesystem calls that may fail or time out. The endpoint host always
 * uses the {@code amazonaws.com} suffix, which is incorrect for the China, GovCloud, and ISO partitions; in those partitions
 * the warm-up request simply fails and is ignored, since it is best-effort.
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
