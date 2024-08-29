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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkClientEndpointProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.EndpointTag;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceEndpointKey;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.internal.SystemSettingUtils;

@SdkProtectedApi
public class AwsClientEndpointProvider implements SdkClientEndpointProvider {
    private static final String GLOBAL_ENDPOINT_OVERRIDE_ENVIRONMENT_VARIABLE = "AWS_ENDPOINT_URL";
    private static final String GLOBAL_ENDPOINT_OVERRIDE_SYSTEM_PROPERTY = "aws.endpointUrl";
    private static final String ENDPOINT_OVERRIDE_PROFILE_PROPERTY = "endpoint_url";

    /**
     * A pairing of the client endpoint (URI) and whether it was an endpoint override from the customer (true) or a
     * default endpoint (false).
     */
    private final Lazy<ClientEndpoint> clientEndpoint;

    public AwsClientEndpointProvider(Builder builder) {
        this.clientEndpoint = new Lazy<>(() -> resolveClientEndpoint(new Builder(builder)));
    }

    public static Builder builder() {
        return new Builder();
    }

    public URI clientEndpoint() {
        return clientEndpoint.getValue().clientEndpoint;
    }

    public boolean isEndpointOverridden() {
        return clientEndpoint.getValue().isEndpointOverridden;
    }

    private ClientEndpoint resolveClientEndpoint(Builder builder) {
        return OptionalUtils.firstPresent(clientEndpointFromClientOverride(builder),
                                          () -> clientEndpointFromEnvironment(builder))
                            .orElseGet(() -> clientEndpointFromServiceMetadata(builder));
    }

    private Optional<ClientEndpoint> clientEndpointFromClientOverride(Builder builder) {
        return Optional.ofNullable(builder.clientEndpointOverride)
                       .map(uri -> new ClientEndpoint(uri, true));
    }

    private Optional<ClientEndpoint> clientEndpointFromEnvironment(Builder builder) {
        if (!builder.environmentCredentialsEnabled) {
            return Optional.empty();
        }

        Validate.paramNotNull(builder.serviceEndpointOverrideEnvironmentVariable,
                              "serviceEndpointOverrideEnvironmentVariable");
        Validate.paramNotNull(builder.serviceEndpointOverrideSystemProperty,
                              "serviceEndpointOverrideSystemProperty");
        Validate.paramNotNull(builder.serviceProfileProperty,
                              "serviceProfileProperty");
        return OptionalUtils.firstPresent(systemProperty(builder.serviceEndpointOverrideSystemProperty),
                                          () -> systemProperty(GLOBAL_ENDPOINT_OVERRIDE_SYSTEM_PROPERTY),
                                          () -> environmentVariable(builder.serviceEndpointOverrideEnvironmentVariable),
                                          () -> environmentVariable(GLOBAL_ENDPOINT_OVERRIDE_ENVIRONMENT_VARIABLE),
                                          () -> profileProperty(builder, ENDPOINT_OVERRIDE_PROFILE_PROPERTY),
                                          () -> profileProperty(builder,
                                                                builder.serviceProfileProperty + "."
                                                                + ENDPOINT_OVERRIDE_PROFILE_PROPERTY))
                            .map(uri -> new ClientEndpoint(uri, true));
    }

    private Optional<URI> systemProperty(String systemProperty) {
        return createUri("system property " + systemProperty,
                         Optional.ofNullable(System.getProperty(systemProperty)));
    }

    private Optional<URI> environmentVariable(String environmentVariable) {
        return createUri("environment variable " + environmentVariable,
                         SystemSettingUtils.resolveEnvironmentVariable(environmentVariable));
    }

    private Optional<URI> profileProperty(Builder builder, String profileProperty) {
        initializeProfileFileDefaults(builder);
        return createUri("profile property " + profileProperty,
                         Optional.ofNullable(builder.profileFile.get())
                                 .flatMap(pf -> pf.profile(builder.profileName))
                                 .flatMap(p -> p.property(profileProperty)));
    }

