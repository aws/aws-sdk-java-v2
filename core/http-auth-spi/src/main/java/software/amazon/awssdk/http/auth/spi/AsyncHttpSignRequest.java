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
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.internal.DefaultAsyncHttpSignRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.builder.SdkBuilder;

@SdkPublicApi
@Immutable
@ThreadSafe
public interface AsyncHttpSignRequest<IdentityT extends Identity> extends HttpSignRequest<Publisher<ByteBuffer>, IdentityT> {
    /**
     * Get a new builder for creating a {@link AsyncHttpSignRequest}.
     */
    static <IdentityT extends Identity> Builder<IdentityT> builder(Class<IdentityT> ignoredIdentityType) {
        return new DefaultAsyncHttpSignRequest.BuilderImpl<>();
    }

    @Override
    default Class<Publisher<ByteBuffer>> payloadType() {
        // TODO: Code Review Note: Note this cast
        return (Class) Publisher.class;
    }

    interface Builder<IdentityT extends Identity> extends HttpSignRequest.Builder<Publisher<ByteBuffer>, IdentityT>,
                                                          SdkBuilder<Builder<IdentityT>, AsyncHttpSignRequest<IdentityT>> {
        @Override
        Builder<IdentityT> request(SdkHttpRequest request);

        @Override
        Builder<IdentityT> payload(Publisher<ByteBuffer> payload);

        @Override
        Builder<IdentityT> identity(IdentityT identity);

        @Override
        <T> Builder<IdentityT> putProperty(SignerProperty<T> key, T value);
    }
}
