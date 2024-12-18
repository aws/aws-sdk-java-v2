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
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.EndpointTag;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceEndpointKey;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.internal.SystemSettingUtils;

/**
 * An implementation of {@link ClientEndpointProvider} that loads the default client endpoint from:
 * <ol>
 *     <li>The client-level endpoint override</li>
 *     <li>The service-specific endpoint override system property (e.g. 'aws.endpointUrlS3')</li>
 *     <li>The service-agnostic endpoint override system property (i.e. 'aws.endpointUrl')</li>
 *     <li>The service-specific endpoint override environment variable (e.g. 'AWS_ENDPOINT_URL_S3')</li>
 *     <li>The service-agnostic endpoint override environment variable (i.e. 'AWS_ENDPOINT_URL')</li>
 *     <li>The service-specific endpoint override profile property (e.g. 's3.endpoint_url')</li>
 *     <li>The service-agnostic endpoint override profile property (i.e. 'endpoint_url')</li>
 *     <li>The {@link ServiceMetadata} for the service</li>
 * </ol>
 */
@SdkProtectedApi
public final class AwsClientEndpointProvider implements ClientEndpointProvider {
    private static final Logger log = Logger.loggerFor(AwsClientEndpointProvider.class);

    private static final String GLOBAL_ENDPOINT_OVERRIDE_ENVIRONMENT_VARIABLE = "AWS_ENDPOINT_URL";
    private static final String GLOBAL_ENDPOINT_OVERRIDE_SYSTEM_PROPERTY = "aws.endpointUrl";

    /**
     * A pairing of the client endpoint (URI) and whether it was an endpoint override from the customer (true) or a
     * default endpoint (false).
     */
    private final Lazy<ClientEndpoint> clientEndpoint;

