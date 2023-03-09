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

package software.amazon.awssdk.auth.credentials;

import static java.time.temporal.ChronoUnit.MINUTES;
import static software.amazon.awssdk.utils.ComparableUtils.maximum;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.cache.CachedSupplier.StaleValueBehavior.ALLOW;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.internal.Ec2MetadataConfigProvider;
import software.amazon.awssdk.auth.credentials.internal.HttpCredentialsLoader;
import software.amazon.awssdk.auth.credentials.internal.HttpCredentialsLoader.LoadedCredentials;
import software.amazon.awssdk.auth.credentials.internal.StaticResourcesEndpointProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.util.HttpResourcesUtils;
import software.amazon.awssdk.regions.util.ResourcesEndpointProvider;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * Credentials provider implementation that loads credentials from the Amazon EC2 Instance Metadata Service.
 *
 * <P>
 * If {@link SdkSystemSetting#AWS_EC2_METADATA_DISABLED} is set to true, it will not try to load
 * credentials from EC2 metadata service and will return null.
 */
@SdkPublicApi
public final class InstanceProfileCredentialsProvider
    implements HttpCredentialsProvider,
               ToCopyableBuilder<InstanceProfileCredentialsProvider.Builder, InstanceProfileCredentialsProvider> {
    private static final Logger log = Logger.loggerFor(InstanceProfileCredentialsProvider.class);
    private static final String EC2_METADATA_TOKEN_HEADER = "x-aws-ec2-metadata-token";

    private static final String SECURITY_CREDENTIALS_RESOURCE = "/latest/meta-data/iam/security-credentials/";
    private static final String TOKEN_RESOURCE = "/latest/api/token";
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";
    private static final String DEFAULT_TOKEN_TTL = "21600";

    private final Clock clock;
    private final String endpoint;
    private final Ec2MetadataConfigProvider configProvider;
    private final HttpCredentialsLoader httpCredentialsLoader;
    private final CachedSupplier<AwsCredentials> credentialsCache;

    private final Boolean asyncCredentialUpdateEnabled;

    private final String asyncThreadName;

    private final Supplier<ProfileFile> profileFile;

    private final String profileName;

    /**
     * @see #builder()
     */
    private InstanceProfileCredentialsProvider(BuilderImpl builder) {
        this.clock = builder.clock;
        this.endpoint = builder.endpoint;
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.asyncThreadName = builder.asyncThreadName;
        this.profileFile = builder.profileFile;
        this.profileName = builder.profileName;

        this.httpCredentialsLoader = HttpCredentialsLoader.create();
        this.configProvider =
            Ec2MetadataConfigProvider.builder()
                                     .profileFile(builder.profileFile)
                                     .profileName(builder.profileName)
                                     .build();

        if (Boolean.TRUE.equals(builder.asyncCredentialUpdateEnabled)) {
            Validate.paramNotBlank(builder.asyncThreadName, "asyncThreadName");
            this.credentialsCache = CachedSupplier.builder(this::refreshCredentials)
                                                  .prefetchStrategy(new NonBlocking(builder.asyncThreadName))
                                                  .staleValueBehavior(ALLOW)
                                                  .clock(clock)
                                                  .build();
        } else {
            this.credentialsCache = CachedSupplier.builder(this::refreshCredentials)
                                                  .staleValueBehavior(ALLOW)
                                                  .clock(clock)
                                                  .build();
        }
    }

    /**
     * Create a builder for creating a {@link InstanceProfileCredentialsProvider}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Create a {@link InstanceProfileCredentialsProvider} with default values.
     *
     * @return a {@link InstanceProfileCredentialsProvider}
     */
    public static InstanceProfileCredentialsProvider create() {
        return builder().build();
    }


    @Override
    public AwsCredentials resolveCredentials() {
        return credentialsCache.get();
    }

    private RefreshResult<AwsCredentials> refreshCredentials() {
        if (isLocalCredentialLoadingDisabled()) {
            throw SdkClientException.create("IMDS credentials have been disabled by environment variable or system property.");
        }

        try {
            LoadedCredentials credentials = httpCredentialsLoader.loadCredentials(createEndpointProvider());
            Instant expiration = credentials.getExpiration().orElse(null);
            log.debug(() -> "Loaded credentials from IMDS with expiration time of " + expiration);

            return RefreshResult.builder(credentials.getAwsCredentials())
                                .staleTime(staleTime(expiration))
                                .prefetchTime(prefetchTime(expiration))
                                .build();
        } catch (RuntimeException e) {
            throw SdkClientException.create("Failed to load credentials from IMDS.", e);
        }
    }

    private boolean isLocalCredentialLoadingDisabled() {
        return SdkSystemSetting.AWS_EC2_METADATA_DISABLED.getBooleanValueOrThrow();
    }

    private Instant staleTime(Instant expiration) {
        if (expiration == null) {
            return null;
        }

        return expiration.minusSeconds(1);
    }

    private Instant prefetchTime(Instant expiration) {
        Instant now = clock.instant();

        if (expiration == null) {
            return now.plus(60, MINUTES);
        }

        Duration timeUntilExpiration = Duration.between(now, expiration);
        if (timeUntilExpiration.isNegative()) {
            // IMDS gave us a time in the past. We're already stale. Don't prefetch.
            return null;
        }

        return now.plus(maximum(timeUntilExpiration.dividedBy(2), Duration.ofMinutes(5)));
    }

    @Override
    public void close() {
        credentialsCache.close();
    }

    @Override
    public String toString() {
        return ToString.create("InstanceProfileCredentialsProvider");
    }

    private ResourcesEndpointProvider createEndpointProvider() {
        String imdsHostname = getImdsEndpoint();
        String token = getToken(imdsHostname);
        String[] securityCredentials = getSecurityCredentials(imdsHostname, token);

        return new StaticResourcesEndpointProvider(URI.create(imdsHostname + SECURITY_CREDENTIALS_RESOURCE +
                                                              securityCredentials[0]),
                                                   getTokenHeaders(token));
    }

    private String getImdsEndpoint() {
        if (endpoint != null) {
            return endpoint;
        }

        return configProvider.getEndpoint();
    }

    private String getToken(String imdsHostname) {
        Map<String, String> tokenTtlHeaders = Collections.singletonMap(EC2_METADATA_TOKEN_TTL_HEADER, DEFAULT_TOKEN_TTL);
        ResourcesEndpointProvider tokenEndpoint = new StaticResourcesEndpointProvider(getTokenEndpoint(imdsHostname),
                                                                                      tokenTtlHeaders);

        try {
            return HttpResourcesUtils.instance().readResource(tokenEndpoint, "PUT");
        } catch (SdkServiceException e) {
            if (e.statusCode() == 400) {
                throw SdkClientException.builder()
                                        .message("Unable to fetch metadata token.")
                                        .cause(e)
                                        .build();
            }

            log.debug(() -> "Ignoring non-fatal exception while attempting to load metadata token from instance profile.", e);
            return null;
        } catch (Exception e) {
            log.debug(() -> "Ignoring non-fatal exception while attempting to load metadata token from instance profile.", e);
            return null;
        }
    }

    private URI getTokenEndpoint(String imdsHostname) {
        String finalHost = imdsHostname;
        if (finalHost.endsWith("/")) {
            finalHost = finalHost.substring(0, finalHost.length() - 1);
        }
        return URI.create(finalHost + TOKEN_RESOURCE);
    }

    private String[] getSecurityCredentials(String imdsHostname, String metadataToken) {
        ResourcesEndpointProvider securityCredentialsEndpoint =
            new StaticResourcesEndpointProvider(URI.create(imdsHostname + SECURITY_CREDENTIALS_RESOURCE),
                                                getTokenHeaders(metadataToken));

        String securityCredentialsList =
            invokeSafely(() -> HttpResourcesUtils.instance().readResource(securityCredentialsEndpoint));
        String[] securityCredentials = securityCredentialsList.trim().split("\n");

        if (securityCredentials.length == 0) {
            throw SdkClientException.builder().message("Unable to load credentials path").build();
        }
        return securityCredentials;
    }

    private Map<String, String> getTokenHeaders(String metadataToken) {
        if (metadataToken == null) {
            return Collections.emptyMap();
        }

        return Collections.singletonMap(EC2_METADATA_TOKEN_HEADER, metadataToken);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * A builder for creating a custom a {@link InstanceProfileCredentialsProvider}.
     */
    public interface Builder extends HttpCredentialsProvider.Builder<InstanceProfileCredentialsProvider, Builder>,
                                     CopyableBuilder<Builder, InstanceProfileCredentialsProvider> {
        /**
         * Configure the profile file used for loading IMDS-related configuration, like the endpoint mode (IPv4 vs IPv6).
         *
         * <p>By default, {@link ProfileFile#defaultProfileFile()} is used.
         * 
         * @see #profileFile(Supplier)
         */
        Builder profileFile(ProfileFile profileFile);

        /**
         * Define the mechanism for loading profile files.
         *
         * @param profileFileSupplier Supplier interface for generating a ProfileFile instance.
         * @see #profileFile(ProfileFile)
         */
        Builder profileFile(Supplier<ProfileFile> profileFileSupplier);

        /**
         * Configure the profile name used for loading IMDS-related configuration, like the endpoint mode (IPv4 vs IPv6).
         *
         * <p>By default, {@link ProfileFileSystemSetting#AWS_PROFILE} is used.
         */
        Builder profileName(String profileName);

        /**
         * Build a {@link InstanceProfileCredentialsProvider} from the provided configuration.
         */
        @Override
        InstanceProfileCredentialsProvider build();
    }

    @SdkTestInternalApi
    static final class BuilderImpl implements Builder {
        private Clock clock = Clock.systemUTC();
        private String endpoint;
        private Boolean asyncCredentialUpdateEnabled;
        private String asyncThreadName;
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        private BuilderImpl() {
            asyncThreadName("instance-profile-credentials-provider");
        }

        private BuilderImpl(InstanceProfileCredentialsProvider provider) {
            this.clock = provider.clock;
            this.endpoint = provider.endpoint;
            this.asyncCredentialUpdateEnabled = provider.asyncCredentialUpdateEnabled;
            this.asyncThreadName = provider.asyncThreadName;
            this.profileFile = provider.profileFile;
            this.profileName = provider.profileName;
        }

        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        @Override
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public void setEndpoint(String endpoint) {
            endpoint(endpoint);
        }

        @Override
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return this;
        }

        public void setAsyncCredentialUpdateEnabled(boolean asyncCredentialUpdateEnabled) {
            asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled);
        }

        @Override
        public Builder asyncThreadName(String asyncThreadName) {
            this.asyncThreadName = asyncThreadName;
            return this;
        }

        public void setAsyncThreadName(String asyncThreadName) {
            asyncThreadName(asyncThreadName);
        }

        @Override
        public Builder profileFile(ProfileFile profileFile) {
            return profileFile(Optional.ofNullable(profileFile)
                                       .map(ProfileFileSupplier::fixedProfileFile)
                                       .orElse(null));
        }

        public void setProfileFile(ProfileFile profileFile) {
            profileFile(profileFile);
        }

        @Override
        public Builder profileFile(Supplier<ProfileFile> profileFileSupplier) {
            this.profileFile = profileFileSupplier;
            return this;
        }

        public void setProfileFile(Supplier<ProfileFile> profileFileSupplier) {
            profileFile(profileFileSupplier);
        }

        @Override
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public void setProfileName(String profileName) {
            profileName(profileName);
        }

        @Override
        public InstanceProfileCredentialsProvider build() {
            return new InstanceProfileCredentialsProvider(this);
        }
    }
}
