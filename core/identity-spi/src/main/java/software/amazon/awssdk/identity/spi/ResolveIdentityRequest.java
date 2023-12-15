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

package software.amazon.awssdk.identity.spi;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.identity.spi.internal.DefaultResolveIdentityRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to resolve an {@link Identity}.
 * <p>
 * The Identity may be determined for each request based on properties of the request (e.g. different credentials per bucket
 * for S3).
 *
 * @see IdentityProvider
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public interface ResolveIdentityRequest extends ToCopyableBuilder<ResolveIdentityRequest.Builder, ResolveIdentityRequest> {

    /**
     * Get a new builder for creating a {@link ResolveIdentityRequest}.
     */
    static Builder builder() {
        return DefaultResolveIdentityRequest.builder();
    }

    /**
     * Returns the value of a property that the {@link IdentityProvider} can use while resolving the identity.
     */
    <T> T property(IdentityProperty<T> property);

    /**
     * A builder for a {@link ResolveIdentityRequest}.
     */
    interface Builder extends CopyableBuilder<Builder, ResolveIdentityRequest> {

        /**
         * Set a property that the {@link IdentityProvider} can use while resolving the identity.
         */
        <T> Builder putProperty(IdentityProperty<T> key, T value);
    }
}
