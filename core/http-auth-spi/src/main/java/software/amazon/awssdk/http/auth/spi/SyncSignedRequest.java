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

package software.amazon.awssdk.http.auth.spi;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.auth.spi.internal.DefaultSyncSignedRequest;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Represents a request with sync payload that has been signed by {@link HttpSigner}.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public interface SyncSignedRequest extends SignedRequest<ContentStreamProvider> {

    /**
     * Get a new builder for creating a {@link SyncSignedRequest}.
     */
    static Builder builder() {
        return new DefaultSyncSignedRequest.BuilderImpl();
    }

    /**
     * A builder for a {@link SyncSignedRequest}.
     */
    interface Builder extends SignedRequest.Builder<Builder, ContentStreamProvider>,
                              SdkBuilder<Builder, SyncSignedRequest> {
    }
}
