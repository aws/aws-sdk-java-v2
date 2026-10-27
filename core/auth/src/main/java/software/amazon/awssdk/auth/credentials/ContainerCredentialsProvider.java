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

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.awssdk.utils.ComparableUtils.maximum;
import static software.amazon.awssdk.utils.ComparableUtils.minimum;
import static software.amazon.awssdk.utils.cache.CachedSupplier.StaleValueBehavior.ALLOW;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.internal.ContainerCredentialsRetryPolicy;
import software.amazon.awssdk.auth.credentials.internal.HttpCredentialsLoader;
import software.amazon.awssdk.auth.credentials.internal.HttpCredentialsLoader.LoadedCredentials;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.regions.util.ResourcesEndpointProvider;
import software.amazon.awssdk.regions.util.ResourcesEndpointRetryPolicy;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.CacheRefreshUtils;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * {@link AwsCredentialsProvider} implementation that loads credentials from a local metadata service.
 *
 * Currently supported containers:
 * <ul>
 *     <li>Amazon Elastic Container Service (ECS)</li>
 *     <li>AWS Greengrass</li>
 * </ul>
 *
 * <p>The URI path is retrieved from the environment variable "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" or
 * "AWS_CONTAINER_CREDENTIALS_FULL_URI" in the container's environment. If the environment variable is not set, this credentials
 * provider will throw an exception.</p>
 *
 * @see <a href="http://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html">Amazon Elastic Container
 * Service (ECS)</a>
 */
