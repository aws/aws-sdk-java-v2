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

package software.amazon.awssdk.http.auth.spi.signer;

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.Validate;

/**
 * Base interface to represent input parameters to sign a request using {@link HttpSigner}, independent of payload type. See
 * specific sub-interfaces {@link SignRequest} for sync payload and {@link AsyncSignRequest} for async payload.
 *
 * @param <PayloadT> The type of payload of the request.
 * @param <IdentityT> The type of the identity.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public interface BaseSignRequest<PayloadT, IdentityT extends Identity> {

    /**
     * Returns the HTTP request object, without the request body payload.
     */
    SdkHttpRequest request();

    /**
     * Returns the body payload of the request. A payload is optional. By default, the payload will be empty.
     */
    Optional<PayloadT> payload();

    /**
     * Returns the identity.
     */
    IdentityT identity();

    /**
     * Returns the value of a property that the {@link HttpSigner} can use during signing.
     */
    <T> T property(SignerProperty<T> property);

    /**
     * Ensure that the {@link SignerProperty} is present in the {@link BaseSignRequest}.
     * <p>
     * The value, {@link T}, is return when present, and an exception is thrown otherwise.
     */
    default <T> boolean hasProperty(SignerProperty<T> property) {
        return property(property) != null;
    }

    /**
     * Ensure that the {@link SignerProperty} is present in the {@link BaseSignRequest}.
     * <p>
     * The value, {@link T}, is return when present, and an exception is thrown otherwise.
     */
    default <T> T requireProperty(SignerProperty<T> property) {
        return Validate.notNull(property(property), property.toString() + " must not be null!");
    }

    /**
     * Ensure that the {@link SignerProperty} is present in the {@link BaseSignRequest}.
     * <p>
     * The value, {@link T}, is return when present, and the default is returned otherwise.
     */
    default <T> T requireProperty(SignerProperty<T> property, T defaultValue) {
        return Validate.getOrDefault(property(property), () -> defaultValue);
    }

    /**
     * A builder for a {@link BaseSignRequest}.
     */
    interface Builder<B extends Builder<B, PayloadT, IdentityT>, PayloadT, IdentityT extends Identity> {

        /**
         * Set the HTTP request object, without the request body payload.
         */
        B request(SdkHttpRequest request);

        /**
         * Set the body payload of the request. A payload is optional. By default, the payload will be empty.
         */
        B payload(PayloadT payload);

        /**
         * Set the identity of the request.
         */
        B identity(IdentityT identity);

        /**
         * Set a property that the {@link HttpSigner} can use during signing.
         */
        <T> B putProperty(SignerProperty<T> key, T value);
    }
}
