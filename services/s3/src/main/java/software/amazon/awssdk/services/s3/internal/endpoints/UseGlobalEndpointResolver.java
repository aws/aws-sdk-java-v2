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

package software.amazon.awssdk.services.s3.internal.endpoints;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.regions.servicemetadata.EnhancedS3ServiceMetadata;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;

/**
 * Resolve the use global endpoint setting for S3.
 * <p>
 * This logic is identical to that in {@link EnhancedS3ServiceMetadata}, there's no good way to share it aside from creating a
 * protected API that both the s3 and regions module consume.
 */
@SdkInternalApi
public class UseGlobalEndpointResolver {
    private static final Logger LOG = Logger.loggerFor(UseGlobalEndpointResolver.class);
    private static final String REGIONAL_SETTING = "regional";
    private final Lazy<Boolean> useUsEast1RegionalEndpoint;

    public UseGlobalEndpointResolver(SdkClientConfiguration config) {
        String defaultS3UsEast1RegionalEndpointFromSmartDefaults =
            config.option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT);
        this.useUsEast1RegionalEndpoint =
            new Lazy<>(() -> useUsEast1RegionalEndpoint(config.option(SdkClientOption.PROFILE_FILE_SUPPLIER),
                                                        () -> config.option(SdkClientOption.PROFILE_NAME),
                                                        defaultS3UsEast1RegionalEndpointFromSmartDefaults));
    }

    public boolean resolve(Region region) {
        if (!Region.US_EAST_1.equals(region)) {
            return false;
        }

        return !useUsEast1RegionalEndpoint.getValue();
    }

    private boolean useUsEast1RegionalEndpoint(Supplier<ProfileFile> profileFile, Supplier<String> profileName,
                                               String defaultS3UsEast1RegionalEndpoint) {

        String env = envVarSetting();

        if (env != null) {
            return REGIONAL_SETTING.equalsIgnoreCase(env);
        }

        String profile = profileFileSetting(profileFile, profileName);

        if (profile != null) {
            return REGIONAL_SETTING.equalsIgnoreCase(profile);
        }

        return REGIONAL_SETTING.equalsIgnoreCase(defaultS3UsEast1RegionalEndpoint);
    }


    private static String envVarSetting() {
        return SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.getStringValue().orElse(null);
    }

    private String profileFileSetting(Supplier<ProfileFile> profileFileSupplier, Supplier<String> profileNameSupplier) {
        try {
            ProfileFile profileFile = profileFileSupplier.get();
            String profileName = profileNameSupplier.get();
            if (profileFile == null || profileName == null) {
                return null;
            }
            return profileFile.profile(profileName)
                              .flatMap(p -> p.property(ProfileProperty.S3_US_EAST_1_REGIONAL_ENDPOINT))
                              .orElse(null);
        } catch (Exception t) {
            LOG.warn(() -> "Unable to load config file", t);
            return null;
        }
    }

}
