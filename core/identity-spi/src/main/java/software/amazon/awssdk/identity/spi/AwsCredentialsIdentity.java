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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.identity.spi.internal.DefaultAwsCredentialsIdentity;

/**
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/security-creds.html">AWS credentials</a>
 * allow accessing resources protected by AWS SigV4, SigV4a or any other authentication mechanism that uses an
 * AWS access key ID, AWS secret access key, and (optionally with {@link AwsSessionCredentialsIdentity}) an AWS session token.
 *
 * <p>
 * {@code AwsCredentialsIdentity} is intended to be an immutable container for credentials. For more information on using
 * credentials with the AWS SDK, see {@link IdentityProvider}.
 *
 * <p>
 * If your credentials contain a session token, you should use {@link AwsSessionCredentialsIdentity}. If you also configure your
 * identity with an {@link Builder#accountId(String)}, the SDK may be able to route your requests to lower-latency,
 * higher-availability endpoints.
 *
 * <p>
 * {@snippet :
 * // Create credentials without an account ID
 * AwsCredentialsIdentity awsCredentials =
 *     AwsCredentialsIdentity.create("accessKeyId", "secretAccessKey");
 *
 * AwsSessionCredentialsIdentity awsSesssionCredentials =
 *     AwsSessionCredentialsIdentity.create("accessKeyId", "secretAccessKey", "sessionToken");
 *
 * // Create credentials with an account ID (may improve performance or availability for some services)
 * AwsCredentialsIdentity awsCredentials =
 *     AwsCredentialsIdentity.builder()
 *                           .accessKeyId("accessKeyId")
 *                           .secretAccessKey("secretAccessKey")
 *                           .accountId("accountId")
 *                           .build();
 *
 * AwsSessionCredentialsIdentity awsSesssionCredentials =
 *     AwsSessionCredentialsIdentity.builder()
 *                                  .accessKeyId("accessKeyId")
 *                                  .secretAccessKey("secretAccessKey")
 *                                  .sessionToken("sessionToken")
 *                                  .accountId("accountId")
 *                                  .build();
 *}
 */
@SdkPublicApi
@ThreadSafe
public interface AwsCredentialsIdentity extends Identity {
    /**
     * The AWS access key, used to identify the user interacting with services.
     */
    String accessKeyId();

    /**
     * The AWS secret access key, used to authenticate the user interacting with services.
     */
    String secretAccessKey();

    /**
     * (Optional) The AWS account id associated with this credential identity. Specifying this value may improve performance
     * or availability for some services.
     */
    default Optional<String> accountId() {
        return Optional.empty();
    }

    /**
     * Create a builder for AWS credentials.
     */
    static Builder builder() {
        return DefaultAwsCredentialsIdentity.builder();
    }

    /**
     * Constructs a new credentials object, with the specified AWS access key and AWS secret key. To specify your AWS account
     * ID, use {@link #builder()}.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with the service.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with the service.
     */
    static AwsCredentialsIdentity create(String accessKeyId, String secretAccessKey) {
        return builder().accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .build();
    }

    interface Builder {
        /**
         * The AWS access key, used to identify the user interacting with services.
         */
        Builder accessKeyId(String accessKeyId);

        /**
         * The AWS secret access key, used to authenticate the user interacting with services.
         */
        Builder secretAccessKey(String secretAccessKey);

        /**
         * (Optional) The AWS account id associated with this credential identity. Specifying this value may improve performance
         * or availability for some services.
         */
        Builder accountId(String accountId);

        /**
         * (Optional) The name of the identity provider that created this credential identity. This value should only be
         * specified by standard providers. If you're creating your own identity or provider, you should not configure this
         * value.
         */
        default Builder providerName(String providerName) {
            return this;
        }

        AwsCredentialsIdentity build();
    }
}
