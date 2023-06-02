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

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.spi.internal.DefaultAuthOption;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * An authentication scheme option, composed of the scheme ID and properties for use when resolving the identity and signing
 * the request.
 * <p>
 * This is used in the output from the auth scheme resolver. The resolver returns a list of these, in the order the auth scheme
 * resolver wishes to use them.
 *
 * @see AuthScheme
 */
@SdkProtectedApi
public interface AuthOption {

    /**
     * Get a new builder for creating a {@link AuthOption}.
     */
    static Builder builder() {
        return new DefaultAuthOption.BuilderImpl();
    }

    /**
     * Retrieve the scheme ID, a unique identifier for the authentication scheme (aws.auth#sigv4, smithy.api#httpBearerAuth).
     */
    String schemeId();

    /**
     * Retrieve the value of an {@link IdentityProperty}.
     */
    <T> T identityProperty(IdentityProperty<T> property);

    /**
     * Retrieve the value of an {@link SignerProperty}.
     */
    <T> T signerProperty(SignerProperty<T> property);

    /**
     * A method to operate on all {@link IdentityProperty} values of this AuthOption.
     */
    <T> void forEachIdentityProperty(IdentityPropertyConsumer consumer);

    /**
     * A method to operate on all {@link SignerProperty} values of this AuthOption.
     */
    <T> void forEachSignerProperty(SignerPropertyConsumer consumer);

    /**
     * Interface for operating on an {@link IdentityProperty} value.
     */
    @FunctionalInterface
    interface IdentityPropertyConsumer {
        <T> void accept(IdentityProperty<T> propertyKey, T propertyValue);
    }

    /**
     * Interface for operating on an {@link SignerProperty} value.
     */
    @FunctionalInterface
    interface SignerPropertyConsumer {
        <T> void accept(SignerProperty<T> propertyKey, T propertyValue);
    }

    interface Builder extends SdkBuilder<Builder, AuthOption> {
        Builder schemeId(String schemeId);

        <T> Builder putIdentityProperty(IdentityProperty<T> key, T value);

        <T> Builder putSignerProperty(SignerProperty<T> key, T value);
    }
}
