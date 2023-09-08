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
import software.amazon.awssdk.http.auth.spi.internal.DefaultSignedRequest;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Represents a request with sync payload that has been signed by {@link HttpSigner}.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public interface SignedRequest extends BaseSignedRequest<ContentStreamProvider> {

    /**
     * Get a new builder for creating a {@link SignedRequest}.
     */
    static Builder builder() {
        return new DefaultSignedRequest.BuilderImpl();
    }

    /**
     * A builder for a {@link SignedRequest}.
     */
    interface Builder extends BaseSignedRequest.Builder<Builder, ContentStreamProvider>,
                              SdkBuilder<Builder, SignedRequest> {
    }
}
