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
import software.amazon.awssdk.identity.spi.internal.StaticIdentityProvider;

/**
 * Interface for loading an {@link Identity}, which is used for authenticating with a service. Common identity types include
 * {@link AwsCredentialsIdentity} and {@link TokenIdentity}.
 *
 * <h2>Summary</h2>
 *
 * This is an interface providing client credentials - usually AWS credentials or bearer token credentials. See
 * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider} or
 * {@link software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider} from the
 * <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a> module for how you can configure
 * these credentials on your host environment.
 * <p>
 * If you want to configure credentials using code, you can implement this interface and return an
 * {@link AwsCredentialsIdentity} for AWS credentials or a {@link TokenIdentity} for bearer token credentials.
 * To avoid needing to implement this interface, the SDK provides numerous built-in implementations in its
 * <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>,
 * <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a> and
 * <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sso">{@code sso}</a> modules. See the documentation below
 * for more information.
 *
 * <h2>Identity Types</h2>
 *
 * <h3>{@link AwsCredentialsIdentity}</h3>
 *
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/security-creds.html">AWS credentials</a> allow accessing resources
 * protected by AWS SigV4, SigV4a or any other authentication mechanism that uses an AWS access key ID, AWS secret access key,
 * and (optionally) an AWS session token.
 * <p>
 * Most SDK-provided implementations of {@code IdentityProvider<}{@link AwsCredentialsIdentity}{@code >} are in the
 * <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>
 * module, with some residing in the <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a>
 * and <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sso">{@code sso}</a> modules:
 * <ul>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}</b>: Discovers credentials from the host
 *     environment. (Requires a dependency on 
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.StaticCredentialsProvider}</b>: Uses a hard-coded set of AWS
 *     credentials. (Requires a dependency on <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code
 *     auth}</a>). Alternatively, you can use {@link #staticAwsCredentials(AwsCredentialsIdentity)}.</li>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain}</b>: Allows chaining together
 *     multiple credential providers, failing over to the next one when one fails. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider}</b>: Allows anonymous access to some
 *     AWS resources, like public S3 objects. (Requires
 *     a dependency on <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.ProcessCredentialsProvider}</b>: Allows loading credentials from
 *     an external process. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider}</b>: Use
 *     <a href="https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRole.html">AWS's STS AssumeRole</a>
 *     API to assume an IAM role. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.services.sso.auth.SsoCredentialsProvider}</b>: Use
 *     <a href="https://docs.aws.amazon.com/singlesignon/latest/PortalAPIReference/API_GetRoleCredentials.html">
 *     AWS's Identity Center GetRoleCredentials</a> to assume an IAM role. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sso">{@code sso}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider}</b>: Use
 *     <a href="https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html">
 *     AWS's STS AssumeRoleWithWebIdentity</a> to assume an IAM role. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithSamlCredentialsProvider}</b>: Use
 *     <a href="https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithSAML.html">
 *     AWS's STS AssumeRoleWithSAML</a> to assume an IAM role. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider}</b>: Use
 *     <a href="https://docs.aws.amazon.com/STS/latest/APIReference/API_GetSessionToken.html">
 *     AWS's STS GetSessionToken</a> to exchange long-term credentials for short-term credentials. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.services.sts.auth.StsGetFederationTokenCredentialsProvider}</b>: Use
 *     <a href="https://docs.aws.amazon.com/STS/latest/APIReference/API_GetFederationToken.html">
 *     AWS's STS GetFederationToken</a> to exchange long-term user credentials for short-term credentials. (Requires a
 *     dependency on <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a>)</li>
 * </ul>
 * <p>
 * Implementations of {@code IdentityProvider<}{@link AwsCredentialsIdentity}{@code >} interface that are included in the
 * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}, but can also be used directly include:
 * <ul>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider}</b>: Uses credentials specified
 *     in JVM system properties. (Requires a
 *     dependency on <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider}</b>: Uses credentials
 *     specified in environment credentials. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider} </b>: Uses credentials from the ~/
 *     .aws/config and ~/.aws/credentials files. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider}</b>: Uses credentials from
 *     your EC2 instance profile configuration. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a> and for your
 *     application to be running in EC2.)</li>
 *     <li><b>{@link software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider}</b>: Uses credentials from your ECS
 *     or EKS configuration. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a> and for your application to be
 *     running in one of these environments.)</li>
 * </ul>
 *
 * <h3>{@link TokenIdentity}</h3>
 *
 * Token credentials allow accessing resources protected by bearer authentication.
 * <p>
 * SDK-provided implementations of {@code IdentityProvider<}{@link TokenIdentity}{@code >} are in the
 * <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>
 * module:
 *
 * <ul>
 *     <li><b>{@link software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider}</b>: Discovers bearer token
 *     from the host environment. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.auth.token.credentials.StaticTokenProvider}</b>: Uses a hard-coded bearer token.
 *     (Requires a dependency on <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>).
 *     Alternatively, you can use {@link #staticToken(TokenIdentity)}.</li>
 *     <li><b>{@link software.amazon.awssdk.auth.token.credentials.SdkTokenProviderChain}</b>: Allows chaining together
 *     multiple token providers, failing over to the next one when one fails. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 * </ul>
 * <p>
 * Implementations of {@code IdentityProvider<}{@link TokenIdentity}{@code >} interface that are included in the
 * {@link software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider}, but can also be used directly include:
 * <ul>
 *     <li><b>{@link software.amazon.awssdk.auth.token.credentials.ProfileTokenProvider}</b>: Uses credentials from the ~/
 *     .aws/config and ~/.aws/credentials files. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/auth">{@code auth}</a>)</li>
 * </ul>
 */
