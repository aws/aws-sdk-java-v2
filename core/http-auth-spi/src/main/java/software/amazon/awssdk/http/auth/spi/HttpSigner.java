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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.internal.DefaultAsyncHttpSignRequest;
import software.amazon.awssdk.http.auth.spi.internal.DefaultSyncHttpSignRequest;
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
    SyncSignedHttpRequest sign(SyncHttpSignRequest<IdentityT> request);

    /**
     * Method that takes in inputs to sign a request with sync payload and returns a signed version of the request.
     * <p>
     * Similar to {@link #sign(SyncHttpSignRequest)}, but takes a lambda to configure a new {@link SyncHttpSignRequest.Builder}.
     * This removes the need to call {@link SyncHttpSignRequest#builder(IdentityT)}} and
     * {@link SyncHttpSignRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link SyncHttpSignRequest.Builder} will be given.
     * @return A signed version of the request.
     */
    default SyncSignedHttpRequest sign(Consumer<SyncHttpSignRequest.Builder<IdentityT>> consumer) {
        return sign(new DefaultSyncHttpSignRequest.BuilderImpl<IdentityT>().applyMutation(consumer).build());
    }

    /**
     * Method that takes in inputs to sign a request with async payload and returns a signed version of the request.
     *
     * @param request The inputs to sign a request.
     * @return A signed version of the request.
     */
    AsyncSignedHttpRequest signAsync(AsyncHttpSignRequest<IdentityT> request);

    /**
     * Method that takes in inputs to sign a request with async payload and returns a signed version of the request.
     * <p>
     * Similar to {@link #signAsync(AsyncHttpSignRequest)}, but takes a lambda to configure a new
     * {@link AsyncHttpSignRequest.Builder}. This removes the need to call {@link AsyncHttpSignRequest#builder(IdentityT)}} and
     * {@link AsyncHttpSignRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link HttpSignRequest.Builder} will be given.
     * @return A signed version of the request.
     */
    default AsyncSignedHttpRequest signAsync(Consumer<AsyncHttpSignRequest.Builder<IdentityT>> consumer) {
        return signAsync(new DefaultAsyncHttpSignRequest.BuilderImpl<IdentityT>().applyMutation(consumer).build());
    }
}
