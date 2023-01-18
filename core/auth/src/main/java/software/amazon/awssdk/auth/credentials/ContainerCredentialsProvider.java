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

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.internal.ContainerCredentialsRetryPolicy;
import software.amazon.awssdk.auth.credentials.internal.HttpCredentialsLoader;
import software.amazon.awssdk.auth.credentials.internal.HttpCredentialsLoader.LoadedCredentials;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.regions.util.ResourcesEndpointProvider;
import software.amazon.awssdk.regions.util.ResourcesEndpointRetryPolicy;
import software.amazon.awssdk.utils.ComparableUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
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
    private static final Predicate<InetAddress> IS_LOOPBACK_ADDRESS = InetAddress::isLoopbackAddress;
    private static final Predicate<InetAddress> ALLOWED_HOSTS_RULES = IS_LOOPBACK_ADDRESS;
    private static final String HTTPS = "https";

    private final String endpoint;
    private final HttpCredentialsLoader httpCredentialsLoader;
    private final CachedSupplier<AwsCredentials> credentialsCache;

    private final Boolean asyncCredentialUpdateEnabled;

    private final String asyncThreadName;

    /**
     * @see #builder()
     */
    private ContainerCredentialsProvider(BuilderImpl builder) {
        this.endpoint = builder.endpoint;
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.asyncThreadName = builder.asyncThreadName;
        this.httpCredentialsLoader = HttpCredentialsLoader.create();

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
     * Create a builder for creating a {@link ContainerCredentialsProvider}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public String toString() {
        return ToString.create("ContainerCredentialsProvider");
    }

    private RefreshResult<AwsCredentials> refreshCredentials() {
        LoadedCredentials loadedCredentials =
            httpCredentialsLoader.loadCredentials(new ContainerCredentialsEndpointProvider(endpoint));
        Instant expiration = loadedCredentials.getExpiration().orElse(null);

        return RefreshResult.builder(loadedCredentials.getAwsCredentials())
                            .staleTime(staleTime(expiration))
                            .prefetchTime(prefetchTime(expiration))
                            .build();
    }

    private Instant staleTime(Instant expiration) {
        if (expiration == null) {
            return null;
        }

        return expiration.minus(1, ChronoUnit.MINUTES);
    }

    private Instant prefetchTime(Instant expiration) {
        Instant oneHourFromNow = Instant.now().plus(1, ChronoUnit.HOURS);

        if (expiration == null) {
            return oneHourFromNow;
        }

        Instant fifteenMinutesBeforeExpiration = expiration.minus(15, ChronoUnit.MINUTES);

        return ComparableUtils.minimum(oneHourFromNow, fifteenMinutesBeforeExpiration);
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return credentialsCache.get();
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
                return SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValue()
                                                                              .map(this::createUri)
                                                                              .orElseGet(this::createGenericContainerUrl);
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
            SdkSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN.getStringValue()
                                                              .filter(StringUtils::isNotBlank)
                                                              .ifPresent(t -> requestHeaders.put("Authorization", t));
            return requestHeaders;
        }

        private URI createUri(String relativeUri) {
            String host = endpoint != null ? endpoint : SdkSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.getStringValueOrThrow();
            return URI.create(host + relativeUri);
        }

        private URI createGenericContainerUrl() {
            URI uri = URI.create(SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.getStringValueOrThrow());
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

                return addresses.length > 0 && Arrays.stream(addresses)
                                                     .allMatch(this::matchesAllowedHostRules);

            } catch (UnknownHostException e) {
                throw SdkClientException.builder()
                                        .cause(e)
                                        .message(String.format("host (%s) could not be resolved to an IP address.", host))
                                        .build();
            }
        }

        private boolean matchesAllowedHostRules(InetAddress inetAddress) {
            return ALLOWED_HOSTS_RULES.test(inetAddress);
        }
    }

    /**
     * A builder for creating a custom a {@link ContainerCredentialsProvider}.
     */
    public interface Builder extends HttpCredentialsProvider.Builder<ContainerCredentialsProvider, Builder>,
                                     CopyableBuilder<Builder, ContainerCredentialsProvider> {
    }

    private static final class BuilderImpl implements Builder {
        private String endpoint;
        private Boolean asyncCredentialUpdateEnabled;
        private String asyncThreadName;

        private BuilderImpl() {
            asyncThreadName("container-credentials-provider");
        }

        private BuilderImpl(ContainerCredentialsProvider credentialsProvider) {
            this.endpoint = credentialsProvider.endpoint;
            this.asyncCredentialUpdateEnabled = credentialsProvider.asyncCredentialUpdateEnabled;
            this.asyncThreadName = credentialsProvider.asyncThreadName;
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
        public ContainerCredentialsProvider build() {
            return new ContainerCredentialsProvider(this);
        }
    }
}
