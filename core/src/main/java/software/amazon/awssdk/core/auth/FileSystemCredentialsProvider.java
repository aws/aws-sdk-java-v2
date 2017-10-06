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

import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * An implementation of {@link AwsCredentialsProvider} that defines common caching behavior for all credential providers that are
 * backed by the file system. This allows us to ensure file-system based credentials providers have the same caching behavior
 * across the SDK.
 */
@SdkInternalApi
abstract class FileSystemCredentialsProvider implements AwsCredentialsProvider {
    private CachedSupplier<AwsCredentials> cachedCredentials;

    protected FileSystemCredentialsProvider() {
        this.cachedCredentials = CachedSupplier.builder(this::refreshCredentials).build();
    }

    private RefreshResult<AwsCredentials> refreshCredentials() {
        return RefreshResult.builder(loadCredentials())
                            .prefetchTime(Instant.now().plus(Duration.ofMinutes(5)))
                            .build();
    }

    /**
     * Load the credentials from the file system.
     */
    protected abstract AwsCredentials loadCredentials();

    @Override
    public final AwsCredentials getCredentials() {
        return cachedCredentials.get();
    }
}
