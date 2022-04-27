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
import static java.time.temporal.ChronoUnit.SECONDS;
import static software.amazon.awssdk.utils.ComparableUtils.minimum;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
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
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.util.HttpResourcesUtils;
import software.amazon.awssdk.regions.util.ResourcesEndpointProvider;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
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
public final class InstanceProfileCredentialsProvider implements HttpCredentialsProvider {
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

    private volatile LoadedCredentials cachedCredentials;

    /**
     * @see #builder()
     */
    private InstanceProfileCredentialsProvider(BuilderImpl builder) {
        this.clock = builder.clock;
        this.endpoint = builder.endpoint;
        this.httpCredentialsLoader = HttpCredentialsLoader.create();
        this.configProvider =
            Ec2MetadataConfigProvider.builder()
                                     .profileFile(builder.profileFile == null ? null : () -> builder.profileFile)
                                     .profileName(builder.profileName == null ? null : builder.profileName)
                                     .build();

        if (Boolean.TRUE.equals(builder.asyncCredentialUpdateEnabled)) {
            Validate.paramNotBlank(builder.asyncThreadName, "asyncThreadName");
            this.credentialsCache = CachedSupplier.builder(this::refreshCredentials)
                                                  .prefetchStrategy(new NonBlocking(builder.asyncThreadName))
                                                  .build();
        } else {
            this.credentialsCache = CachedSupplier.builder(this::refreshCredentials).build();
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

        LoadedCredentials credentials;
        try {
            credentials = httpCredentialsLoader.loadCredentials(createEndpointProvider());
            logExpirationTime(credentials);
            this.cachedCredentials = credentials;
        } catch (RuntimeException | IOException e) {
            credentials = this.cachedCredentials;

            if (credentials != null) {
                credentials.getExpiration().ifPresent(expiration -> {
                    // Choose whether to report this failure at the debug or warn level based on how much time is left on the
                    // credentials before expiration.
                    Supplier<String> errorMessage = () -> "Failure encountered when attempting to refresh credentials from IMDS.";
                    Instant fifteenMinutesFromNow = Instant.now().plus(15, MINUTES);
                    if (expiration.isBefore(fifteenMinutesFromNow)) {
                        log.warn(errorMessage, e);
                    } else {
                        log.debug(errorMessage, e);
                    }
                });
            } else {
                throw SdkClientException.create("Failed to load credentials from IMDS.", e);
            }
        }

        return RefreshResult.builder(credentials.getAwsCredentials())
                            .staleTime(null) // Allow use of expired credentials - they may still work
                            .prefetchTime(prefetchTime(credentials.getExpiration().orElse(null)))
                            .build();
    }

    private void logExpirationTime(LoadedCredentials credentials) {
        log.debug(() -> "Loaded credentials from IMDS with expiration time of " + credentials.getExpiration());
    }

    private boolean isLocalCredentialLoadingDisabled() {
        return SdkSystemSetting.AWS_EC2_METADATA_DISABLED.getBooleanValueOrThrow();
    }

    private Instant prefetchTime(Instant expiration) {
        Instant now = clock.instant();

        // If expiration time doesn't exist, refresh in 60 minutes
        if (expiration == null) {
            return now.plus(60, MINUTES);
        }

        // If expiration time is 60+ minutes from now, refresh in 30 minutes.
        Instant sixtyMinutesBeforeExpiration = expiration.minus(60, MINUTES);
        if (now.isBefore(sixtyMinutesBeforeExpiration)) {
            return now.plus(30, MINUTES);
        }

        // If expiration time is 15 minutes or more from now, refresh in 10 minutes.
        Instant fifteenMinutesBeforeExpiration = expiration.minus(15, MINUTES);
        if (now.isBefore(fifteenMinutesBeforeExpiration)) {
            return now.plus(10, MINUTES);
        }

        // If expiration time is 0.25-15 minutes from now, refresh in 5 minutes, or 15 seconds before expiration, whichever is
        // sooner.
        Instant fifteenSecondsBeforeExpiration = expiration.minus(15, SECONDS);
        if (now.isBefore(fifteenSecondsBeforeExpiration)) {
            return minimum(now.plus(5, MINUTES), fifteenSecondsBeforeExpiration);
        }

        // These credentials are expired. Try refreshing again in 5 minutes. We can't be more aggressive than that, because we
        // don't want to overload the IMDS endpoint.
        log.warn(() -> "IMDS credential expiration has been extended due to an IMDS availability outage. A refresh "
                       + "of these credentials will be attempted again in 5 minutes.");
        return now.plus(5, MINUTES);
    }

    @Override
    public void close() {
        credentialsCache.close();
    }

    @Override
    public String toString() {
        return ToString.create("InstanceProfileCredentialsProvider");
    }

    private ResourcesEndpointProvider createEndpointProvider() throws IOException {
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

    private String[] getSecurityCredentials(String imdsHostname, String metadataToken) throws IOException {
        ResourcesEndpointProvider securityCredentialsEndpoint =
            new StaticResourcesEndpointProvider(URI.create(imdsHostname + SECURITY_CREDENTIALS_RESOURCE),
                                                getTokenHeaders(metadataToken));

        String securityCredentialsList = HttpResourcesUtils.instance().readResource(securityCredentialsEndpoint);
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

    /**
     * A builder for creating a custom a {@link InstanceProfileCredentialsProvider}.
     */
    public interface Builder extends HttpCredentialsProvider.Builder<InstanceProfileCredentialsProvider, Builder> {
        /**
         * Configure the profile file used for loading IMDS-related configuration, like the endpoint mode (IPv4 vs IPv6).
         *
         * <p>By default, {@link ProfileFile#defaultProfileFile()} is used.
         */
        Builder profileFile(ProfileFile profileFile);

        /**
         * Configure the profile name used for loading IMDS-related configuration, like the endpoint mode (IPv4 vs IPv6).
         *
         * <p>By default, {@link ProfileFileSystemSetting#AWS_PROFILE} is used.
         */
        Builder profileName(String profileName);

        /**
         * Build a {@link InstanceProfileCredentialsProvider} from the provided configuration.
         */
        InstanceProfileCredentialsProvider build();
    }

    @SdkTestInternalApi
    static final class BuilderImpl implements Builder {
        private Clock clock = Clock.systemUTC();
        private String endpoint;
        private Boolean asyncCredentialUpdateEnabled;
        private String asyncThreadName;
        private ProfileFile profileFile;
        private String profileName;

        private BuilderImpl() {
            asyncThreadName("instance-profile-credentials-provider");
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
            this.profileFile = profileFile;
            return this;
        }

        public void setProfileFile(ProfileFile profileFile) {
            profileFile(profileFile);
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
