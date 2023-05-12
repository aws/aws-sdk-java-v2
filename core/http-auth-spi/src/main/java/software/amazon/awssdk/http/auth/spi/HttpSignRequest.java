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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.internal.DefaultHttpSignRequest;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Represents a request to be signed by {@link HttpSigner}.
 *
 * @param <PayloadT> The type of payload of this request.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public interface HttpSignRequest<PayloadT> {

    /**
     * Get a new builder for creating a {@link HttpSignRequest}.
     */
    static <PayloadT> Builder<PayloadT> builder(Class<PayloadT> payloadType) {
        return new DefaultHttpSignRequest.BuilderImpl<>(payloadType);
    }

    /**
     * Returns the type of the payload.
     */
    Class<PayloadT> payloadType();

    /**
     * Returns the HTTP request object, without the request body payload.
     */
    SdkHttpRequest request();

    /**
     * Returns the body payload of the request. A payload is optional. By default, the payload will be empty.
     */
    Optional<PayloadT> payload();

    /**
     * Returns the property that the {@link HttpSigner} can use during signing.
     */
    <T> T property(SignerProperty<T> property);

    /**
     * A builder for a {@link HttpSignRequest}.
     */
    interface Builder<PayloadT> extends SdkBuilder<Builder<PayloadT>,
            HttpSignRequest<PayloadT>> {

        /**
         * Set the HTTP request object, without the request body payload.
         */
        Builder<PayloadT> request(SdkHttpRequest request);

        /**
         * Set the body payload of the request. A payload is optional. By default, the payload will be empty.
         */
        Builder<PayloadT> payload(PayloadT payload);

        /**
         * Set a property that the {@link HttpSigner} can use during signing.
         */
        <T> Builder<PayloadT> putProperty(SignerProperty<T> key, T value);
    }
}
