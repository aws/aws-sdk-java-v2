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

import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.internal.DefaultAsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.internal.DefaultSignRequest;
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
     * A {@link Clock} to be used to derive the signing time. This property defaults to the system clock.
     *
     * <p>Note, signing time may not be relevant to some signers.
     */
    SignerProperty<Clock> SIGNING_CLOCK = SignerProperty.create(Clock.class, "SigningClock");

    /**
     * Method that takes in inputs to sign a request with sync payload and returns a signed version of the request.
     *
     * @param request The inputs to sign a request.
     * @return A signed version of the request.
     */
    SignedRequest sign(SignRequest<? extends IdentityT> request);

    /**
     * Method that takes in inputs to sign a request with sync payload and returns a signed version of the request.
     * <p>
     * Similar to {@link #sign(SignRequest)}, but takes a lambda to configure a new {@link SignRequest.Builder}.
     * This removes the need to call {@link SignRequest#builder(IdentityT)}} and
     * {@link SignRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link SignRequest.Builder} will be given.
     * @return A signed version of the request.
     */
    default SignedRequest sign(Consumer<SignRequest.Builder<IdentityT>> consumer) {
        return sign(DefaultSignRequest.<IdentityT>builder().applyMutation(consumer).build());
    }

    /**
     * Method that takes in inputs to sign a request with async payload and returns a future containing the signed version of the
     * request.
     *
     * @param request The inputs to sign a request.
     * @return A future containing the signed version of the request.
     */
    CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends IdentityT> request);

    /**
     * Method that takes in inputs to sign a request with async payload and returns a future containing the signed version of the
     * request.
     * <p>
     * Similar to {@link #signAsync(AsyncSignRequest)}, but takes a lambda to configure a new
     * {@link AsyncSignRequest.Builder}. This removes the need to call {@link AsyncSignRequest#builder(IdentityT)}} and
     * {@link AsyncSignRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link BaseSignRequest.Builder} will be given.
     * @return A future containing the signed version of the request.
     */
    default CompletableFuture<AsyncSignedRequest> signAsync(Consumer<AsyncSignRequest.Builder<IdentityT>> consumer) {
        return signAsync(DefaultAsyncSignRequest.<IdentityT>builder().applyMutation(consumer).build());
    }
}
