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

package software.amazon.awssdk.imds.internal;

import static software.amazon.awssdk.imds.EndpointMode.IPV4;
import static software.amazon.awssdk.imds.EndpointMode.IPV6;

import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Validate;

/**
 * Endpoint Provider Class which contains methods for endpoint resolution.
 */
@SdkInternalApi
public final class Ec2MetadataEndpointProvider {

    private static final Ec2MetadataEndpointProvider DEFAULT_ENDPOINT_PROVIDER = builder().build();

    private static final EnumMap<EndpointMode, String> DEFAULT_ENDPOINT_MODE;

    static {
        DEFAULT_ENDPOINT_MODE = new EnumMap<>(EndpointMode.class);
        DEFAULT_ENDPOINT_MODE.put(IPV4, "http://169.254.169.254");
        DEFAULT_ENDPOINT_MODE.put(IPV6, "http://[fd00:ec2::254]");
    }

    private final Supplier<ProfileFile> profileFile;
    private final String profileName;

    private Ec2MetadataEndpointProvider(Builder builder) {
        this.profileFile = builder.profileFile;
        this.profileName = builder.profileName;
    }

    public static Ec2MetadataEndpointProvider instance() {
        return DEFAULT_ENDPOINT_PROVIDER;
    }

    /**
     * Resolve the endpoint to be used for the {@link DefaultEc2MetadataClient} client. Users may manually provide an endpoint
     * through the {@code AWS_EC2_METADATA_SERVICE_ENDPOINT} environment variable or the {@code ec2_metadata_service_endpoint}
     * key in their aws config file.
     * If an endpoint is specified is this manner, use it. If no values are provided, the defaults to:
     * <ol>
     *     <li>If endpoint mode is set to IPv4: {@code "http://169.254.169.254"}</li>
     *     <li>If endpoint mode is set to IPv6: {@code "http://[fd00:ec2::254]"}</li>
     * </ol>
     * (the default endpoint mode is IPV4).
     * @param endpointMode Used only if an endpoint value is not specified. If so, this method will use the endpointMode to
     *                     choose the default value to return.
     * @return the String representing the endpoint to be used,
     */
    public String resolveEndpoint(EndpointMode endpointMode) {
        Optional<String> endpointFromSystem = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.getNonDefaultStringValue();
        if (endpointFromSystem.isPresent()) {
            return stripEndingSlash(endpointFromSystem.get());
        }

        Optional<String> endpointFromConfigProfile = resolveProfile()
            .flatMap(profile -> profile.property(ProfileProperty.EC2_METADATA_SERVICE_ENDPOINT));
        if (endpointFromConfigProfile.isPresent()) {
            return stripEndingSlash(endpointFromConfigProfile.get());
        }

        Validate.notNull(endpointMode, "endpointMode must not be null.");
        return endpointFromConfigProfile.orElseGet(() -> DEFAULT_ENDPOINT_MODE.get(endpointMode));
    }

    private static String stripEndingSlash(String uri) {
        return uri.endsWith("/")
               ? uri.substring(0, uri.length() - 1)
               : uri;
    }

    public EndpointMode resolveEndpointMode() {
        Optional<String> systemEndpointMode = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.getNonDefaultStringValue();
        if (systemEndpointMode.isPresent()) {
            return EndpointMode.fromValue(systemEndpointMode.get());
        }

        Optional<EndpointMode> configFileEndPointMode = resolveProfile()
            .flatMap(p -> p.property(ProfileProperty.EC2_METADATA_SERVICE_ENDPOINT_MODE))
            .map(EndpointMode::fromValue);
        if (configFileEndPointMode.isPresent()) {
            return configFileEndPointMode.get();
        }

        String defaultSystemEndpointMode = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.defaultValue();
        return EndpointMode.fromValue(defaultSystemEndpointMode);
    }

    public Optional<Profile> resolveProfile() {
        String profileNameToUse = profileName == null
                                  ? ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow()
                                  : profileName;
        ProfileFile profileFileToUse = profileFile.get();
        return profileFileToUse.profile(profileNameToUse);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        private Builder() {
            this.profileFile = ProfileFile::defaultProfileFile;
        }

        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            Validate.notNull(profileFile, "profileFile Supplier must not be null");
            this.profileFile = profileFile;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public Ec2MetadataEndpointProvider build() {
            return new Ec2MetadataEndpointProvider(this);
        }
    }

}
