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

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.auth.spi.internal.DefaultAsyncSignedRequest;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Represents a request with async payload that has been signed by {@link HttpSigner}.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public interface AsyncSignedRequest extends BaseSignedRequest<Publisher<ByteBuffer>> {

    /**
     * Get a new builder for creating a {@link AsyncSignedRequest}.
     */
    static Builder builder() {
        return new DefaultAsyncSignedRequest.BuilderImpl();
    }

    /**
     * A builder for a {@link AsyncSignedRequest}.
     */
    interface Builder extends BaseSignedRequest.Builder<Builder, Publisher<ByteBuffer>>,
                              SdkBuilder<Builder, AsyncSignedRequest> {
    }
}
