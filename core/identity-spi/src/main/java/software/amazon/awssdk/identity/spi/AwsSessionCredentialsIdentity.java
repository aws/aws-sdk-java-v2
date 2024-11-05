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

import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.identity.spi.internal.DefaultAwsSessionCredentialsIdentity;

/**
 * Temporary {@link AwsCredentialsIdentity}, with a session token.
 * <p>
 * AWS security best-practices recommended using
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_temp_use-resources.html">
 * temporary AWS credentials</a> returned by a token broker service like AWS Security Token Service (STS), instead of
 * a non-temporary {@link AwsCredentialsIdentity} like that of an AWS user.
 * <p>
 * This can be created using {@link AwsSessionCredentialsIdentity#create} or {@link AwsSessionCredentialsIdentity#builder()}. SDK
 * methods usually accept a {@link AwsCredentialsProvider}, not {@code AwsSessionCredentials}. To use fixed/unchanging
 * credentials with
 * these APIs, see {@link StaticCredentialsProvider}.
 */
@SdkPublicApi
@ThreadSafe
public interface AwsSessionCredentialsIdentity extends AwsCredentialsIdentity {
    /**
     * The AWS session token, used to authenticate that this user has received temporary permission to access some
     * resource.
     */
    String sessionToken();

    /**
     * Create a builder for AWS session credentials.
     */
    static AwsSessionCredentialsIdentity.Builder builder() {
        return DefaultAwsSessionCredentialsIdentity.builder();
    }

    /**
     * Constructs a new session credentials object, with the specified AWS access key, AWS secret key and AWS session token. To
     * specify your AWS account ID, use {@link #builder()}.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with the service.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with the service.
     * @param sessionToken The AWS session token, used to authenticate that this user has received temporary permission to access some
     * resource.
     */
    static AwsSessionCredentialsIdentity create(String accessKeyId, String secretAccessKey, String sessionToken) {
        return builder().accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .sessionToken(sessionToken)
                        .build();
    }

    interface Builder extends AwsCredentialsIdentity.Builder {
        @Override
        Builder accessKeyId(String accessKeyId);

        @Override
        Builder secretAccessKey(String secretAccessKey);

        /**
         * The AWS session token, used to authenticate that this user has received temporary permission to access some
         * resource.
         */
        Builder sessionToken(String sessionToken);

        @Override
        Builder accountId(String accountId);

        /**
         * (Optional) The time after which this identity is no longer valid. When not specified, the identity may
         * still expire at some unknown time in the future.
         */
        default Builder expirationTime(Instant expirationTime) {
            return this;
        }

        @Override
        Builder providerName(String providerName);

        @Override
        AwsSessionCredentialsIdentity build();
    }
}
