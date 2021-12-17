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

package software.amazon.awssdk.regions;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Configuration for a {@link ServiceMetadata}. This allows modifying the values used by default when a metadata instance is
 * generating endpoint data.
 *
 * Created using a {@link #builder()}.
 */
@SdkPublicApi
public final class ServiceMetadataConfiguration {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;
    private final AttributeMap advancedOptions;

    private ServiceMetadataConfiguration(Builder builder) {
        this.profileFile = builder.profileFile;
        this.profileName = builder.profileName;
        this.advancedOptions = builder.advancedOptions.build();
    }

    /**
     * Create a {@link Builder} that can be used to create {@link ServiceMetadataConfiguration} instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retrieve the profile file configured via {@link Builder#profileFile(Supplier)}.
     */
    public Supplier<ProfileFile> profileFile() {
        return profileFile;
    }

    /**
     * Retrieve the profile name configured via {@link Builder#profileName(String)}.
     */
    public String profileName() {
        return profileName;
    }

    /**
     * Load the optional requested advanced option that was configured on the service metadata builder.
     *
     * @see ServiceMetadataConfiguration.Builder#putAdvancedOption(ServiceMetadataAdvancedOption, Object)
     */
    public <T> Optional<T> advancedOption(ServiceMetadataAdvancedOption<T> option) {
        return Optional.ofNullable(advancedOptions.get(option));
    }

    public static final class Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private AttributeMap.Builder advancedOptions = AttributeMap.builder();

        private Builder() {
        }

        /**
         * Configure the profile file used by some services to calculate the endpoint from the region. The supplier is only
         * invoked zero or one time, and only the first time the value is needed.
         *
         * If this is null, the {@link ProfileFile#defaultProfileFile()} is used.
         */
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /**
         * Configure which profile in the {@link #profileFile(Supplier)} should be usedto calculate the endpoint from the region.
         *
         * If this is null, the {@link ProfileFileSystemSetting#AWS_PROFILE} is used.
         */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Configure the map of advanced override options. This will override all values currently configured. The values in the
         * map must match the key type of the map, or a runtime exception will be raised.
         */
        public <T> Builder putAdvancedOption(ServiceMetadataAdvancedOption<T> option, T value) {
            this.advancedOptions.put(option, value);
            return this;
        }

        /**
         * Configure an advanced override option.
         * @see ServiceMetadataAdvancedOption
         */
        public Builder advancedOptions(Map<ServiceMetadataAdvancedOption<?>, ?> advancedOptions) {
            this.advancedOptions.putAll(advancedOptions);
            return this;
        }

        /**
         * Build the {@link ServiceMetadata} instance with the updated configuration.
         */
        public ServiceMetadataConfiguration build() {
            return new ServiceMetadataConfiguration(this);
        }
    }
}
