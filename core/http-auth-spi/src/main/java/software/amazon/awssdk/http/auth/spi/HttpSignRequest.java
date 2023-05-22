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

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.Identity;

/**
 * Input parameters to sign a request using {@link HttpSigner}.
 *
 * @param <PayloadT> The type of payload of the request.
 * @param <IdentityT> The type of the identity.
 */
@SdkProtectedApi
@Immutable
@ThreadSafe
public interface HttpSignRequest<PayloadT, IdentityT extends Identity> {

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
     * A builder for a {@link HttpSignRequest}.
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