    private AwsClientEndpointProvider(Builder builder) {
        this.clientEndpoint = new Lazy<>(() -> resolveClientEndpoint(new Builder(builder)));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retrieve the endpoint to be used by the client.
     * @throws SdkClientException If the endpoint could not be loaded or was invalid
     */
    public URI clientEndpoint() {
        return clientEndpoint.getValue().clientEndpoint;
    }

    /**
     * Return true if the endpoint was overridden by the customer, or false if it was loaded from the
     * {@link ServiceMetadata}.
     * @throws SdkClientException If the endpoint could not be loaded or was invalid
     */
    public boolean isEndpointOverridden() {
        return clientEndpoint.getValue().isEndpointOverridden;
    }

    private ClientEndpoint resolveClientEndpoint(Builder builder) {
        return OptionalUtils.firstPresent(clientEndpointFromClientOverride(builder),
                                          () -> clientEndpointFromEnvironment(builder),
                                          () -> clientEndpointFromServiceMetadata(builder))
                            .orElseThrow(AwsClientEndpointProvider::failToLoadEndpointException);
    }

    private static SdkClientException failToLoadEndpointException() {
        return SdkClientException.create("Unable to determine the default client endpoint. Enable TRACE logging on " +
                                         AwsClientEndpointProvider.class.getName() + " for more information.");
    }

    private Optional<ClientEndpoint> clientEndpointFromClientOverride(Builder builder) {
        Optional<ClientEndpoint> result = Optional.ofNullable(builder.clientEndpointOverride)
                                                  .map(uri -> new ClientEndpoint(uri, true));
        result.ifPresent(e -> log.trace(() -> "Client was configured with endpoint override: " + e.clientEndpoint));
        return result;
    }

    private Optional<ClientEndpoint> clientEndpointFromEnvironment(Builder builder) {
        if (builder.serviceEndpointOverrideEnvironmentVariable == null ||
            builder.serviceEndpointOverrideSystemProperty == null ||
            builder.serviceProfileProperty == null) {
            Validate.isTrue(builder.serviceEndpointOverrideEnvironmentVariable == null &&
                            builder.serviceEndpointOverrideSystemProperty == null &&
                            builder.serviceProfileProperty == null ,
                            "If any of the service endpoint override environment variable, system property or profile "
                            + "property are configured, they must all be configured.");
            log.trace(() -> "Environment was not checked for client endpoint.");
            return Optional.empty();
        }

        return OptionalUtils.firstPresent(systemProperty(builder.serviceEndpointOverrideSystemProperty),
                                          () -> systemProperty(GLOBAL_ENDPOINT_OVERRIDE_SYSTEM_PROPERTY),
                                          () -> environmentVariable(builder.serviceEndpointOverrideEnvironmentVariable),
                                          () -> environmentVariable(GLOBAL_ENDPOINT_OVERRIDE_ENVIRONMENT_VARIABLE),
                                          () -> profileProperty(builder,
                                                                builder.serviceProfileProperty + "."
                                                                + ProfileProperty.ENDPOINT_URL),
                                          () -> profileProperty(builder, ProfileProperty.ENDPOINT_URL))
                            .map(uri -> new ClientEndpoint(uri, true));
    }

    private Optional<URI> systemProperty(String systemProperty) {
        // CHECKSTYLE:OFF - We have to read system properties directly here to match the load order of the other SDKs
        return createUri("system property " + systemProperty,
                         Optional.ofNullable(System.getProperty(systemProperty)));
        // CHECKSTYLE:ON
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

    private Optional<ClientEndpoint> clientEndpointFromServiceMetadata(Builder builder) {
        // This value is generally overridden after endpoints 2.0. It seems to exist for backwards-compatibility
        // with older client versions or interceptors.

        if (builder.serviceEndpointPrefix == null ||
            builder.region == null ||
            builder.protocol == null) {
            // Make sure that people didn't set just one value and expect it to be used.
            Validate.isTrue(builder.serviceEndpointPrefix == null &&
                            builder.region == null &&
                            builder.protocol == null,
                            "If any of the service endpoint prefix, region or protocol are configured, they must all "
                            + "be configured.");
            log.trace(() -> "Service metadata was not checked for client endpoint.");
            return Optional.empty();
        }

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

        log.trace(() -> "Client endpoint was loaded from service metadata, but this endpoint will likely be overridden "
                        + "at the request-level by the endpoint resolver: " + endpoint);
        return Optional.of(new ClientEndpoint(endpoint, false));
    }

    private Optional<URI> createUri(String source, Optional<String> uri) {
        return uri.map(u -> {
            try {
                URI parsedUri = new URI(uri.get());
                log.trace(() -> "Client endpoint was loaded from the " + source + ": " + parsedUri);
                return parsedUri;
            } catch (URISyntaxException e) {
                throw SdkClientException.create("An invalid URI was configured in " + source, e);
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
        private final URI clientEndpoint;
        private final boolean isEndpointOverridden;

        private ClientEndpoint(URI clientEndpoint, boolean isEndpointOverridden) {
            this.clientEndpoint = clientEndpoint;
            this.isEndpointOverridden = isEndpointOverridden;
        }

        @Override
        public String toString() {
            return ToString.builder("ClientEndpoint")
                           .add("clientEndpoint", clientEndpoint)
                           .add("isEndpointOverridden", isEndpointOverridden)
                           .build();
        }
    }

    public static final class Builder {
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
            this.serviceEndpointOverrideEnvironmentVariable = src.serviceEndpointOverrideEnvironmentVariable;
            this.serviceEndpointOverrideSystemProperty = src.serviceEndpointOverrideSystemProperty;
            this.serviceProfileProperty = src.serviceProfileProperty;
        }

        /**
         * Set the endpoint that was overridden by the customer using {@link SdkClientBuilder#endpointOverride(URI)}.
         * <p>
         * If specified, this provider will behave the same as
         * {@link ClientEndpointProvider#forEndpointOverride(URI)}. Other configured values will not be used.
         */
        public Builder clientEndpointOverride(URI clientEndpointOverride) {
            if (clientEndpointOverride != null) {
                Validate.paramNotNull(clientEndpointOverride.getScheme(), "The scheme of the endpoint override");
            }
            this.clientEndpointOverride = clientEndpointOverride;
            return this;
        }

        /**
         * Set the service-specific environment variable that should be used to load the endpoint override for this
         * service.
         * <p>
         * If this value is set, {@link #serviceEndpointOverrideSystemProperty} and {@link #serviceProfileProperty} must
         * also be set.
         * <p>
         * If this value is not set, loading the service endpoint from the environment is skipped.
         */
        public Builder serviceEndpointOverrideEnvironmentVariable(String serviceEndpointOverrideEnvironmentVariable) {
            this.serviceEndpointOverrideEnvironmentVariable = serviceEndpointOverrideEnvironmentVariable;
            return this;
        }

        /**
         * Set the service-specific system property that should be used to load the endpoint override for this service.
         * <p>
         * If this value is set, {@link #serviceEndpointOverrideEnvironmentVariable} and {@link #serviceProfileProperty}
         * must also be set.
         * <p>
         * If this value is not set, loading the service endpoint from the environment is skipped.
         */
        public Builder serviceEndpointOverrideSystemProperty(String serviceEndpointOverrideSystemProperty) {
            this.serviceEndpointOverrideSystemProperty = serviceEndpointOverrideSystemProperty;
            return this;
        }

        /**
         * Set the service-specific profile property that should be used to load the endpoint override for this service.
         * <p>
         * If this value is set, {@link #serviceEndpointOverrideEnvironmentVariable} and
         * {@link #serviceEndpointOverrideSystemProperty} must also be set.
         * <p>
         * If this value is not set, loading the service endpoint from the environment is skipped.
         */
        public Builder serviceProfileProperty(String serviceProfileProperty) {
            this.serviceProfileProperty = serviceProfileProperty;
            return this;
        }

        /**
         * Set the profile file supplier that will supply the customer's profile file. This profile file is used both
         * for checking for the endpoint override and when resolving the endpoint from {@link ServiceMetadata}.
         * <p>
         * If this value is not set, the {@link ProfileFile#defaultProfileFile()} is used.
         */
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /**
         * Set the profile name for the profile that should be used . This profile is used both
         * for checking for the endpoint override and when resolving the endpoint from {@link ServiceMetadata}.
         * <p>
         * If this value is not set, {@link ProfileFileSystemSetting#AWS_PROFILE} is used.
         */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Set the service endpoint prefix, as it is expected by {@link ServiceMetadata#of(String)}.
         * <p>
         * This value will only be used if the endpoint is loaded from service metadata.
         * <p>
         * If this value is set, {@link #defaultProtocol} and {@link #region} must also be set. If this value is not
         * set, loading the service endpoint from service metadata is skipped.
         */
        public Builder serviceEndpointPrefix(String serviceEndpointPrefix) {
            this.serviceEndpointPrefix = serviceEndpointPrefix;
            return this;
        }

        /**
         * Set the protocol to be used for endpoints returned by {@link ServiceMetadata#of(String)}.
         * <p>
         * This value will only be used if the endpoint is loaded from service metadata.
         * <p>
         * If this value is set, {@link #serviceEndpointPrefix} and {@link #region} must also be set. If this value is
         * not set, loading the service endpoint from service metadata is skipped.
         */
        public Builder defaultProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * The region to use when resolving with {@link ServiceMetadata#of(String)}.
         * <p>
         * This value will only be used if the region is loaded from service metadata.
         * <p>
         * If this value is set, {@link #serviceEndpointPrefix} and {@link #defaultProtocol} must also be set. If this
         * value is not set, loading the service endpoint from service metadata is skipped.
         */
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        /**
         * Whether dualstack endpoints should be used when resolving with {@link ServiceMetadata#of(String)}.
         * <p>
         * This value will only be used if the endpoint is loaded from service metadata.
         * <p>
         * If this value is not set, the {@link DualstackEnabledProvider} will be used.
         */
        public Builder dualstackEnabled(Boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return this;
        }

        /**
         * Whether FIPS endpoints should be used when resolving with {@link ServiceMetadata#of(String)}.
         * <p>
         * This value will only be used if the endpoint is loaded from service metadata.
         * <p>
         * If this value is not set, the {@link FipsEnabledProvider} will be used.
         */
        public Builder fipsEnabled(Boolean fipsEnabled) {
            this.fipsEnabled = fipsEnabled;
            return this;
        }

        /**
         * Specify advanced options that should be passed on to the {@link ServiceMetadata}.
         * <p>
         * This value will only be used if the endpoint is loaded from service metadata.
         */
        public <T> Builder putAdvancedOption(ServiceMetadataAdvancedOption<T> option, T value) {
            this.advancedOptions.put(option, value);
            return this;
        }

        public AwsClientEndpointProvider build() {
            return new AwsClientEndpointProvider(this);
        }
    }
}
