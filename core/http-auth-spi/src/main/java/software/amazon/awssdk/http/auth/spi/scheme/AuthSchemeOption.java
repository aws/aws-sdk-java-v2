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

package software.amazon.awssdk.http.auth.spi.scheme;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.internal.scheme.DefaultAuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An authentication scheme option, composed of the scheme ID and properties for use when resolving the identity and signing
 * the request.
 * <p>
 * This is used in the output from the auth scheme resolver. The resolver returns a list of these, in the order the auth scheme
 * resolver wishes to use them.
 *
 * @see AuthScheme
 */
@SdkPublicApi
public interface AuthSchemeOption extends ToCopyableBuilder<AuthSchemeOption.Builder, AuthSchemeOption> {

    /**
     * Get a new builder for creating a {@link AuthSchemeOption}.
     */
    static Builder builder() {
        return DefaultAuthSchemeOption.builder();
    }

    /**
     * Retrieve the scheme ID, a unique identifier for the authentication scheme (aws.auth#sigv4, smithy.api#httpBearerAuth).
     */
    String schemeId();

    /**
     * Retrieve the value of an {@link IdentityProperty}.
     * @param property The IdentityProperty to retrieve the value of.
     * @param <T> The type of the IdentityProperty.
     */
    <T> T identityProperty(IdentityProperty<T> property);

    /**
     * Retrieve the value of an {@link SignerProperty}.
     * @param property The SignerProperty to retrieve the value of.
     * @param <T> The type of the SignerProperty.
     */
    <T> T signerProperty(SignerProperty<T> property);

    /**
     * A method to operate on all {@link IdentityProperty} values of this AuthSchemeOption.
     * @param consumer The method to apply to each IdentityProperty.
     */
    void forEachIdentityProperty(IdentityPropertyConsumer consumer);

    /**
     * A method to operate on all {@link SignerProperty} values of this AuthSchemeOption.
     * @param consumer The method to apply to each SignerProperty.
     */
    void forEachSignerProperty(SignerPropertyConsumer consumer);

    /**
     * Interface for operating on an {@link IdentityProperty} value.
     */
    interface IdentityPropertyConsumer {
        /**
         * A method to operate on an {@link IdentityProperty} and it's value.
         * @param propertyKey The IdentityProperty.
         * @param propertyValue The value of the IdentityProperty.
         * @param <T> The type of the IdentityProperty.
         */
        <T> void accept(IdentityProperty<T> propertyKey, T propertyValue);
    }

    /**
     * Interface for operating on an {@link SignerProperty} value.
     */
    interface SignerPropertyConsumer {
        /**
         * A method to operate on a {@link SignerProperty} and it's value.
         * @param propertyKey The SignerProperty.
         * @param propertyValue The value of the SignerProperty.
         * @param <T> The type of the SignerProperty.
         */
        <T> void accept(SignerProperty<T> propertyKey, T propertyValue);
    }

    /**
     * A builder for a {@link AuthSchemeOption}.
     */
    interface Builder extends CopyableBuilder<Builder, AuthSchemeOption> {

        /**
         * Set the scheme ID.
         */
        Builder schemeId(String schemeId);

        /**
         * Update or add the provided property value.
         */
        <T> Builder putIdentityProperty(IdentityProperty<T> key, T value);

        /**
         * Add the provided property value if the property does not already exist.
         */
        <T> Builder putIdentityPropertyIfAbsent(IdentityProperty<T> key, T value);

        /**
         * Update or add the provided property value.
         */
        <T> Builder putSignerProperty(SignerProperty<T> key, T value);

        /**
         * Add the provided property value if the property does not already exist.
         */
        <T> Builder putSignerPropertyIfAbsent(SignerProperty<T> key, T value);
    }
}
