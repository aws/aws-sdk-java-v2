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

package software.amazon.awssdk.core.signer;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;

/**
 * Interface for the signer used for signing the requests. All SDK signer implementations will implement this interface.
 *
 * @deprecated Replaced by {@code software.amazon.awssdk.http.auth.spi.signer.HttpSigner} in 'http-auth-spi'.
 * <p>
 * <b>Migration Guide:</b>
 * <ul>
 * <li>For custom signing logic: Implement {@code HttpSigner} and configure via {@link AuthScheme}. See example 1 below.</li>
 * <li>For overriding signing properties only: Use custom {@link AuthSchemeProvider} instead. See example 2 below.</li>
 * </ul>
 * <p>
 * Example 1 - Custom signer implementation:
 * <p>
 * {@snippet :
 * S3AsyncClient s3 = S3AsyncClient.builder()
 *                                 .region(Region.US_WEST_2)
 *                                 .credentialsProvider(CREDENTIALS)
 *                                 .putAuthScheme(new CustomSigV4AuthScheme())
 *                                 .build();
 *
 * public class CustomSigV4AuthScheme implements AwsV4AuthScheme {
 *     @Override
 *     public String schemeId() {
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
 *
 *     private class CustomSigV4Signer implements AwsV4HttpSigner {
 *         @Override
 *         public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
 *             // Custom implementation
 *         }
 *
 *         @Override
 *         public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
 *             // Custom implementation
 *         }
 *     }
 * }
 *  }
 *
 * <p>
 * Example 2 - Overriding signer properties only:
 * <p>
 * {@snippet :
 * S3AsyncClient s3 = S3AsyncClient.builder()
 *                                 .region(Region.US_WEST_2)
 *                                 .credentialsProvider(CREDENTIALS)
 *                                 .authSchemeProvider(new CustomSigningNameAuthSchemeProvider())
 *                                 .build();
 *
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
 *                                            .build())
 *                       .collect(Collectors.toList());
 *     }
 * }
 *  }
 */
@SdkPublicApi
@FunctionalInterface
@Deprecated
public interface Signer {
    /**
     * Method that takes in an request and returns a signed version of the request.
     *
     * @param request The request to sign
     * @param executionAttributes Contains the attributes required for signing the request
     * @return A signed version of the input request
     */
    SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes);


    /**
     * Method that retrieves {@link CredentialType} i.e. the type of Credentials used by the Signer while authorizing a request.
     *
     * @return null by default else return {@link CredentialType} as defined by the signer implementation.
     */
    default CredentialType credentialType() {
        return null;
    }

}
