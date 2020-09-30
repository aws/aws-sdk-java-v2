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

package software.amazon.awssdk.services.s3control;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * S3 Control specific configuration allowing customers to enabled FIPS or
 * dualstack.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class S3ControlConfiguration implements ServiceConfiguration,
                                                     ToCopyableBuilder<S3ControlConfiguration.Builder, S3ControlConfiguration> {
    /**
     * S3 FIPS mode is by default not enabled
     */
    private static final boolean DEFAULT_FIPS_MODE_ENABLED = false;

    /**
     * S3 Dualstack endpoint is by default not enabled
     */
    private static final boolean DEFAULT_DUALSTACK_ENABLED = false;

    private static final boolean DEFAULT_USE_ARN_REGION_ENABLED = false;

    private final Boolean fipsModeEnabled;
    private final Boolean dualstackEnabled;
    private final Boolean useArnRegionEnabled;
    private final ProfileFile profileFile;
    private final String profileName;

    private S3ControlConfiguration(DefaultS3ServiceConfigurationBuilder builder) {
        this.dualstackEnabled = builder.dualstackEnabled;
        this.fipsModeEnabled = builder.fipsModeEnabled;
        this.profileFile = builder.profileFile;
        this.profileName = builder.profileName;
        this.useArnRegionEnabled = builder.useArnRegionEnabled;
    }

    /**
     * Create a {@link Builder}, used to create a {@link S3ControlConfiguration}.
     */
    public static Builder builder() {
        return new DefaultS3ServiceConfigurationBuilder();
    }

    /**
     * <p>
     * Returns whether the client has enabled fips mode for accessing S3 Control.
     *
     * @return True if client will use FIPS mode.
     */
    public boolean fipsModeEnabled() {
        return resolveBoolean(fipsModeEnabled, DEFAULT_FIPS_MODE_ENABLED);
    }

    /**
     * <p>
     * Returns whether the client is configured to use dualstack mode for
     * accessing S3. If you want to use IPv6 when accessing S3, dualstack
     * must be enabled.
     * </p>
     *
     * <p>
     * Dualstack endpoints are disabled by default.
     * </p>
     *
     * @return True if the client will use the dualstack endpoints
     */
    public boolean dualstackEnabled() {
        return resolveBoolean(dualstackEnabled, DEFAULT_DUALSTACK_ENABLED);
    }

    /**
     * Returns whether the client is configured to make calls to a region specified in an ARN that represents an
     * S3 resource even if that region is different to the region the client was initialized with. This setting is disabled by
     * default.
     *
     * @return true if use arn region is enabled.
     */
    public boolean useArnRegionEnabled() {
        return resolveBoolean(useArnRegionEnabled, DEFAULT_USE_ARN_REGION_ENABLED);
    }

    private boolean resolveBoolean(Boolean suppliedValue, boolean defaultValue) {
        return suppliedValue == null ? defaultValue : suppliedValue;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .dualstackEnabled(dualstackEnabled)
                .fipsModeEnabled(fipsModeEnabled)
                .useArnRegionEnabled(useArnRegionEnabled)
                .profileFile(profileFile)
                .profileName(profileName);
    }

    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, S3ControlConfiguration> {
        Boolean dualstackEnabled();

        /**
         * Option to enable using the dualstack endpoints when accessing S3. Dualstack
         * should be enabled if you want to use IPv6.
         *
         * <p>
         * Dualstack endpoints are disabled by default.
         * </p>
         *
         * @see S3ControlConfiguration#dualstackEnabled().
         */
        Builder dualstackEnabled(Boolean dualstackEnabled);

        Boolean fipsModeEnabled();

        /**
         * Option to enable using the fips endpoint when accessing S3 Control.
         *
         * <p>
         * FIPS mode is disabled by default.
         * </p>
         *
         * @see S3ControlConfiguration#fipsModeEnabled().
         */
        Builder fipsModeEnabled(Boolean fipsModeEnabled);

        /**
         * Option to enable the client to make calls to a region specified in an ARN that represents an S3 resource
         * even if that region is different to the region the client was initialized with. This setting is disabled by
         * default.
         */
        Builder useArnRegionEnabled(Boolean arnRegionEnabled);

        /**
         * Option to enable the client to make calls to a region specified in an ARN that represents an S3 resource
         * even if that region is different to the region the client was initialized with. This setting is disabled by
         * default.
         */
        Boolean useArnRegionEnabled();

        ProfileFile profileFile();

        /**
         * The profile file that should be consulted to determine the service-specific default configuration. This is not
         * currently used by S3 control, but may be in a future SDK version.
         */
        Builder profileFile(ProfileFile profileFile);

        String profileName();

        /**
         * The profile name that should be consulted to determine the service-specific default configuration. This is not
         * currently used by S3 control, but may be in a future SDK version.
         */
        Builder profileName(String profileName);
    }

    private static final class DefaultS3ServiceConfigurationBuilder implements Builder {

        private Boolean dualstackEnabled;
        private Boolean fipsModeEnabled;
        private ProfileFile profileFile;
        private String profileName;
        private Boolean useArnRegionEnabled;

        public Boolean dualstackEnabled() {
            return dualstackEnabled;
        }

        public Builder dualstackEnabled(Boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return this;
        }

        public void setDualstackEnabled(Boolean dualstackEnabled) {
            dualstackEnabled(dualstackEnabled);
        }

        public Boolean fipsModeEnabled() {
            return fipsModeEnabled;
        }

        public Builder fipsModeEnabled(Boolean fipsModeEnabled) {
            this.fipsModeEnabled = fipsModeEnabled;
            return this;
        }

        public void setFipsModeEnabled(Boolean fipsModeEnabled) {
            fipsModeEnabled(fipsModeEnabled);
        }

        @Override
        public Builder useArnRegionEnabled(Boolean arnRegionEnabled) {
            this.useArnRegionEnabled = arnRegionEnabled;
            return this;
        }

        @Override
        public Boolean useArnRegionEnabled() {
            return useArnRegionEnabled;
        }

        public void setUseArnRegionEnabled(Boolean useArnRegionEnabled) {
            useArnRegionEnabled(useArnRegionEnabled);
        }

        @Override
        public ProfileFile profileFile() {
            return profileFile;
        }

        @Override
        public Builder profileFile(ProfileFile profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        @Override
        public String profileName() {
            return profileName;
        }

        @Override
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public S3ControlConfiguration build() {
            return new S3ControlConfiguration(this);
        }
    }
}
