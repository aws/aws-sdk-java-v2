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

package software.amazon.awssdk.awscore.endpoint;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Validate;

/**
 * Resolves whether configured endpoint URLs should be ignored. This checks the system property, environment variable,
 * and profile file for the {@code ignore_configured_endpoint_urls} setting.
 *
 * <p>When this returns {@code true}, the SDK will not read endpoint URLs from environment variables, system properties,
 * or the shared configuration file. Programmatic endpoint overrides on the client builder are not affected.
 */
@SdkProtectedApi
public class IgnoreConfiguredEndpointUrlsProvider {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;

    private IgnoreConfiguredEndpointUrlsProvider(Builder builder) {
        this.profileFile = Validate.paramNotNull(builder.profileFile, "profileFile");
        this.profileName = builder.profileName;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns {@code true} when configured endpoint URLs should be ignored, {@code false} otherwise.
     * Resolution order: system property, then environment variable, then profile file. If none are set, returns
     * empty.
     */
    public Optional<Boolean> ignoreConfiguredEndpointUrls() {
        Optional<Boolean> setting = SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.getBooleanValue();
        if (setting.isPresent()) {
            return setting;
        }

        return profileFile.get()
                          .profile(profileName())
                          .flatMap(p -> p.booleanProperty(ProfileProperty.IGNORE_CONFIGURED_ENDPOINT_URLS));
    }

    private String profileName() {
        return profileName != null ? profileName : ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
    }

    public static final class Builder {
        private Supplier<ProfileFile> profileFile = ProfileFile::defaultProfileFile;
        private String profileName;

        private Builder() {
        }

        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public IgnoreConfiguredEndpointUrlsProvider build() {
            return new IgnoreConfiguredEndpointUrlsProvider(this);
        }
    }
}
