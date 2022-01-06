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

package software.amazon.awssdk.awscore.internal.defaultsmode;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.utils.JavaSystemSetting;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.SystemSetting;
import software.amazon.awssdk.utils.internal.SystemSettingUtils;

/**
 * This class attempts to discover the appropriate {@link DefaultsMode} by inspecting the environment. It falls
 * back to the {@link DefaultsMode#STANDARD} mode if the target mode cannot be determined.
 */
@SdkInternalApi
public class AutoDefaultsModeDiscovery {
    private static final String EC2_METADATA_REGION_PATH = "/latest/meta-data/placement/region";
    private static final DefaultsMode FALLBACK_DEFAULTS_MODE = DefaultsMode.STANDARD;
    private static final String ANDROID_JAVA_VENDOR = "The Android Project";
    private static final String AWS_DEFAULT_REGION_ENV_VAR = "AWS_DEFAULT_REGION";

    /**
     *  Discovers the defaultMode using the following workflow:
     *
     *  1. Check if it's on mobile
     *  2. If it's not on mobile (best we can tell), see if we can determine whether we're an in-region or cross-region client.
     *  3. If we couldn't figure out the region from environment variables. Check IMDSv2. This step might take up to 1 second
     *  (default connect timeout)
     *  4. Finally, use fallback mode
     */
    public DefaultsMode discover(Region regionResolvedFromSdkClient) {

        if (isMobile()) {
            return DefaultsMode.MOBILE;
        }

        if (isAwsExecutionEnvironment()) {
            Optional<String> regionStr = regionFromAwsExecutionEnvironment();

            if (regionStr.isPresent()) {
                return compareRegion(regionStr.get(), regionResolvedFromSdkClient);
            }
        }

        Optional<String> regionFromEc2 = queryImdsV2();
        if (regionFromEc2.isPresent()) {
            return compareRegion(regionFromEc2.get(), regionResolvedFromSdkClient);
        }

        return FALLBACK_DEFAULTS_MODE;
    }

    private static DefaultsMode compareRegion(String region, Region clientRegion) {
        if (region.equalsIgnoreCase(clientRegion.id())) {
            return DefaultsMode.IN_REGION;
        }

        return DefaultsMode.CROSS_REGION;
    }

    private static Optional<String> queryImdsV2() {
        try {
            String ec2InstanceRegion = EC2MetadataUtils.fetchData(EC2_METADATA_REGION_PATH, false, 1);
            // ec2InstanceRegion could be null
            return Optional.ofNullable(ec2InstanceRegion);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    /**
     * Check to see if the application is running on a mobile device by verifying the Java
     * vendor system property. Currently only checks for Android. While it's technically possible to
     * use Java with iOS, it's not a common use-case.
     * <p>
     * https://developer.android.com/reference/java/lang/System#getProperties()
     */
    private static boolean isMobile() {
        return JavaSystemSetting.JAVA_VENDOR.getStringValue()
                                            .filter(o -> o.equals(ANDROID_JAVA_VENDOR))
                                            .isPresent();
    }

    private static boolean isAwsExecutionEnvironment() {
        return SdkSystemSetting.AWS_EXECUTION_ENV.getStringValue().isPresent();
    }

    private static Optional<String> regionFromAwsExecutionEnvironment() {
        Optional<String> regionFromRegionEnvVar = SdkSystemSetting.AWS_REGION.getStringValue();
        return OptionalUtils.firstPresent(regionFromRegionEnvVar,
                                          () -> SystemSettingUtils.resolveEnvironmentVariable(new DefaultRegionEnvVar()));
    }

    private static final class DefaultRegionEnvVar implements SystemSetting {
        @Override
        public String property() {
            return null;
        }

        @Override
        public String environmentVariable() {
            return AWS_DEFAULT_REGION_ENV_VAR;
        }

        @Override
        public String defaultValue() {
            return null;
        }
    }
}
