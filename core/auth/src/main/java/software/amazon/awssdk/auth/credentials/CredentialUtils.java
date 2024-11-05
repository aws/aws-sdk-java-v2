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

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkProtectedApi
public final class CredentialUtils {

    private CredentialUtils() {
    }

    /**
     * Determine whether the provided credentials are anonymous credentials, indicating that the customer is not attempting to
     * authenticate themselves.
     */
    public static boolean isAnonymous(AwsCredentials credentials) {
        return isAnonymous((AwsCredentialsIdentity) credentials);
    }

    /**
     * Determine whether the provided credentials are anonymous credentials, indicating that the customer is not attempting to
     * authenticate themselves.
     */
    public static boolean isAnonymous(AwsCredentialsIdentity credentials) {
        return credentials.secretAccessKey() == null && credentials.accessKeyId() == null;
    }

    /**
     * Converts an {@link AwsCredentialsIdentity} to {@link AwsCredentials}.
     *
     * <p>Usage of the new AwsCredentialsIdentity type is preferred over AwsCredentials. But some places may need to still
     * convert to the older AwsCredentials type to work with existing code.</p>
     *
     * <p>The conversion is only aware of {@link AwsCredentialsIdentity} and {@link AwsSessionCredentialsIdentity} types. If the
     * input is another sub-type that has other properties, they are not carried over. i.e.,
     * <ul>
     *     <li>AwsSessionCredentialsIdentity -> AwsSessionCredentials</li>
     *     <li>AwsCredentialsIdentity -> AwsBasicCredentials</li>
     * </ul>
     * </p>
     *
     * @param awsCredentialsIdentity The {@link AwsCredentialsIdentity} to convert
     * @return The corresponding {@link AwsCredentials}
     */
    public static AwsCredentials toCredentials(AwsCredentialsIdentity awsCredentialsIdentity) {
        if (awsCredentialsIdentity == null) {
            return null;
        }
        if (awsCredentialsIdentity instanceof AwsCredentials) {
            return (AwsCredentials) awsCredentialsIdentity;
        }

        // identity-spi defines 2 known types - AwsCredentialsIdentity and a sub-type AwsSessionCredentialsIdentity
        if (awsCredentialsIdentity instanceof AwsSessionCredentialsIdentity) {
            AwsSessionCredentialsIdentity awsSessionCredentialsIdentity = (AwsSessionCredentialsIdentity) awsCredentialsIdentity;
            return AwsSessionCredentials.builder()
                                        .accessKeyId(awsSessionCredentialsIdentity.accessKeyId())
                                        .secretAccessKey(awsSessionCredentialsIdentity.secretAccessKey())
                                        .sessionToken(awsSessionCredentialsIdentity.sessionToken())
                                        .accountId(awsSessionCredentialsIdentity.accountId().orElse(null))
                                        .build();
        }
        if (isAnonymous(awsCredentialsIdentity)) {
            return AnonymousCredentialsProvider.create().resolveCredentials();
        }
        return AwsBasicCredentials.builder()
                                  .accessKeyId(awsCredentialsIdentity.accessKeyId())
                                  .secretAccessKey(awsCredentialsIdentity.secretAccessKey())
                                  .accountId(awsCredentialsIdentity.accountId().orElse(null))
                                  .build();
    }

    /**
     * Converts an {@link IdentityProvider<? extends AwsCredentialsIdentity>} to {@link AwsCredentialsProvider} based on
     * {@link #toCredentials(AwsCredentialsIdentity)}.
     *
     * <p>Usage of the new IdentityProvider type is preferred over AwsCredentialsProvider. But some places may need to still
     * convert to the older AwsCredentialsProvider type to work with existing code.
     * </p>
     *
     * @param identityProvider The {@link IdentityProvider<? extends AwsCredentialsIdentity>} to convert
     * @return The corresponding {@link AwsCredentialsProvider}
     */
    public static AwsCredentialsProvider toCredentialsProvider(
            IdentityProvider<? extends AwsCredentialsIdentity> identityProvider) {
        if (identityProvider == null) {
            return null;
        }
        if (identityProvider instanceof AwsCredentialsProvider) {
            return (AwsCredentialsProvider) identityProvider;
        }
        return () -> {
            AwsCredentialsIdentity awsCredentialsIdentity =
                CompletableFutureUtils.joinLikeSync(identityProvider.resolveIdentity());
            return toCredentials(awsCredentialsIdentity);
        };
    }
}
