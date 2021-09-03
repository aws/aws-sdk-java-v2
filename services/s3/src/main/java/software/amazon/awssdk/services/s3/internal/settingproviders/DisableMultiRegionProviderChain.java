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

package software.amazon.awssdk.services.s3.internal.settingproviders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.utils.Logger;

/**
 * {@link DisableMultiRegionProvider} implementation that chains together multiple disable multi-region providers.
 */
@SdkInternalApi
public final class DisableMultiRegionProviderChain implements DisableMultiRegionProvider {
    private static final Logger log = Logger.loggerFor(DisableMultiRegionProvider.class);
    private static final String SETTING = "disableMultiRegion";

    private final List<DisableMultiRegionProvider> providers;

    private DisableMultiRegionProviderChain(List<DisableMultiRegionProvider> providers) {
        this.providers = providers;
    }

    /**
     * Creates a default {@link DisableMultiRegionProviderChain}.
     *
     * <p>
     * AWS disable multi-region provider that looks for the disable flag in this order:
     *
     * <ol>
     *   <li>Check if 'aws.s3DisableMultiRegionAccessPoints' system property is set.</li>
     *   <li>Check if 'AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS' environment is set.</li>
     *   <li>Check if 's3_disable_multiregion_access_points' profile file configuration is set.</li>
     * </ol>
     */
    public static DisableMultiRegionProviderChain create() {
        return create(ProfileFile.defaultProfileFile(),
                      ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow());
    }

    public static DisableMultiRegionProviderChain create(ProfileFile profileFile, String profileName) {
        return new DisableMultiRegionProviderChain(Arrays.asList(
            SystemsSettingsDisableMultiRegionProvider.create(),
            ProfileDisableMultiRegionProvider.create(profileFile, profileName)));
    }

    @Override
    public Optional<Boolean> resolve() {
        for (DisableMultiRegionProvider provider : providers) {
            try {
                Optional<Boolean> value = provider.resolve();
                if (value.isPresent()) {
                    return value;
                }
            } catch (Exception ex) {
                log.warn(() -> "Failed to retrieve " + SETTING + " from " + provider, ex);
            }
        }
        return Optional.empty();
    }
}