@SdkPublicApi
@ThreadSafe
public interface IdentityProvider<IdentityT extends Identity> {
    /**
     * Create an {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} that always returns the provided
     * identity.
     */
    static IdentityProvider<AwsCredentialsIdentity> staticAwsCredentials(AwsCredentialsIdentity identity) {
        return new StaticIdentityProvider<>(AwsCredentialsIdentity.class, identity);
    }

    /**
     * Create an {@link IdentityProvider}{@code <}{@link TokenIdentity}{@code >} that always returns the provided identity.
     */
    static IdentityProvider<TokenIdentity> staticToken(TokenIdentity identity) {
        return new StaticIdentityProvider<>(TokenIdentity.class, identity);
    }

    /**
     * Retrieve the class of identity this identity provider produces.
     * <p>
     * This is used by the SDK to determine which identity provider should be used to resolve a specific type of
     * identity.
     */
    Class<IdentityT> identityType();

    /**
     * Resolve the identity from this identity provider.
     *
     * @param request The request to resolve an identity.
     * @return A future that is completed with the resolved identity.
     */
    CompletableFuture<? extends IdentityT> resolveIdentity(ResolveIdentityRequest request);

    /**
     * Resolve the identity from this identity provider.
     * <p>
     * Similar to {@link #resolveIdentity(ResolveIdentityRequest)}, but takes a lambda to configure a new
     * {@link ResolveIdentityRequest.Builder}. This removes the need to call {@link ResolveIdentityRequest#builder()} and
     * {@link ResolveIdentityRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link ResolveIdentityRequest.Builder} will be given.
     * @return A future that is completed with the resolved identity.
     */
    default CompletableFuture<? extends IdentityT> resolveIdentity(Consumer<ResolveIdentityRequest.Builder> consumer) {
        return resolveIdentity(ResolveIdentityRequest.builder().applyMutation(consumer).build());
    }

    /**
     * Resolve the identity from this identity provider.
     * <p>
     * Similar to {@link #resolveIdentity(ResolveIdentityRequest)}, with no specific identity request parameters set.
     *
     * @return A future that is completed with the resolved identity.
     */
    default CompletableFuture<? extends IdentityT> resolveIdentity() {
        return resolveIdentity(ResolveIdentityRequest.builder().build());
    }
}
