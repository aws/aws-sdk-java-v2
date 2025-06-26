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

package software.amazon.awssdk.auth.credentials.internal;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.OptionalUtils;

@SdkInternalApi
// TODO: Remove or consolidate this class with the one from the regions module.
// There's currently no good way for both auth and regions to share the same
// class since there's no suitable common dependency between the two where this
// can live. Ideally, we can do this when the EC2MetadataUtils is replaced with
// the IMDS client.
public final class Ec2MetadataConfigProvider {
    /** Default IPv4 endpoint for the Amazon EC2 Instance Metadata Service. */
    private static final String EC2_METADATA_SERVICE_URL_IPV4 = "http://169.254.169.254";

    /** Default IPv6 endpoint for the Amazon EC2 Instance Metadata Service. */
    private static final String EC2_METADATA_SERVICE_URL_IPV6 = "http://[fd00:ec2::254]";

    private final Supplier<ProfileFile> profileFile;
    private final String profileName;

    private final Lazy<Boolean> metadataV1Disabled;
    private final Lazy<Long> serviceTimeout;
    private final Lazy<String> ec2InstanceProfileName;

    private Ec2MetadataConfigProvider(Builder builder) {
        this.profileFile = builder.profileFile;
        this.profileName = builder.profileName;
        this.metadataV1Disabled = new Lazy<>(this::resolveMetadataV1Disabled);
        this.serviceTimeout = new Lazy<>(this::resolveServiceTimeout);
        this.ec2InstanceProfileName = new Lazy<>(this::resolveEc2InstanceProfileName);
    }

    public enum EndpointMode {
        IPV4,
        IPV6,
        ;

        public static EndpointMode fromValue(String s) {
            if (s == null) {
                return null;
            }

            for (EndpointMode value : EndpointMode.values()) {
                if (value.name().equalsIgnoreCase(s)) {
                    return value;
                }
            }

            throw new IllegalArgumentException("Unrecognized value for endpoint mode: " + s);
        }
    }

    public String getEndpoint() {
        String endpointOverride = getEndpointOverride();
        if (endpointOverride != null) {
            return endpointOverride;
        }

        EndpointMode endpointMode = getEndpointMode();
        switch (endpointMode) {
            case IPV4:
                return EC2_METADATA_SERVICE_URL_IPV4;
            case IPV6:
                return EC2_METADATA_SERVICE_URL_IPV6;
            default:
                throw SdkClientException.create("Unknown endpoint mode: " + endpointMode);
        }
    }

    public EndpointMode getEndpointMode() {
        Optional<String> endpointMode = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.getNonDefaultStringValue();
        if (endpointMode.isPresent()) {
            return EndpointMode.fromValue(endpointMode.get());
        }

        return configFileEndpointMode().orElseGet(() ->
                EndpointMode.fromValue(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.defaultValue()));
    }

    public String getEndpointOverride() {
        Optional<String> endpointOverride = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.getNonDefaultStringValue();
        if (endpointOverride.isPresent()) {
            return endpointOverride.get();
        }

        Optional<String> configFileValue = configFileEndpointOverride();

        return configFileValue.orElse(null);
    }

    /**
     * Resolves whether EC2 Metadata V1 is disabled.
     * @return true if EC2 Metadata V1 is disabled, false otherwise.
     */
    public boolean isMetadataV1Disabled() {
        return metadataV1Disabled.getValue();
    }

    /**
     * Resolves the EC2 Metadata Service Timeout in milliseconds.
     * @return the timeout value in milliseconds.
     */
    public long serviceTimeout() {
        return serviceTimeout.getValue();
    }
    
    /**
     * Resolves the EC2 Instance Profile Name to use.
     * @return the EC2 Instance Profile Name or null if not specified.
     */
    public String ec2InstanceProfileName() {
        return ec2InstanceProfileName.getValue();
    }

    // Internal resolution logic for Metadata V1 disabled
    private boolean resolveMetadataV1Disabled() {
        return OptionalUtils.firstPresent(
                                fromSystemSettingsMetadataV1Disabled(),
                                () -> fromProfileFileMetadataV1Disabled(profileFile, profileName)
                            )
                            .orElse(false);
    }