    private ClientEndpoint clientEndpointFromServiceMetadata(Builder builder) {
        // This value is generally overridden, after endpoints 2.0. It seems to exist for backwards-compatibility
        // with older client versions or interceptors.

        Validate.paramNotNull(builder.serviceEndpointPrefix, "serviceName");
        Validate.paramNotNull(builder.region, "region");
        Validate.paramNotNull(builder.protocol, "protocol");

        initializeProfileFileDefaults(builder);

        if (builder.dualstackEnabled == null) {
            builder.dualstackEnabled = DualstackEnabledProvider.builder()
                                                               .profileFile(builder.profileFile)
                                                               .profileName(builder.profileName)
                                                               .build()
                                                               .isDualstackEnabled()
                                                               .orElse(false);
        }

        if (builder.fipsEnabled == null) {
            builder.fipsEnabled = FipsEnabledProvider.builder()
                                                     .profileFile(builder.profileFile)
                                                     .profileName(builder.profileName)
                                                     .build()
                                                     .isFipsEnabled()
                                                     .orElse(false);
        }

        List<EndpointTag> endpointTags = new ArrayList<>();
        if (builder.dualstackEnabled) {
            endpointTags.add(EndpointTag.DUALSTACK);
        }
        if (builder.fipsEnabled) {
            endpointTags.add(EndpointTag.FIPS);
        }

        ServiceMetadata serviceMetadata = ServiceMetadata.of(builder.serviceEndpointPrefix)
                                                         .reconfigure(c -> c.profileFile(builder.profileFile)
                                                                            .profileName(builder.profileName)
                                                                            .advancedOptions(builder.advancedOptions));
        URI endpointWithoutProtocol =
            serviceMetadata.endpointFor(ServiceEndpointKey.builder()
                                                          .region(builder.region)
                                                          .tags(endpointTags)
                                                          .build());
        URI endpoint = URI.create(builder.protocol + "://" + endpointWithoutProtocol);
        if (endpoint.getHost() == null) {
            String error = "Configured region (" + builder.region + ") and tags (" + endpointTags + ") resulted in "
                           + "an invalid URI: " + endpoint + ". This is usually caused by an invalid region "
                           + "configuration.";

            List<Region> exampleRegions = serviceMetadata.regions();
            if (!exampleRegions.isEmpty()) {
                error += " Valid regions: " + exampleRegions;
            }

            throw SdkClientException.create(error);
        }

        return new ClientEndpoint(endpoint, false);
    }

    private Optional<URI> createUri(String source, Optional<String> uri) {
        return uri.map(u -> {
            try {
                return new URI(uri.get());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("An invalid URI was configured in " + source, e);
            }
        });
    }

    private void initializeProfileFileDefaults(Builder builder) {
        if (builder.profileFile == null) {
            builder.profileFile = new Lazy<>(ProfileFile::defaultProfileFile)::getValue;
        }

        if (builder.profileName == null) {
            builder.profileName = ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
        }
    }

    private static final class ClientEndpoint {
        private URI clientEndpoint;
        private boolean isEndpointOverridden;

        private ClientEndpoint(URI clientEndpoint, boolean isEndpointOverridden) {
            this.clientEndpoint = clientEndpoint;
            this.isEndpointOverridden = isEndpointOverridden;
        }
    }

    public static class Builder {
        private URI clientEndpointOverride;
        private String serviceEndpointPrefix;
        private String protocol;
        private Region region;
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private final Map<ServiceMetadataAdvancedOption<?>, Object> advancedOptions = new HashMap<>();
        private Boolean dualstackEnabled;
        private Boolean fipsEnabled;
        private String serviceEndpointOverrideEnvironmentVariable;
        private String serviceEndpointOverrideSystemProperty;
        private String serviceProfileProperty;
        private boolean environmentCredentialsEnabled = true;

        private Builder() {
        }

        private Builder(Builder src) {
            this.clientEndpointOverride = src.clientEndpointOverride;
            this.serviceEndpointPrefix = src.serviceEndpointPrefix;
            this.protocol = src.protocol;
            this.region = src.region;
            this.profileFile = src.profileFile;
            this.profileName = src.profileName;
            this.advancedOptions.putAll(src.advancedOptions);
            this.dualstackEnabled = src.dualstackEnabled;
            this.fipsEnabled = src.fipsEnabled;
        }

        public Builder clientEndpointOverride(URI clientEndpointOverride) {
            Validate.paramNotNull(clientEndpointOverride.getScheme(), "The scheme of the endpoint override");
            this.clientEndpointOverride = clientEndpointOverride;
            return this;
        }

        public Builder serviceEndpointPrefix(String serviceEndpointPrefix) {
            this.serviceEndpointPrefix = serviceEndpointPrefix;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public <T> Builder putAdvancedOption(ServiceMetadataAdvancedOption<T> option, T value) {
            this.advancedOptions.put(option, value);
            return this;
        }

        public Builder dualstackEnabled(Boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return this;
        }

        public Builder fipsEnabled(Boolean fipsEnabled) {
            this.fipsEnabled = fipsEnabled;
            return this;
        }

        public Builder serviceEndpointOverrideEnvironmentVariable(String serviceEndpointOverrideEnvironmentVariable) {
            this.serviceEndpointOverrideEnvironmentVariable = serviceEndpointOverrideEnvironmentVariable;
            return this;
        }

        public Builder serviceEndpointOverrideSystemProperty(String serviceEndpointOverrideSystemProperty) {
            this.serviceEndpointOverrideSystemProperty = serviceEndpointOverrideSystemProperty;
            return this;
        }

        public Builder serviceProfileProperty(String serviceProfileProperty) {
            this.serviceProfileProperty = serviceProfileProperty;
            return this;
        }

        public Builder environmentCredentialsEnabled(boolean environmentCredentialsEnabled) {
            this.environmentCredentialsEnabled = environmentCredentialsEnabled;
            return this;
        }

        public AwsClientEndpointProvider build() {
            return new AwsClientEndpointProvider(this);
        }
    }
}
