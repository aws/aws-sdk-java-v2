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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.ToString;

/**
 * An {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} that always returns an anonymous
 * {@link AwsCredentialsIdentity}. Anonymous AWS credentials result in un-authenticated requests, which will fail unless the API
 * or resource allows anonymous access.
 * <p>
 * This is useful to access:
 * <ol>
 *     <li>Public resources that you or another AWS customer owns, like objects in a public S3 bucket. This typically
 *     requires the owner of that resource to allow anonymous access using an appropriate
 *     <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_identity-vs-resource.html">
 *     identity or resource policy</a>.</li>
 *     <li>AWS APIs that do not require AWS credentials, like Amazon Cognito's
 *     <a href="https://docs.aws.amazon.com/cognitoidentity/latest/APIReference/API_GetCredentialsForIdentity.html">
 *     GetCredentialsForIdentity</a>.</li>
 * </ol>
 * <p>
 * To learn more about authentication with AWS, see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/id.html">AWS's
 * guide to identities</a>.
 * <p>
 * Create using {@link #create()}:
 * {@snippet :
 * AnonymousCredentialsProvider credentialsProvider =
 *    AnonymousCredentialsProvider.create(); // @link substring="create" target="#create()"
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 * }
 */
@SdkPublicApi
public final class AnonymousCredentialsProvider implements AwsCredentialsProvider {
    private static final String PROVIDER_NAME = "AnonymousCredentialsProvider";

    private AnonymousCredentialsProvider() {
    }

    /**
     * Create an {@link AnonymousCredentialsProvider}.
     *
     * <p>
     * {@snippet :
     * AnonymousCredentialsProvider credentialsProvider = AnonymousCredentialsProvider.create();
     * }
     */
    public static AnonymousCredentialsProvider create() {
        return new AnonymousCredentialsProvider();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return AwsBasicCredentials.builder()
                                  .validateCredentials(false)
                                  .providerName(PROVIDER_NAME)
                                  .build();
    }

    @Override
    public String toString() {
        return ToString.create(PROVIDER_NAME);
    }
}
