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

package software.amazon.awssdk.services.s3.internal.s3express;

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateSessionRequest;
import software.amazon.awssdk.services.s3.model.SessionCredentials;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.utils.cache.lru.LruCache;

@SdkInternalApi
public class S3ExpressIdentityCache {

    /**
     * Original specification calls for 100. We'll use 25 for now, pending testing.
     */
    private static final Integer DEFAULT_LRU_CACHE_SIZE = 25;

    /**
     * Control timeout for create session requests so that calls are not blocked.
     */
    private static final Duration DEFAULT_API_CALL_TIMEOUT = Duration.ofSeconds(10);

    private final LruCache<S3ExpressIdentityKey, CachedS3ExpressCredentials> cache;

    private S3ExpressIdentityCache() {
        this.cache = initCache();
    }

    public static S3ExpressIdentityCache create() {
        return new S3ExpressIdentityCache();
    }

    //TODO (s3express) add test to make sure the right exception type is returned and not CompletionException
    public S3ExpressSessionCredentials get(S3ExpressIdentityKey key) {
        CachedS3ExpressCredentials cachedCredentials = cache.get(key);
        return S3ExpressSessionCredentials.fromSessionResponse(cachedCredentials.get());
    }

    private LruCache<S3ExpressIdentityKey, CachedS3ExpressCredentials> initCache() {
        return LruCache.builder(this::getCachedCredentials)
                       .maxSize(DEFAULT_LRU_CACHE_SIZE)
                       .build();
    }

    private CachedS3ExpressCredentials getCachedCredentials(S3ExpressIdentityKey key) {
        AwsCredentials credentialsIdentity = CredentialUtils.toCredentials(key.identity());
        StaticCredentialsProvider resolvedCredentialsProvider = StaticCredentialsProvider.create(credentialsIdentity);
        return CachedS3ExpressCredentials.builder(k -> getCredentials(k, resolvedCredentialsProvider))
                                         .key(key)
                                         .build();
    }

    SessionCredentials getCredentials(S3ExpressIdentityKey key, IdentityProvider<AwsCredentialsIdentity> provider) {
        SdkClient client = key.client();
        String bucket = key.bucket();
        SdkServiceClientConfiguration serviceClientConfiguration = client.serviceClientConfiguration();

        if (client instanceof S3AsyncClient) {
            // TODO (s3express) don't join here
            return ((S3AsyncClient) client).createSession(createSessionRequest(bucket, provider, serviceClientConfiguration))
                                           .join()
                                           .credentials();
        }
        if (client instanceof S3Client) {
            return ((S3Client) client).createSession(createSessionRequest(bucket, provider, serviceClientConfiguration))
                                      .credentials();
        }
        throw new UnsupportedOperationException("SdkClient must be either an S3Client or an S3AsyncClient, but was " +
                                                client.getClass());
    }

    private static CreateSessionRequest
            createSessionRequest(String bucket,
                                 IdentityProvider<AwsCredentialsIdentity> provider,
                                 SdkServiceClientConfiguration serviceClientConfiguration) {

        Duration requestApiCallTimeout = clientSetTimeoutIfExists(serviceClientConfiguration).orElse(DEFAULT_API_CALL_TIMEOUT);

        return CreateSessionRequest.builder().bucket(bucket)
                     .overrideConfiguration(o -> o.credentialsProvider(provider)
                                                  .apiCallTimeout(requestApiCallTimeout)).build();
    }

    private static Optional<Duration> clientSetTimeoutIfExists(SdkServiceClientConfiguration serviceClientConfiguration) {
        if (serviceClientConfiguration != null && serviceClientConfiguration.overrideConfiguration() != null) {
            return serviceClientConfiguration.overrideConfiguration().apiCallTimeout();
        }
        return Optional.empty();
    }
}
