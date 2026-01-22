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

package software.amazon.awssdk.identity.spi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Interface for loading {@link Identity} that is used for authentication.
 *
 * <p>
 * Identity providers are responsible for resolving credentials, tokens, or other authentication identities
 * that are used by signers to authenticate requests. The SDK provides built-in identity providers for common
 * identity types like {@link AwsCredentialsIdentity} and {@link TokenIdentity}.
 *
 * <p>
 * <b>Common Built-in Identity Providers</b>
 * <ul>
 *     <li>{@code DefaultCredentialsProvider} - Resolves AWS credentials from the default credential chain</li>
 *     <li>{@code StaticCredentialsProvider} - Provides static AWS credentials</li>
 *     <li>{@code ProfileCredentialsProvider} - Resolves credentials from AWS profiles</li>
 *     <li>{@code StsAssumeRoleCredentialsProvider} - Assumes an IAM role using STS</li>
 * </ul>
 *
 * <p>
 * <b>How Identity Providers Work</b>
 * <p>
 * Identity providers are selected by {@link software.amazon.awssdk.http.auth.spi.scheme.AuthScheme}s based on the
 * identity type they produce. The SDK matches the identity type required by the auth scheme with the appropriate
 * provider from {@link IdentityProviders}.
 *
 * <p>
 * <b>Implementing a Custom Identity Provider</b>
 * <p>
 * You can implement custom identity providers for specialized authentication scenarios, such as retrieving
 * credentials from a custom credential store or implementing a custom token provider.
 *
 * <p>
 * Example - Custom credentials provider:
 *
 * {@snippet :
 * public class CustomCredentialsProvider implements IdentityProvider<AwsCredentialsIdentity> {
 *     @Override
 *     public Class<AwsCredentialsIdentity> identityType() {
 *         return AwsCredentialsIdentity.class;
 *     }
 *
 *     @Override
 *     public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
 *         // Retrieve credentials from custom source
 *         String accessKeyId = retrieveAccessKeyFromCustomStore();
 *         String secretAccessKey = retrieveSecretKeyFromCustomStore();
 *
 *         AwsCredentialsIdentity credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
 *         return CompletableFuture.completedFuture(credentials);
 *     }
 *
 *     private String retrieveAccessKeyFromCustomStore() {
 *         // Custom implementation
 *     }
 *
 *     private String retrieveSecretKeyFromCustomStore() {
 *         // Custom implementation
 *     }
 * }
 *
 * // Configure on client
 * S3Client s3 = S3Client.builder()
 *     .region(Region.US_WEST_2)
 *     .credentialsProvider(new CustomCredentialsProvider())
 *     .build();
 * }
 *
 * <p>
 * <b>Using Identity Properties</b>
 * <p>
 * Identity providers can read {@link IdentityProperty} values from the {@link ResolveIdentityRequest} to
 * customize identity resolution based on request-specific parameters.
 *
 * <p>
 * Example - Identity provider using properties:
 *
 * {@snippet :
 * public class RoleBasedCredentialsProvider implements IdentityProvider<AwsCredentialsIdentity> {
 *     public static final IdentityProperty<String> ROLE_ARN =
 *         IdentityProperty.create(RoleBasedCredentialsProvider.class, "RoleArn");
 *
 *     @Override
 *     public Class<AwsCredentialsIdentity> identityType() {
 *         return AwsCredentialsIdentity.class;
 *     }
 *
 *     @Override
 *     public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
 *         // Read property from request
 *         String roleArn = request.property(ROLE_ARN);
 *
 *         // Assume role and return credentials
 *         return assumeRoleAndGetCredentials(roleArn);
 *     }
 * }
 * }
 *
 * @see Identity
 * @see IdentityProviders
 * @see IdentityProperty
 * @see software.amazon.awssdk.http.auth.spi.scheme.AuthScheme
 */
@SdkPublicApi
@ThreadSafe
public interface IdentityProvider<IdentityT extends Identity> {
    /**
     * Retrieve the class of identity this identity provider produces.
     *
     * This is necessary for the SDK core to determine which identity provider should be used to resolve a specific type of
     * identity.
     */
    Class<IdentityT> identityType();

    /**
     * Resolve the identity from this identity provider.
     * @param request The request to resolve an Identity
     */
    CompletableFuture<? extends IdentityT> resolveIdentity(ResolveIdentityRequest request);

    /**
     * Resolve the identity from this identity provider.
     *
     * Similar to {@link #resolveIdentity(ResolveIdentityRequest)}, but takes a lambda to configure a new
     * {@link ResolveIdentityRequest.Builder}. This removes the need to call {@link ResolveIdentityRequest#builder()} and
     * {@link ResolveIdentityRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link ResolveIdentityRequest.Builder} will be given.
     */
    default CompletableFuture<? extends IdentityT> resolveIdentity(Consumer<ResolveIdentityRequest.Builder> consumer) {
        return resolveIdentity(ResolveIdentityRequest.builder().applyMutation(consumer).build());
    }

    /**
     * Resolve the identity from this identity provider.
     */
    default CompletableFuture<? extends IdentityT> resolveIdentity() {
        return resolveIdentity(ResolveIdentityRequest.builder().build());
    }
}
