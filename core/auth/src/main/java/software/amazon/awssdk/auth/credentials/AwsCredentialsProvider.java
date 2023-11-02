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
 *
 * <p>Commonly-used implementations include {@link StaticCredentialsProvider} for a fixed set of credentials and the
 * {@link DefaultCredentialsProvider} for discovering credentials from the host's environment. The AWS Security Token
 * Service (STS) client also provides implementations of this interface for loading temporary, limited-privilege credentials from
 * AWS STS.</p>
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
