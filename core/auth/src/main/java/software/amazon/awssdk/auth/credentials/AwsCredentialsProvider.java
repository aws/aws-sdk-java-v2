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

package software.amazon.awssdk.auth.credentials;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * Interface for loading {@link AwsCredentials} that are used for authentication.
 * <p>
 * See our <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html">credentials
 * documentation</a> for more information.
 * <p>
 * The most common implementations of this interface include:
 * <ul>
 *     <li>{@link DefaultCredentialsProvider}: Discovers credentials from the host environment.</li>
 *     <li>{@link StaticCredentialsProvider}: Uses a hard-coded set of AWS credentials for an
 *         <a href="https://docs.aws.amazon.com/iam/">IAM</a> user or role.</li>
 *     <li>{@link ProcessCredentialsProvider}: Allows loading credentials from an external process.</li>
 *     <li>{@code software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider}: Use
 *         <a href="https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRole.html">AWS's STS AssumeRole</a>
 *         API to assume an <a href="https://docs.aws.amazon.com/iam/">IAM</a> role. (Requires a dependency on 'sts')</li>
 *     <li>{@code software.amazon.awssdk.services.sso.auth.SsoCredentialsProvider}: Use
 *         <a href="https://docs.aws.amazon.com/singlesignon/latest/PortalAPIReference/API_GetRoleCredentials.html">
 *         AWS's Identity Center GetRoleCredentials</a> to assume an <a href="https://docs.aws.amazon.com/iam/">IAM</a>
 *         role. (Requires a dependency on 'sso')</li>
 * </ul>
 * <p>
 * Implementations of this interface that are included in the {@link DefaultCredentialsProvider}, but can also be used
 * directly include:
 * <ul>
 *     <li>{@link EnvironmentVariableCredentialsProvider}: Uses credentials specified in environment credentials.</li>
 *     <li>{@link SystemPropertyCredentialsProvider}: Uses credentials specified using JVM system properties.</li>
 *     <li>{@link ProfileCredentialsProvider} : Uses credentials from the ~/.aws/config and ~/.aws/credentials files.</li>
 *     <li>{@link InstanceProfileCredentialsProvider}: Uses credentials from your EC2 instance profile configuration.
 *     Requires your application to be running in EC2.</li>
 *     <li>{@link ContainerCredentialsProvider}: Uses credentials from your ECS, EKS or GreenGrass configuration.
 *     Requires your application to be running in one of these environments.</li>
 * </ul>
 * <p>
 * Some special use-case implementation of this interface include:
 * <ul>
 *     <li>{@link AnonymousCredentialsProvider}: Allows anonymous access to some AWS resources.</li>
 *     <li></li>
 * </ul>
 */
@FunctionalInterface
@SdkPublicApi
public interface AwsCredentialsProvider extends IdentityProvider<AwsCredentialsIdentity> {
    /**
     * Returns {@link AwsCredentials} that can be used to authorize an AWS request. Each implementation of AWSCredentialsProvider
     * can choose its own strategy for loading credentials. For example, an implementation might load credentials from an existing
     * key management system, or load new credentials when credentials are rotated.
     *
     * <p>If an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be
     * raised.</p>
     *
     * @return AwsCredentials which the caller can use to authorize an AWS request.
     */
    AwsCredentials resolveCredentials();

    @Override
    default Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    default CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
        return CompletableFuture.completedFuture(resolveCredentials());
    }
}
