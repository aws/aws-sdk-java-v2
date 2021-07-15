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

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;

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

    private Ec2MetadataConfigProvider(Builder builder) {
        this.profileFile = builder.profileFile;
        this.profileName = builder.profileName;
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
