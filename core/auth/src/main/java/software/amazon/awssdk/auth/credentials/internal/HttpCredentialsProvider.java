/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.ComparableUtils;
import software.amazon.awssdk.core.util.DateUtils;
import software.amazon.awssdk.core.util.json.JacksonUtils;
import software.amazon.awssdk.regions.internal.util.HttpResourcesUtils;
import software.amazon.awssdk.regions.internal.util.ResourcesEndpointProvider;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * Helper class that contains the common behavior of the CredentialsProviders that loads the credentials from a local endpoint on
 * a container (e.g. an EC2 instance).
 */
@SdkInternalApi
public abstract class HttpCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    private final Optional<CachedSupplier<AwsCredentials>> credentialsCache;

    protected HttpCredentialsProvider(BuilderImpl<?, ?> builder) {
        this(builder.asyncCredentialUpdateEnabled, builder.asyncThreadName);
    }

    HttpCredentialsProvider(boolean asyncCredentialUpdateEnabled, String asyncThreadName) {
        if (isLocalCredentialLoadingDisabled()) {
            this.credentialsCache = Optional.empty();
        } else {
            CachedSupplier.Builder<AwsCredentials> cacheBuilder = CachedSupplier.builder(this::refreshCredentials);
            if (asyncCredentialUpdateEnabled) {
                cacheBuilder.prefetchStrategy(new NonBlocking(asyncThreadName));
            }
            this.credentialsCache = Optional.of(cacheBuilder.build());
        }
    }

    protected abstract ResourcesEndpointProvider getCredentialsEndpointProvider();

    /**
     * Can be overridden by subclass to decide whether loading credential is disabled or not.
     *
     * @return whether loading credential from local endpoint is disabled.
     */
    protected boolean isLocalCredentialLoadingDisabled() {
        return false;
    }

    private RefreshResult<AwsCredentials> refreshCredentials() {
        try {
            String credentialsResponse = HttpResourcesUtils.instance().readResource(getCredentialsEndpointProvider());

            JsonNode node = JacksonUtils.jsonNodeOf(credentialsResponse);
            JsonNode accessKey = node.get("AccessKeyId");
            JsonNode secretKey = node.get("SecretAccessKey");
            JsonNode token = node.get("Token");
            JsonNode expirationNode = node.get("Expiration");

            Validate.notNull(accessKey, "Failed to load access key.");
            Validate.notNull(secretKey, "Failed to load secret key.");

            AwsCredentials credentials =
                token == null ? AwsBasicCredentials.create(accessKey.asText(), secretKey.asText())
                              : AwsSessionCredentials.create(accessKey.asText(), secretKey.asText(), token.asText());

            Instant expiration = getExpiration(expirationNode).orElse(null);
            if (expiration != null && Instant.now().isAfter(expiration)) {
                throw SdkClientException.builder()
                                        .message("Credentials obtained from metadata service are already expired.")
                                        .build();
            }
            return RefreshResult.builder(credentials)
                                .staleTime(getStaleTime(expiration))
                                .prefetchTime(getPrefetchTime(expiration))
                                .build();
        } catch (SdkClientException e) {
            throw e;
        } catch (JsonMappingException e) {
            throw SdkClientException.builder()
                                    .message("Unable to parse response returned from service endpoint.")
                                    .cause(e)
                                    .build();
        } catch (RuntimeException | IOException e) {
            throw SdkClientException.builder()
                                    .message("Unable to load credentials from service endpoint.")
                                    .cause(e)
                                    .build();
        }
    }

    private Optional<Instant> getExpiration(JsonNode expirationNode) {
        return Optional.ofNullable(expirationNode).map(node -> {
            // Convert the expirationNode string to ISO-8601 format.
            String expirationValue = node.asText().replaceAll("\\+0000$", "Z");

            try {
                return DateUtils.parseIso8601Date(expirationValue);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Unable to parse credentials expiration date from metadata service.", e);
            }
        });
    }

    private Instant getStaleTime(Instant expiration) {
        return expiration == null ? null
                                  : expiration.minus(Duration.ofMinutes(1));
    }

    private Instant getPrefetchTime(Instant expiration) {
        Instant oneHourFromNow = Instant.now().plus(Duration.ofHours(1));
        return expiration == null ? oneHourFromNow
                                  : ComparableUtils.minimum(oneHourFromNow, expiration.minus(Duration.ofMinutes(15)));
    }

    @Override
    public AwsCredentials resolveCredentials() {
        if (isLocalCredentialLoadingDisabled()) {
            throw SdkClientException.builder()
                                    .message("Loading credentials from local endpoint is disabled. Unable to load " +
                                             "credentials from service endpoint.")
                                    .build();
        }
        return credentialsCache.map(CachedSupplier::get).orElseThrow(() ->
                SdkClientException.builder().message("Unable to load credentials from service endpoint").build());
    }

    @Override
    public void close() {
        credentialsCache.ifPresent(CachedSupplier::close);
    }

    public interface Builder<TypeToBuildT extends HttpCredentialsProvider, BuilderT extends Builder> {
        /**
         * Configure whether this provider should fetch credentials asynchronously in the background. If this is true, threads are
         * less likely to block when {@link #resolveCredentials()} is called, but additional resources are used to maintain the
         * provider.
         *
         * <p>
         * By default, this is disabled.
         */
        BuilderT asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled);

        BuilderT asyncThreadName(String asyncThreadName);

        TypeToBuildT build();
    }

    /**
     * A builder for creating a custom a {@link InstanceProfileCredentialsProvider}.
     */
    protected abstract static class BuilderImpl<TypeToBuildT extends HttpCredentialsProvider, BuilderT extends Builder>
        implements Builder<TypeToBuildT, BuilderT> {
        private boolean asyncCredentialUpdateEnabled = false;
        private String asyncThreadName;

        protected BuilderImpl() {
        }

        @Override
        public BuilderT asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return (BuilderT) this;
        }

        public void setAsyncCredentialUpdateEnabled(boolean asyncCredentialUpdateEnabled) {
            asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled);
        }

        @Override
        public BuilderT asyncThreadName(String asyncThreadName) {
            this.asyncThreadName = asyncThreadName;
            return (BuilderT) this;
        }

        public void setAsyncThreadName(String asyncThreadName) {
            asyncThreadName(asyncThreadName);
        }
    }
}
