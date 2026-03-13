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
 *
 * <p>
 * Auth scheme options are returned by {@link AuthSchemeProvider}s to specify which authentication schemes should be used
 * for a request, along with the properties needed to configure the identity provider and signer. The SDK will attempt
 * to use the schemes in the order they are returned.
 *
 * <p>
 * Each option contains:
 * <ul>
 *     <li>A scheme ID - Identifies which {@link AuthScheme} to use (e.g., "aws.auth#sigv4")</li>
 *     <li>Identity properties - Configuration for the identity provider (e.g., account ID, role ARN)</li>
 *     <li>Signer properties - Configuration for the signer (e.g., signing name, region, algorithm parameters)</li>
 * </ul>
 *
 * <p>
 * <b>Using Auth Scheme Options</b>
 * <p>
 * Auth scheme options are typically created and modified within custom {@link AuthSchemeProvider} implementations
 * to customize authentication behavior.
 *
 * <p>
 * Example - Modifying signer properties in an auth scheme option:
 *
 * {@snippet :
 * public class CustomSigningNameAuthSchemeProvider implements S3AuthSchemeProvider {
 *     private final S3AuthSchemeProvider delegate;
 *
 *     public CustomSigningNameAuthSchemeProvider() {
 *         this.delegate = S3AuthSchemeProvider.defaultProvider();
 *     }
 *
 *     @Override
 *     public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
 *         List<AuthSchemeOption> options = delegate.resolveAuthScheme(authSchemeParams);
 *         return options.stream()
 *                       .map(option -> option.toBuilder()
 *                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "custom-service")
 *                                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME, "us-west-2")
 *                                            .build())
 *                       .collect(Collectors.toList());
 *     }
 * }
 * }
 *
 * <p>
 * <b>Creating Custom Auth Scheme Options</b>
 * <p>
 * You can create custom auth scheme options from scratch when implementing a custom {@link AuthSchemeProvider}.
 *
 * <p>
 * Example - Creating a custom auth scheme option:
 *
 * {@snippet :
 * public class CustomAuthSchemeProvider implements S3AuthSchemeProvider {
 *     @Override
 *     public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
 *         AuthSchemeOption customOption = AuthSchemeOption.builder()
 *             .schemeId("custom.auth#v1")
 *             .putSignerProperty(CustomHttpSigner.CUSTOM_HEADER, "custom-value")
 *             .putIdentityProperty(IdentityProperty.create(CustomAuthSchemeProvider.class, "AccountId"), "123456789")
 *             .build();
 *
 *         return Collections.singletonList(customOption);
 *     }
 * }
 * }
 *
 * <p>
 * <b>Reading Properties from Auth Scheme Options</b>
 * <p>
 * Within a custom {@link software.amazon.awssdk.http.auth.spi.signer.HttpSigner}, you can read properties from
 * the auth scheme option via the sign request.
 *
 * <p>
 * Example - Reading signer properties in a custom signer:
 *
 * {@snippet :
 * public class CustomHttpSigner implements HttpSigner<AwsCredentialsIdentity> {
 *     public static final SignerProperty<String> CUSTOM_HEADER =
 *         SignerProperty.create(CustomHttpSigner.class, "CustomHeader");
 *
 *     @Override
 *     public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
 *         // Read property that was set on the AuthSchemeOption
 *         String headerValue = request.property(CUSTOM_HEADER);
 *
 *         SdkHttpRequest signedRequest = request.request().toBuilder()
 *             .putHeader("X-Custom-Auth", headerValue)
 *             .build();
 *
 *         return SignedRequest.builder()
 *             .request(signedRequest)
 *             .payload(request.payload().orElse(null))
 *             .build();
 *     }
 * }
 * }
 *
 * @see AuthScheme
 * @see AuthSchemeProvider
 * @see SignerProperty
 * @see IdentityProperty
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
    @FunctionalInterface
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
    @FunctionalInterface
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
