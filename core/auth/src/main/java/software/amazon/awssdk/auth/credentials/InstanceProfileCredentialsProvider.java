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
import software.amazon.awssdk.auth.credentials.internal.Ec2MetadataDisableV1Resolver;
import software.amazon.awssdk.auth.credentials.internal.HttpCredentialsLoader;
import software.amazon.awssdk.auth.credentials.internal.HttpCredentialsLoader.LoadedCredentials;
import software.amazon.awssdk.auth.credentials.internal.StaticResourcesEndpointProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
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
 * An {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} that loads credentials from the current Amazon EC2
 * Instance's
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2_instance-profiles.html">instance
 * profile</a>.
 *
 * <p>
 * This is commonly used to load credentials in Amazon EC2.
 *
 * <p>
 * There are system properties, environment variables and profile properties that can control the behavior of this credential
 * provider:
 * <ul>
 *     <li>The {@code aws.disableEc2Metadata} system property or {@code AWS_EC2_METADATA_DISABLED} environment
 *     variable can be set to {@code true} to disable this credential provider.</li>
 *     <li>The {@code aws.disableEc2MetadataV1} system property, {@code AWS_EC2_METADATA_V1_DISABLED} environment
 *     variable or {@code ec2_metadata_v1_disabled} profile property can be set to {@code true} to prevent this
 *     credential provider from "falling back" to
 *     <a href="https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html">IMDSv1</a>
 *     when IMDSv2 fails to load credentials.</li>
 *     <li>The {@code aws.ec2MetadataServiceEndpoint} system property, {@code AWS_EC2_METADATA_SERVICE_ENDPOINT} environment
 *     variable or {@code ec2_metadata_service_endpoint} profile property can be set to an endpoint (including protocol) to
 *     override the default endpoint used to query the instance metadata service.</li>
 *     <li>The {@code aws.ec2MetadataServiceEndpointMode} system property, {@code AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE}
 *     environment variable or {@code ec2_metadata_service_endpoint_mode} profile property can be set to {@code IPv6} to
 *     override the default {@code IPv4} instance metadata service endpoint with the default {@code IPv6} endpoint. (This
 *     option is not used if the endpoint is overridden using another setting.)</li>
 * </ul>
 *
 * <p>
 * This uses
 * <a href="https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html">IMDSv2</a>
 * by default, but will "fall back" to IMDSv1 if IMDSv2 is not supported and IMDSv1 is not disabled using the parameters
 * described above.
 *
 * <p>
 * This credential provider caches the credential result, and will only invoke the instance metadata service
 * periodically to keep the credential "fresh". As a result, it is recommended that you create a single credentials provider of
 * this type and reuse it throughout your application. You may notice small latency increases on requests that refresh the cached
 * credentials. To avoid this latency increase, you can enable async refreshing with
 * {@link Builder#asyncCredentialUpdateEnabled(Boolean)}. If you enable this setting, you must {@link #close()} the credential
 * provider if you are done using it, to disable the background refreshing task. If you fail to do this, your application could
 * run out of resources.
 *
 * <p>
 * This credentials provider is included in the {@link DefaultCredentialsProvider}.
 *
 * <p>
 * This can be created using {@link #create()} or {@link #builder()}:
 * {@snippet :
 * InstanceProfileCredentialsProvider credentialsProvider =
 *    InstanceProfileCredentialsProvider.create(); // @link substring="create" target="#create()"
 *
 * // or
 *
 * InstanceProfileCredentialsProvider credentialsProvider =
 *     InstanceProfileCredentialsProvider.builder() // @link substring="builder" target="#builder()"
 *                                       .asyncCredentialUpdateEnabled(false)
 *                                       .build();
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 * }
 */
@SdkPublicApi
public final class InstanceProfileCredentialsProvider
    implements HttpCredentialsProvider,
               ToCopyableBuilder<InstanceProfileCredentialsProvider.Builder, InstanceProfileCredentialsProvider> {
    private static final Logger log = Logger.loggerFor(InstanceProfileCredentialsProvider.class);
    private static final String PROVIDER_NAME = "InstanceProfileCredentialsProvider";
    private static final String EC2_METADATA_TOKEN_HEADER = "x-aws-ec2-metadata-token";
    private static final String SECURITY_CREDENTIALS_RESOURCE = "/latest/meta-data/iam/security-credentials/";
    private static final String TOKEN_RESOURCE = "/latest/api/token";
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";
    private static final String DEFAULT_TOKEN_TTL = "21600";

    private final Clock clock;
    private final String endpoint;
    private final Ec2MetadataConfigProvider configProvider;
    private final Ec2MetadataDisableV1Resolver ec2MetadataDisableV1Resolver;
    private final HttpCredentialsLoader httpCredentialsLoader;
    private final CachedSupplier<AwsCredentials> credentialsCache;

    private final Boolean asyncCredentialUpdateEnabled;

    private final String asyncThreadName;

    private final Supplier<ProfileFile> profileFile;

    private final String profileName;

    private InstanceProfileCredentialsProvider(BuilderImpl builder) {
        this.clock = builder.clock;
        this.endpoint = builder.endpoint;
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.asyncThreadName = builder.asyncThreadName;
        this.profileFile = Optional.ofNullable(builder.profileFile)
                                   .orElseGet(() -> ProfileFileSupplier.fixedProfileFile(ProfileFile.defaultProfileFile()));
        this.profileName = Optional.ofNullable(builder.profileName)
                                   .orElseGet(ProfileFileSystemSetting.AWS_PROFILE::getStringValueOrThrow);

        this.httpCredentialsLoader = HttpCredentialsLoader.create(PROVIDER_NAME);
        this.configProvider =
            Ec2MetadataConfigProvider.builder()
                                     .profileFile(profileFile)
                                     .profileName(profileName)
                                     .build();
        this.ec2MetadataDisableV1Resolver = Ec2MetadataDisableV1Resolver.create(profileFile, profileName);

        if (Boolean.TRUE.equals(builder.asyncCredentialUpdateEnabled)) {
            Validate.paramNotBlank(builder.asyncThreadName, "asyncThreadName");
            this.credentialsCache = CachedSupplier.builder(this::refreshCredentials)
                                                  .cachedValueName(toString())
                                                  .prefetchStrategy(new NonBlocking(builder.asyncThreadName))
                                                  .staleValueBehavior(ALLOW)
                                                  .clock(clock)
                                                  .build();
        } else {
            this.credentialsCache = CachedSupplier.builder(this::refreshCredentials)
                                                  .cachedValueName(toString())
                                                  .staleValueBehavior(ALLOW)
                                                  .clock(clock)
                                                  .build();
        }
    }

    /**
     * Create a {@link InstanceProfileCredentialsProvider} with default configuration.
     * <p>
     * {@snippet :
     * InstanceProfileCredentialsProvider credentialsProvider = InstanceProfileCredentialsProvider.create();
     * }
     */
    public static InstanceProfileCredentialsProvider create() {
        return builder().build();
    }

    /**
     * Get a new builder for creating a {@link InstanceProfileCredentialsProvider}.
     * <p>
     * {@snippet :
     * InstanceProfileCredentialsProvider credentialsProvider =
     *     InstanceProfileCredentialsProvider.builder()
     *                                       .asyncCredentialUpdateEnabled(false)
     *                                       .build();
     * }
     */
    public static Builder builder() {
        return new BuilderImpl();
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

    /**
     * Release resources held by this credentials provider. This must be called when you're done using the credentials provider if
     * {@link Builder#asyncCredentialUpdateEnabled(Boolean)} was set to {@code true}.
     */
    @Override
    public void close() {
        credentialsCache.close();
    }

    @Override
    public String toString() {
        return ToString.create(PROVIDER_NAME);
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
            return handleTokenErrorResponse(e);
        } catch (Exception e) {
            return handleTokenErrorResponse(e);
        }
    }

    private URI getTokenEndpoint(String imdsHostname) {
        String finalHost = imdsHostname;
        if (finalHost.endsWith("/")) {
            finalHost = finalHost.substring(0, finalHost.length() - 1);
        }
        return URI.create(finalHost + TOKEN_RESOURCE);
    }

    private String handleTokenErrorResponse(Exception e) {
        if (isInsecureFallbackDisabled()) {
            String message = String.format("Failed to retrieve IMDS token, and fallback to IMDS v1 is disabled via the "
                                           + "%s system property, %s environment variable, or %s configuration file profile"
                                           + " setting.",
                                           SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.environmentVariable(),
                                           SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.property(),
                                           ProfileProperty.EC2_METADATA_V1_DISABLED);
            throw SdkClientException.builder()
                                    .message(message)
                                    .cause(e)
                                    .build();
        }
        log.debug(() -> "Ignoring non-fatal exception while attempting to load metadata token from instance profile.", e);
        return null;
    }

    private boolean isInsecureFallbackDisabled() {
        return ec2MetadataDisableV1Resolver.resolve();
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
     * See {@link InstanceProfileCredentialsProvider} for detailed documentation.
     */
    public interface Builder extends HttpCredentialsProvider.Builder<InstanceProfileCredentialsProvider, Builder>,
                                     CopyableBuilder<Builder, InstanceProfileCredentialsProvider> {
        /**
         * Configure the profile file used for loading IMDS-related configuration, like the endpoint mode (IPv4 vs IPv6).
         *
         * <p>
         * The profile file is only read when the {@link ProfileFile} object is created, so the credentials provider will not
         * reflect any changes made in the provided file. To automatically adjust to changes in the file, see
         * {@link #profileFile(Supplier)}.
         *
         * <p>
         * If not specified, the {@link ProfileFile#defaultProfileFile()} will be used.
         *
         * <p>
         * {@snippet :
         * InstanceProfileCredentialsProvider.builder()
         *                                   .profileFile(ProfileFile.builder()
         *                                                           .type(ProfileFile.Type.CONFIGURATION)
         *                                                           .content(Paths.get("~/.aws/config"))
         *                                                           .build())
         *                                   .build()
         *}
         *
         * @see ProfileFile
         */
        Builder profileFile(ProfileFile profileFile);

        /**
         * Configure the {@link ProfileFileSupplier} used for loading IMDS-related configuration, like the endpoint mode (IPv4 vs
         * IPv6).
         *
         * <p>
         * The profile file supplier is called each time the {@link ProfileFile} is read, so the credentials provider can
         * "pick up" changes made in the provided file.
         *
         * <p>
         * If not specified, the (fixed) {@link ProfileFile#defaultProfileFile()} will be used.
         *
         * <p>
         * {@snippet :
         * InstanceProfileCredentialsProvider.builder()
         *                                   .profileFile(ProfileFileSupplier.defaultSupplier())
         *                                   .build()
         *}
         *
         * @see ProfileFileSupplier
         */
        Builder profileFile(Supplier<ProfileFile> profileFileSupplier);

        /**
         * Configure the profile name used for loading IMDS-related configuration, like the endpoint mode (IPv4 vs IPv6).
         *
         * <p>
         * If not specified, the {@code aws.profile} system property or {@code AWS_PROFILE} environment variable's value will
         * be used. If these are not set, then {@code default} will be used.
         *
         * <p>
         * {@snippet :
         * InstanceProfileCredentialsProvider.builder()
         *                                   .profileName("custom-profile-name")
         *                                   .build()
         *}
         */
        Builder profileName(String profileName);

        /**
         * Build the {@link InstanceProfileCredentialsProvider}.
         *
         * <p>
         * {@snippet :
         * InstanceProfileCredentialsProvider credentialsProvider =
         *     InstanceProfileCredentialsProvider.builder()
         *                                       .asyncCredentialUpdateEnabled(false)
         *                                       .build();
         * }
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