@SdkPublicApi
public final class ContainerCredentialsProvider
    implements HttpCredentialsProvider,
               ToCopyableBuilder<ContainerCredentialsProvider.Builder, ContainerCredentialsProvider> {
    private static final String CLASS_NAME = "ContainerCredentialsProvider";
    private static final String PROVIDER_NAME = BusinessMetricFeatureId.CREDENTIALS_HTTP.value();
    private static final Predicate<InetAddress> IS_LOOPBACK_ADDRESS = InetAddress::isLoopbackAddress;
    private static final Predicate<InetAddress> ALLOWED_HOSTS_RULES = IS_LOOPBACK_ADDRESS;
    private static final String HTTPS = "https";

    private static final String ECS_CONTAINER_HOST = "169.254.170.2";
    private static final String EKS_CONTAINER_HOST_IPV6 = "[fd00:ec2::23]";
    private static final String EKS_CONTAINER_HOST_IPV4 = "169.254.170.23";
    private static final List<String> VALID_LOOP_BACK_IPV4 = Arrays.asList(ECS_CONTAINER_HOST, EKS_CONTAINER_HOST_IPV4);
    private static final List<String> VALID_LOOP_BACK_IPV6 = Arrays.asList(EKS_CONTAINER_HOST_IPV6);

    private static final Duration DEFAULT_STALE_TIME = Duration.ofMinutes(1);

    // Bounds of the default prefetch time. See defaultPrefetchTime.
    private static final Duration DEFAULT_PREFETCH_LATEST_AFTER_FETCH = Duration.ofHours(1);
    private static final Duration DEFAULT_PREFETCH_BEFORE_EXPIRATION = Duration.ofMinutes(15);
    private static final Duration DEFAULT_PREFETCH_NO_EXPIRATION = Duration.ofMinutes(60);
    private static final Duration DEFAULT_PREFETCH_NO_EXPIRATION_JITTER = Duration.ofMinutes(5);

    private final DoubleSupplier prefetchJitterRandom;
    private final String endpoint;
    private final HttpCredentialsLoader httpCredentialsLoader;
    private final CachedSupplier<AwsCredentials> credentialsCache;

    private final Boolean asyncCredentialUpdateEnabled;

    private final String asyncThreadName;
    private final String sourceChain;
    private final String providerName;
    private final Duration staleTime;
    private final Duration prefetchTime;

    /**
     * @see #builder()
     */
    private ContainerCredentialsProvider(BuilderImpl builder) {
        this.prefetchJitterRandom = Validate.getOrDefault(builder.prefetchJitterRandom, () -> new Random()::nextDouble);
        this.endpoint = builder.endpoint;
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.asyncThreadName = builder.asyncThreadName;
        this.sourceChain = builder.sourceChain;
        this.providerName = StringUtils.isEmpty(builder.sourceChain)
            ? PROVIDER_NAME 
            : builder.sourceChain + "," + PROVIDER_NAME;
        this.httpCredentialsLoader = HttpCredentialsLoader.create(this.providerName);
        this.staleTime = Optional.ofNullable(builder.staleTime).orElse(DEFAULT_STALE_TIME);
        this.prefetchTime = builder.prefetchTime;
        if (this.prefetchTime != null) {
            Validate.isTrue(this.staleTime.compareTo(this.prefetchTime) <= 0,
                            "staleTime (%s) must be less than or equal to prefetchTime (%s).", this.staleTime, this.prefetchTime);
        }

        // CachedSupplier's prefetch jitter is disabled. A configured advisory refresh window is a fixed distance from
        // expiration and must be honored exactly. The default prefetch time is already jittered by defaultPrefetchTime,
        // within bounds that CachedSupplier's jitter would not respect.
        if (Boolean.TRUE.equals(builder.asyncCredentialUpdateEnabled)) {
            Validate.paramNotBlank(builder.asyncThreadName, "asyncThreadName");
            this.credentialsCache = CachedSupplier.builder(this::refreshCredentials)
                                                  .cachedValueName(toString())
                                                  .prefetchStrategy(new NonBlocking(builder.asyncThreadName))
                                                  .staleValueBehavior(ALLOW)
                                                  .prefetchJitterEnabled(false)
                                                  .build();
        } else {
            this.credentialsCache = CachedSupplier.builder(this::refreshCredentials)
                                                  .cachedValueName(toString())
                                                  .staleValueBehavior(ALLOW)
                                                  .prefetchJitterEnabled(false)
                                                  .build();
        }
    }

    public static ContainerCredentialsProvider create() {
        return builder().build();
    }

    /**
     * Create a builder for creating a {@link ContainerCredentialsProvider}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public String toString() {
        return ToString.create(CLASS_NAME);
    }

    private RefreshResult<AwsCredentials> refreshCredentials() {
        LoadedCredentials loadedCredentials =
            httpCredentialsLoader.loadCredentials(new ContainerCredentialsEndpointProvider(endpoint));
        Instant expiration = loadedCredentials.getExpiration().orElse(null);

        return RefreshResult.builder(loadedCredentials.getAwsCredentials())
                            .staleTime(staleTime(expiration))
                            .prefetchTime(prefetchTime(expiration))
                            .expiration(expiration)
                            .build();
    }

    private Instant staleTime(Instant expiration) {
        if (expiration == null) {
            return null;
        }

        return expiration.minus(staleTime);
    }

    private Instant prefetchTime(Instant expiration) {
        Instant now = Instant.now();

        if (expiration == null) {
            return noExpirationPrefetchTime(now, prefetchJitterRandom.getAsDouble());
        }

        if (prefetchTime != null) {
            return expiration.minus(prefetchTime);
        }

        if (!expiration.isAfter(now)) {
            // The credentials have already expired, so this is in the past and the next call refreshes them.
            return expiration.minus(DEFAULT_PREFETCH_BEFORE_EXPIRATION);
        }

        return defaultPrefetchTime(now, expiration, prefetchJitterRandom.getAsDouble());
    }

    /**
     * Compute the default prefetch (advisory refresh) time for credentials that expire after {@code now}, when no prefetch
     * time is configured. With {@code L} the remaining lifetime and {@code W} the advisory refresh window that
     * {@link CacheRefreshUtils#computePrefetchWindow} selects for {@code L}, the refresh happens at a uniformly random time
     * between {@code lo} and {@code hi} after the fetch:
     * <pre>
     * hi = L - W
     * lo = min(max(min(1 hour, L - 15 min), 0), 3/4 * hi)
     * </pre>
     * For the common 6 hour credentials, that is 1 to 5 hours after the fetch.
     *
     * <p>The lower bound is the provider's own schedule: 15 minutes before expiration, but no later than 1 hour after the
     * fetch. The refresh is never later than {@code L - W}, the point at which the credential cache specification starts the
     * advisory refresh. A provider may keep its own schedule only when that schedule refreshes earlier. The lower bound is
     * capped so that at least a quarter of the range is left for the jitter, which spreads out the refreshes of multiple
     * providers on one host.
     *
     * <p>If {@code L - W} is zero or negative, the credentials are already inside their advisory refresh window when they are
     * fetched. The refresh then happens on the next call, as the credential cache specification specifies, and the result is
     * {@code now}. The mandatory refresh at the stale time still applies.
     *
     * @param now the time of the fetch
     * @param expiration the expiration of the fetched credentials, after {@code now}
     * @param randomFraction where to place the refresh between the lower bound (0) and the upper bound (1)
     */
    static Instant defaultPrefetchTime(Instant now, Instant expiration, double randomFraction) {
        Duration lifetime = Duration.between(now, expiration);
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, now);

        Duration latest = lifetime.minus(window);
        if (latest.isNegative() || latest.isZero()) {
            return now;
        }

        Duration providerSchedule = maximum(minimum(DEFAULT_PREFETCH_LATEST_AFTER_FETCH,
                                                    lifetime.minus(DEFAULT_PREFETCH_BEFORE_EXPIRATION)),
                                            Duration.ZERO);
        Duration earliest = minimum(providerSchedule, latest.minus(latest.dividedBy(4)));

        Duration prefetch = earliest.plusMillis(jitterMillis(latest.minus(earliest), randomFraction));
        return now.plus(prefetch);
    }

    /**
     * Compute the prefetch time for credentials without an expiration: a uniformly random time 60 to 65 minutes after the
     * fetch. This is the schedule that CachedSupplier's prefetch jitter produced before the provider disabled it.
     *
     * @param now the time of the fetch
     * @param randomFraction where to place the refresh between 60 minutes (0) and 65 minutes (1) after the fetch
     */
    static Instant noExpirationPrefetchTime(Instant now, double randomFraction) {
        return now.plus(DEFAULT_PREFETCH_NO_EXPIRATION)
                  .plusMillis(jitterMillis(DEFAULT_PREFETCH_NO_EXPIRATION_JITTER, randomFraction));
    }

    private static long jitterMillis(Duration range, double randomFraction) {
        return (long) (range.toMillis() * randomFraction);
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return credentialsCache.get();
    }

    @Override
    public CompletableFuture<Void> invalidate(AwsCredentialsIdentity identity) {
        String rejectedAccessKeyId = identity.accessKeyId();
        credentialsCache.invalidate(cachedCreds -> rejectedAccessKeyId.equals(cachedCreds.accessKeyId()));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
        credentialsCache.close();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    static final class ContainerCredentialsEndpointProvider implements ResourcesEndpointProvider {
        private final String endpoint;

        ContainerCredentialsEndpointProvider(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public URI endpoint() throws IOException {
            if (!SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValue().isPresent() &&
                !SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.getStringValue().isPresent()) {
                throw SdkClientException.builder()
                        .message(String.format("Cannot fetch credentials from container - neither %s or %s " +
                                 "environment variables are set.",
                                 SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(),
                                 SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.environmentVariable()))
                        .build();
            }

            try {
                URI resolvedURI = SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI
                    .getStringValue()
                    .map(this::createUri)
                    .orElseGet(() -> URI.create(SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.getStringValueOrThrow()));
                validateURI(resolvedURI);
                return resolvedURI;
            } catch (SdkClientException e) {
                throw e;
            } catch (Exception e) {
                throw SdkClientException.builder()
                                        .message("Unable to fetch credentials from container.")
                                        .cause(e)
                                        .build();
            }
        }

        @Override
        public ResourcesEndpointRetryPolicy retryPolicy() {
            return new ContainerCredentialsRetryPolicy();
        }

        @Override
        public Map<String, String> headers() {
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("User-Agent", SdkUserAgent.create().userAgent());
            getTokenValue()
                                                              .filter(StringUtils::isNotBlank)
                                                              .ifPresent(t -> requestHeaders.put("Authorization", t));
            return requestHeaders;
        }

        private Optional<String> getTokenValue() {
            if (SdkSystemSetting
                .AWS_CONTAINER_AUTHORIZATION_TOKEN.getStringValue().isPresent()) {
                return SdkSystemSetting
                    .AWS_CONTAINER_AUTHORIZATION_TOKEN
                    .getStringValue();
            }


            return SdkSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE.getStringValue()
                                                                          .map(this::readToken);

        }

        private String readToken(String filePath) {
            Path path = Paths.get(filePath);
            try {
                return new String(Files.readAllBytes(path), UTF_8);
            } catch (IOException e) {
                throw SdkClientException.create(String.format("Failed to read %s.", path.toAbsolutePath()), e);
            }
        }

        private URI createUri(String relativeUri) {
            String host = endpoint != null ? endpoint : SdkSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.getStringValueOrThrow();
            return URI.create(host + relativeUri);
        }

        private URI validateURI(URI uri) {
            if (!isHttps(uri) && !isAllowedHost(uri.getHost())) {
                String envVarName = SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable();
                throw SdkClientException.builder()
                                        .message(String.format("The full URI (%s) contained within environment variable " +
                                                               "%s has an invalid host. Host should resolve to a loopback " +
                                                               "address or have the full URI be HTTPS.",
                                                               uri, envVarName))
                                        .build();
            }
            return uri;
        }

        private boolean isHttps(URI endpoint) {
            return Objects.equals(HTTPS, endpoint.getScheme());
        }

        /**
         * Determines if the addresses for a given host are resolved to a loopback address.
         * <p>
         *     This is a best-effort in determining what address a host will be resolved to. DNS caching might be disabled,
         *     or could expire between this check and when the API is invoked.
         * </p>
         * @param host The name or IP address of the host.
         * @return A boolean specifying whether the host is allowed as an endpoint for credentials loading.
         */
        private boolean isAllowedHost(String host) {
            try {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                return addresses.length > 0 && (
                    Arrays.stream(addresses).allMatch(this::matchesAllowedHostRules)
                       || isMetadataServiceEndpoint(host));
            } catch (UnknownHostException e) {
                throw SdkClientException.builder()
                                        .cause(e)
                                        .message(String.format("host (%s) could not be resolved to an IP address.", host))
                                        .build();
            }
        }

        private boolean matchesAllowedHostRules(InetAddress inetAddress) {
            return ALLOWED_HOSTS_RULES.test(inetAddress) ;
        }

        public boolean isMetadataServiceEndpoint(String host) {
            return VALID_LOOP_BACK_IPV4.contains(host) || VALID_LOOP_BACK_IPV6.contains(host);
        }
    }

    /**
     * A builder for creating a custom a {@link ContainerCredentialsProvider}.
     */
    public interface Builder extends HttpCredentialsProvider.Builder<ContainerCredentialsProvider, Builder>,
                                     CopyableBuilder<Builder, ContainerCredentialsProvider> {

        /**
         * Configure the amount of time, relative to credential expiration, that defines the mandatory refresh window. When
         * the cached credentials are within this window (i.e., their remaining lifetime is less than this duration), the
         * provider will block all callers until a refresh attempt completes. If the refresh attempt fails, the provider
         * returns the cached credentials and will not attempt another refresh until a backoff period has elapsed.
         *
         * <p>This value must be less than or equal to {@link #prefetchTime(Duration)}, when that is set. Setting this equal to
         * {@code prefetchTime} effectively disables prefetch, causing all refreshes to be mandatory (blocking).
         *
         * <p>By default, this is 1 minute.
         *
         * @param staleTime the duration before expiration that triggers mandatory (blocking) refresh
         */
        Builder staleTime(Duration staleTime);

        /**
         * Configure the amount of time, relative to credential expiration, that defines the advisory refresh window. When
         * the cached credentials are within this window (i.e., their remaining lifetime is less than this duration), the
         * provider will attempt to refresh them proactively. If the refresh fails, the provider returns the existing cached
         * credentials without error and will not attempt another refresh until a backoff period has elapsed.
         *
         * <p>When {@link #asyncCredentialUpdateEnabled(Boolean)} is true, advisory refreshes happen in a background thread
         * and callers immediately receive the current cached credentials. When it is false, one caller will block to perform
         * the refresh while other callers receive the current cached credentials.
         *
         * <p>This value must be greater than or equal to {@link #staleTime(Duration)}. Setting this equal to
         * {@code staleTime} effectively disables prefetch, causing all refreshes to be mandatory (blocking). When set, the
         * advisory refresh starts exactly this long before expiration, without jitter.
         *
         * <p>If not explicitly set, the provider proactively refreshes credentials at a random time, so that many clients do
         * not refresh at the same moment. The earliest refresh is 15 minutes before the credentials expire, or 1 hour after
         * they were retrieved if that is sooner. The refresh happens no later than the start of the advisory refresh window
         * that the SDK computes from the remaining lifetime of the credentials: 60 minutes before expiration for credentials
         * with 90 minutes or more remaining, 15 minutes before expiration for credentials with more than 20 minutes remaining,
         * and 5 minutes before expiration otherwise. For 6-hour credentials, the refresh is 1 to 5 hours after retrieval. When
         * the earliest refresh is close to that limit, it moves earlier to keep a range for the random time. Credentials that
         * are already inside that window when they are retrieved are refreshed on the next call. If the endpoint does not
         * return an expiration, the credentials are refreshed at a random time 60 to 65 minutes after they were retrieved.
         *
         * @param prefetchTime the duration before expiration that triggers advisory (proactive) refresh
         */
        Builder prefetchTime(Duration prefetchTime);
    }

    @SdkTestInternalApi
    static final class BuilderImpl implements Builder {
        private DoubleSupplier prefetchJitterRandom;
        private String endpoint;
        private Boolean asyncCredentialUpdateEnabled;
        private String asyncThreadName;
        private String sourceChain;
        private Duration staleTime;
        private Duration prefetchTime;

        private BuilderImpl() {
            asyncThreadName("container-credentials-provider");
        }

        private BuilderImpl(ContainerCredentialsProvider credentialsProvider) {
            this.prefetchJitterRandom = credentialsProvider.prefetchJitterRandom;
            this.endpoint = credentialsProvider.endpoint;
            this.asyncCredentialUpdateEnabled = credentialsProvider.asyncCredentialUpdateEnabled;
            this.asyncThreadName = credentialsProvider.asyncThreadName;
            this.sourceChain = credentialsProvider.sourceChain;
            this.staleTime = credentialsProvider.staleTime;
            this.prefetchTime = credentialsProvider.prefetchTime;
        }

        /**
         * The source of the random fraction, from 0 to 1, that places the default prefetch time within its range.
         */
        Builder prefetchJitterRandom(DoubleSupplier prefetchJitterRandom) {
            this.prefetchJitterRandom = prefetchJitterRandom;
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
        public Builder staleTime(Duration staleTime) {
            this.staleTime = staleTime;
            return this;
        }

        public void setStaleTime(Duration staleTime) {
            staleTime(staleTime);
        }

        @Override
        public Builder prefetchTime(Duration prefetchTime) {
            this.prefetchTime = prefetchTime;
            return this;
        }

        public void setPrefetchTime(Duration prefetchTime) {
            prefetchTime(prefetchTime);
        }

        /**
         * An optional string denoting previous credentials providers that are chained with this one.
         * <p><b>Note:</b> This method is primarily intended for use by AWS SDK internal components
         * and should not be used directly by external users.</p>
         */
        @Override
        public Builder sourceChain(String sourceChain) {
            this.sourceChain = sourceChain;
            return this;
        }

        public void setSourceChain(String sourceChain) {
            sourceChain(sourceChain);
        }

        @Override
        public ContainerCredentialsProvider build() {
            return new ContainerCredentialsProvider(this);
        }
    }
}