    // Internal resolution logic for Service Timeout
    private long resolveServiceTimeout() {
        return OptionalUtils.firstPresent(
                                fromSystemSettingsServiceTimeout(),
                                () -> fromProfileFileServiceTimeout(profileFile, profileName)
                            )
                            .orElseGet(() -> parseTimeoutValue(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.defaultValue()));
    }

    private String resolveEc2InstanceProfileName() {
        return OptionalUtils.firstPresent(
                                fromSystemSettingsEc2InstanceProfileName(),
                                () -> fromProfileFileEc2InstanceProfileName(profileFile, profileName)
                            )
                            .orElse(null);
    }

    // System settings resolution for Metadata V1 disabled
    private static Optional<Boolean> fromSystemSettingsMetadataV1Disabled() {
        return SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.getBooleanValue();
    }

    // Profile file resolution for Metadata V1 disabled
    private static Optional<Boolean> fromProfileFileMetadataV1Disabled(Supplier<ProfileFile> profileFile, String profileName) {
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.booleanProperty(ProfileProperty.EC2_METADATA_V1_DISABLED));
    }

    // System settings resolution for Service Timeout
    private static Optional<Long> fromSystemSettingsServiceTimeout() {
        return SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.getNonDefaultStringValue()
                                                            .map(Ec2MetadataConfigProvider::parseTimeoutValue);
    }

    // Profile file resolution for Service Timeout
    private static Optional<Long> fromProfileFileServiceTimeout(Supplier<ProfileFile> profileFile, String profileName) {
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.property(ProfileProperty.METADATA_SERVICE_TIMEOUT))
                          .map(Ec2MetadataConfigProvider::parseTimeoutValue);
    }
    
    // System settings resolution for EC2 Instance Profile Name
    private static Optional<String> fromSystemSettingsEc2InstanceProfileName() {
        return SdkSystemSetting.AWS_EC2_INSTANCE_PROFILE_NAME.getNonDefaultStringValue();
    }
    
    // Profile file resolution for EC2 Instance Profile Name
    private static Optional<String> fromProfileFileEc2InstanceProfileName(Supplier<ProfileFile> profileFile, String profileName) {
        try {
            return profileFile.get()
                              .profile(profileName)
                              .flatMap(p -> p.property(ProfileProperty.EC2_INSTANCE_PROFILE_NAME));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Parses a timeout value from a string to milliseconds
    private static long parseTimeoutValue(String timeoutValue) {
        try {
            int timeoutSeconds = Integer.parseInt(timeoutValue);
            return Duration.ofSeconds(timeoutSeconds).toMillis();
        } catch (NumberFormatException e) {
            try {
                double timeoutSeconds = Double.parseDouble(timeoutValue);
                return Math.round(timeoutSeconds * 1000);
            } catch (NumberFormatException ignored) {
                throw new IllegalStateException(String.format(
                    "Timeout value '%s' is not a valid integer or double.",
                    timeoutValue
                ));
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private Optional<EndpointMode> configFileEndpointMode() {
        return resolveProfile().flatMap(p -> p.property(ProfileProperty.EC2_METADATA_SERVICE_ENDPOINT_MODE))
                               .map(EndpointMode::fromValue);
    }

    private Optional<String> configFileEndpointOverride() {
        return resolveProfile().flatMap(p -> p.property(ProfileProperty.EC2_METADATA_SERVICE_ENDPOINT));
    }

    private Optional<Profile> resolveProfile() {
        ProfileFile profileFileToUse = resolveProfileFile();
        String profileNameToUse = resolveProfileName();

        return profileFileToUse.profile(profileNameToUse);
    }

    private ProfileFile resolveProfileFile() {
        if (profileFile != null) {
            return profileFile.get();
        }

        return ProfileFile.defaultProfileFile();
    }

    private String resolveProfileName() {
        if (profileName != null) {
            return profileName;
        }

        return ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
    }

    public static class Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public Ec2MetadataConfigProvider build() {
            return new Ec2MetadataConfigProvider(this);
        }
    }
}
