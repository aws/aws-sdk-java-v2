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
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.auth.spi.internal.DefaultHttpSignRequest;
import software.amazon.awssdk.identity.spi.Identity;

/**
 * Interface for the process of modifying a request destined for a service so that the service can authenticate the SDK
 * customer’s identity.
 *
 * @param <IdentityT> The type of the identity.
 */
@SdkPublicApi
public interface HttpSigner<IdentityT extends Identity> {

    /**
     * Method that takes in inputs to sign a request with sync payload and returns a signed version of the request.
     *
     * @param request The inputs to sign a request.
     * @return A signed version of the request.
     */
    SignedHttpRequest<ContentStreamProvider> sign(HttpSignRequest<ContentStreamProvider, IdentityT> request);

    /**
     * Method that takes in inputs to sign a request with sync payload and returns a signed version of the request.
     * <p>
     * Similar to {@link #sign(HttpSignRequest)}, but takes a lambda to configure a new {@link HttpSignRequest.Builder}. This
     * removes the need to call {@link HttpSignRequest#builder(Class, Class)}} and {@link HttpSignRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link HttpSignRequest.Builder} will be given.
     * @return A signed version of the request.
     */
    default SignedHttpRequest<ContentStreamProvider> sign(
        Consumer<HttpSignRequest.Builder<ContentStreamProvider, IdentityT>> consumer) {
        return sign(new DefaultHttpSignRequest.BuilderImpl<ContentStreamProvider, IdentityT>(ContentStreamProvider.class)
                        .applyMutation(consumer).build());
    }

    /**
     * Method that takes in inputs to sign a request with async payload and returns a signed version of the request.
     *
     * @param request The inputs to sign a request.
     * @return A signed version of the request.
     */
    SignedHttpRequest<Publisher<ByteBuffer>> signAsync(HttpSignRequest<Publisher<ByteBuffer>, IdentityT> request);

    /**
     * Method that takes in inputs to sign a request with async payload and returns a signed version of the request.
     * <p>
     * Similar to {@link #signAsync(HttpSignRequest)}, but takes a lambda to configure a new {@link HttpSignRequest.Builder}. This
     * removes the need to call {@link HttpSignRequest#builder(Class, Class)}} and {@link HttpSignRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link HttpSignRequest.Builder} will be given.
     * @return A signed version of the request.
     */
    default SignedHttpRequest<Publisher<ByteBuffer>> signAsync(
        Consumer<HttpSignRequest.Builder<Publisher<ByteBuffer>, IdentityT>> consumer) {
        return signAsync(new DefaultHttpSignRequest.BuilderImpl<Publisher<ByteBuffer>, IdentityT>((Class)Publisher.class)
                        .applyMutation(consumer).build());
    }
}
