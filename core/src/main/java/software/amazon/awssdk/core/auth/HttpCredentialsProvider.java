/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.auth;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.CredentialsEndpointProvider;
import software.amazon.awssdk.core.internal.HttpCredentialsUtils;
import software.amazon.awssdk.core.util.ComparableUtils;
import software.amazon.awssdk.core.util.DateUtils;
import software.amazon.awssdk.core.util.json.JacksonUtils;
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
abstract class HttpCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    private final CachedSupplier<AwsCredentials> credentialsCache;

    HttpCredentialsProvider(Builder<?, ?> builder) {
        this(builder.asyncCredentialUpdateEnabled, builder.asyncThreadName);
    }

    HttpCredentialsProvider(boolean asyncCredentialUpdateEnabled, String asyncThreadName) {
        CachedSupplier.Builder<AwsCredentials> cacheBuilder = CachedSupplier.builder(this::refreshCredentials);
        if (asyncCredentialUpdateEnabled) {
            cacheBuilder.prefetchStrategy(new NonBlocking(asyncThreadName));
        }
        this.credentialsCache = cacheBuilder.build();
    }

    protected abstract CredentialsEndpointProvider getCredentialsEndpointProvider();

    private RefreshResult<AwsCredentials> refreshCredentials() {
        try {
            String credentialsResponse = HttpCredentialsUtils.instance().readResource(getCredentialsEndpointProvider());

            JsonNode node = JacksonUtils.jsonNodeOf(credentialsResponse);
            JsonNode accessKey = node.get("AccessKeyId");
            JsonNode secretKey = node.get("SecretAccessKey");
            JsonNode token = node.get("Token");
            JsonNode expirationNode = node.get("Expiration");

            Validate.notNull(accessKey, "Failed to load access key.");
            Validate.notNull(secretKey, "Failed to load secret key.");

            AwsCredentials credentials =
                    token == null ? new AwsCredentials(accessKey.asText(), secretKey.asText())
                                  : AwsSessionCredentials.create(accessKey.asText(), secretKey.asText(), token.asText());

            Instant expiration = getExpiration(expirationNode).orElse(null);
            if (expiration != null && Instant.now().isAfter(expiration)) {
                throw new SdkClientException("Credentials obtained from metadata service are already expired.");
            }
            return RefreshResult.builder(credentials)
                                .staleTime(getStaleTime(expiration))
                                .prefetchTime(getPrefetchTime(expiration))
                                .build();
        } catch (SdkClientException e) {
            throw e;
        } catch (JsonMappingException e) {
            throw new SdkClientException("Unable to parse response returned from service endpoint.", e);
        } catch (RuntimeException | IOException e) {
            throw new SdkClientException("Unable to load credentials from service endpoint.", e);
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
    public AwsCredentials getCredentials() {
        return credentialsCache.get();
    }

    @Override
    public void close() {
        credentialsCache.close();
    }

    /**
     * A builder for creating a custom a {@link InstanceProfileCredentialsProvider}.
     */
    protected abstract static class Builder<TypeToBuildT extends HttpCredentialsProvider, BuilderT extends Builder> {
        private boolean asyncCredentialUpdateEnabled = false;
        private String asyncThreadName;

        /**
         * Created using {@link #builder()}.
         */
        protected Builder() {}

        /**
         * Configure whether this provider should fetch credentials asynchronously in the background. If this is true, threads are
         * less likely to block when {@link #getCredentials()} is called, but additional resources are used to maintain the
         * provider.
         *
         * <p>By default, this is disabled.</p>
         */
        public BuilderT asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return (BuilderT) this;
        }

        public BuilderT asyncThreadName(String asyncThreadName) {
            this.asyncThreadName = asyncThreadName;
            return (BuilderT) this;
        }

        public abstract TypeToBuildT build();
    }
}
