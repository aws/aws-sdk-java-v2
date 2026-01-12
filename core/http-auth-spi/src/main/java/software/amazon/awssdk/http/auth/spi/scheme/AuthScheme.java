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
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/**
 * An authentication scheme, composed of:
 * <ol>
 *     <li>A scheme ID - A unique identifier for the authentication scheme.</li>
 *     <li>An identity provider - An API that can be queried to acquire the customer's identity.</li>
 *     <li>A signer - An API that can be used to sign HTTP requests.</li>
 * </ol>
 *
 * <p>
 * Auth schemes are used to configure how requests are authenticated. The SDK provides built-in schemes like
 * {@code AwsV4AuthScheme} for AWS Signature Version 4, but you can implement custom schemes for specialized
 * authentication requirements.
 *
 * <p>
 * See example auth schemes defined <a href="https://smithy.io/2.0/spec/authentication-traits.html">here</a>.
 *
 * <p>
 * <b>Implementing a Custom Auth Scheme</b>
 * <p>
 * To implement a custom authentication scheme, you need to:
 * <ol>
 *     <li>Implement the {@link AuthScheme} interface</li>
 *     <li>Implement a custom {@link HttpSigner}</li>
 *     <li>Configure the scheme on the client builder</li>
 * </ol>
 *
 * <p>
 * Example - Custom authentication scheme with custom signer:
 *
 * {@snippet :
 * // Step 1: Implement custom signer
 * public class CustomHttpSigner implements HttpSigner<AwsCredentialsIdentity> {
 *     public static final SignerProperty<String> CUSTOM_HEADER =
 *         SignerProperty.create(CustomHttpSigner.class, "CustomHeader");
 *
 *     @Override
 *     public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
 *         String headerValue = request.property(CUSTOM_HEADER);
 *         SdkHttpRequest signedRequest = request.request().toBuilder()
 *             .putHeader("X-Custom-Auth", headerValue)
 *             .build();
 *         return SignedRequest.builder()
 *             .request(signedRequest)
 *             .payload(request.payload().orElse(null))
 *             .build();
 *     }
 *
 *     @Override
 *     public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
 *         // Async implementation
 *     }
 * }
 *
 * // Step 2: Implement custom auth scheme
 * public class CustomAuthScheme implements AwsV4AuthScheme {
 *     private static final String SCHEME_ID = "custom.auth#v1";
 *
 *     @Override
 *     public String schemeId() {
 *         return SCHEME_ID;
 *     }
 *
 *     @Override
 *     public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
 *         return providers.identityProvider(AwsCredentialsIdentity.class);
 *     }
 *
 *     @Override
 *     public AwsV4HttpSigner signer() {
 *         return new CustomHttpSigner();
 *     }
 * }
 *
 * // Step 3: Configure on client
 * S3AsyncClient s3 = S3AsyncClient.builder()
 *     .region(Region.US_WEST_2)
 *     .credentialsProvider(CREDENTIALS)
 *     .putAuthScheme(new CustomAuthScheme())
 *     .build();
 * }
 *
 * <p>
 * <b>Overriding Built-in Auth Schemes</b>
 * <p>
 * You can override built-in auth schemes by providing a custom implementation with the same scheme ID.
 * The custom scheme will take precedence over the default.
 *
 * <p>
 * Example - Overriding the default SigV4 scheme:
 *
 * {@snippet :
 * public class CustomSigV4AuthScheme implements AwsV4AuthScheme {
 *     @Override
 *     public String schemeId() {
 *         // Use the same scheme ID as the default SigV4 scheme
 *         return AwsV4AuthScheme.SCHEME_ID;
 *     }
 *
 *     @Override
 *     public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
 *         return providers.identityProvider(AwsCredentialsIdentity.class);
 *     }
 *
 *     @Override
 *     public AwsV4HttpSigner signer() {
 *         return new CustomSigV4Signer();
 *     }
 * }
 *
 * S3AsyncClient s3 = S3AsyncClient.builder()
 *     .region(Region.US_WEST_2)
 *     .credentialsProvider(CREDENTIALS)
 *     .putAuthScheme(new CustomSigV4AuthScheme())
 *     .build();
 * }
 *
 * @param <T> The type of the {@link Identity} used by this authentication scheme.
 * @see IdentityProvider
 * @see HttpSigner
 * @see AuthSchemeProvider
 */
@SdkPublicApi
public interface AuthScheme<T extends Identity> {

    /**
     * Retrieve the scheme ID, a unique identifier for the authentication scheme.
     */
    String schemeId();

    /**
     * Retrieve the identity provider associated with this authentication scheme. The identity generated by this provider is
     * guaranteed to be supported by the signer in this authentication scheme.
     * <p>
     * For example, if the scheme ID is aws.auth#sigv4, the provider returns an {@link AwsCredentialsIdentity}, if the scheme ID
     * is httpBearerAuth, the provider returns a {@link TokenIdentity}.
     * <p>
     * Note, the returned identity provider may differ from the type of identity provider retrieved from the provided
     * {@link IdentityProviders}.
     */
    IdentityProvider<T> identityProvider(IdentityProviders providers);

    /**
     * Retrieve the signer associated with this authentication scheme. This signer is guaranteed to support the identity generated
     * by the identity provider in this authentication scheme.
     */
    HttpSigner<T> signer();
}
