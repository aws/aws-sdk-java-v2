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
import software.amazon.awssdk.http.auth.spi.internal.DefaultSyncHttpSignRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Input parameters to sign a request with sync payload, using {@link HttpSigner}.
 *
 * @param <IdentityT> The type of the identity.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public interface SyncHttpSignRequest<IdentityT extends Identity> extends HttpSignRequest<ContentStreamProvider, IdentityT> {
    /**
     * Get a new builder for creating a {@link SyncHttpSignRequest}.
     */
    static <IdentityT extends Identity> Builder<IdentityT> builder(IdentityT identity) {
        return new DefaultSyncHttpSignRequest.BuilderImpl<>(identity);
    }

    /**
     * A builder for a {@link SyncHttpSignRequest}.
     */
    interface Builder<IdentityT extends Identity>
        extends HttpSignRequest.Builder<Builder<IdentityT>, ContentStreamProvider, IdentityT>,
                SdkBuilder<Builder<IdentityT>, SyncHttpSignRequest<IdentityT>> {
    }
}
