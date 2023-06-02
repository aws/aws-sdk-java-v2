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
import software.amazon.awssdk.http.auth.spi.internal.DefaultAsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.internal.DefaultSyncSignRequest;
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
    SyncSignedRequest sign(SyncSignRequest<? extends IdentityT> request);

    /**
     * Method that takes in inputs to sign a request with sync payload and returns a signed version of the request.
     * <p>
     * Similar to {@link #sign(SyncSignRequest)}, but takes a lambda to configure a new {@link SyncSignRequest.Builder}.
     * This removes the need to call {@link SyncSignRequest#builder(IdentityT)}} and
     * {@link SyncSignRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link SyncSignRequest.Builder} will be given.
     * @return A signed version of the request.
     */
    default SyncSignedRequest sign(Consumer<SyncSignRequest.Builder<IdentityT>> consumer) {
        return sign(new DefaultSyncSignRequest.BuilderImpl<IdentityT>().applyMutation(consumer).build());
    }

    /**
     * Method that takes in inputs to sign a request with async payload and returns a signed version of the request.
     *
     * @param request The inputs to sign a request.
     * @return A signed version of the request.
     */
    AsyncSignedRequest signAsync(AsyncSignRequest<? extends IdentityT> request);

    /**
     * Method that takes in inputs to sign a request with async payload and returns a signed version of the request.
     * <p>
     * Similar to {@link #signAsync(AsyncSignRequest)}, but takes a lambda to configure a new
     * {@link AsyncSignRequest.Builder}. This removes the need to call {@link AsyncSignRequest#builder(IdentityT)}} and
     * {@link AsyncSignRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link SignRequest.Builder} will be given.
     * @return A signed version of the request.
     */
    default AsyncSignedRequest signAsync(Consumer<AsyncSignRequest.Builder<IdentityT>> consumer) {
        return signAsync(new DefaultAsyncSignRequest.BuilderImpl<IdentityT>().applyMutation(consumer).build());
    }
}
