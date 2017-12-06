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
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkClientException;
import software.amazon.awssdk.core.internal.CredentialsEndpointProvider;
import software.amazon.awssdk.core.internal.EC2CredentialsUtils;
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
 * an EC2 instance.
 */
@SdkInternalApi
class EC2CredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    private final CredentialsEndpointProvider credentialsEndpointProvider;
    private final CachedSupplier<AwsCredentials> credentialsCache;

    EC2CredentialsProvider(CredentialsEndpointProvider credentialsEndpointProvider,
                                  boolean asyncRefreshEnabled,
                                  String asyncThreadName) {
        this.credentialsEndpointProvider = credentialsEndpointProvider;

        CachedSupplier.Builder<AwsCredentials> cacheBuilder = CachedSupplier.builder(this::refreshCredentials);
        if (asyncRefreshEnabled) {
            cacheBuilder.prefetchStrategy(new NonBlocking(asyncThreadName));
        }
        this.credentialsCache = cacheBuilder.build();
    }

    private RefreshResult<AwsCredentials> refreshCredentials() {
        try {
            String credentialsResponse = EC2CredentialsUtils.getInstance()
                                                            .readResource(credentialsEndpointProvider.getCredentialsEndpoint(),
                                                                          credentialsEndpointProvider.getRetryPolicy());

            JsonNode node = JacksonUtils.jsonNodeOf(credentialsResponse);
            JsonNode accessKey = node.get("AccessKeyId");
            JsonNode secretKey = node.get("SecretAccessKey");
            JsonNode token = node.get("Token");
            JsonNode expirationNode = node.get("Expiration");

            Validate.notNull(accessKey, "Failed to load access key.");
            Validate.notNull(secretKey, "Failed to load secret key.");

            AwsCredentials credentials =
                    token == null ? AwsCredentials.create(accessKey.asText(), secretKey.asText())
                                  : AwsSessionCredentials.create(accessKey.asText(), secretKey.asText(), token.asText());

            Instant expiration = getExpiration(expirationNode).orElse(null);
            if (expiration != null && Instant.now().isAfter(expiration)) {
                throw new SdkClientException("Credentials obtained from EC2 metadata service are already expired");
            }
            return RefreshResult.builder(credentials)
                                .staleTime(getStaleTime(expiration))
                                .prefetchTime(getPrefetchTime(expiration))
                                .build();
        } catch (SdkClientException e) {
            throw e;
        } catch (JsonMappingException e) {
            throw new SdkClientException("Unable to parse response returned from service endpoint.", e);
        } catch (RuntimeException | IOException | URISyntaxException e) {
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
                throw new IllegalStateException("Unable to parse credentials expiration date from Amazon EC2 instance.", e);
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

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
