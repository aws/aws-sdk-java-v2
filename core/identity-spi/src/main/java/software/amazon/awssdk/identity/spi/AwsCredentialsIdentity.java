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
 * Provides access to the AWS credentials used for accessing services: AWS access key ID and secret access key. These
 * credentials are used to securely sign requests to services (e.g., AWS services) that use them for authentication.
 *
 * <p>For more details on AWS access keys, see:
 * <a href="https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys">
 * https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys</a></p>
 *
 * @see AwsSessionCredentialsIdentity
 */
@SdkPublicApi
@ThreadSafe
public interface AwsCredentialsIdentity extends Identity {

    /**
     * Retrieve the AWS access key, used to identify the user interacting with services.
     */
    String accessKeyId();

    /**
     * Retrieve the AWS secret access key, used to authenticate the user interacting with services.
     */
    String secretAccessKey();

    /**
     * Retrieve the AWS region, if set, of the single-region account. Otherwise, returns empty {@link Optional}.
     */
    default Optional<String> credentialScope() {
        return Optional.empty();
    }

    static Builder builder() {
        return DefaultAwsCredentialsIdentity.builder();
    }

    /**
     * Constructs a new credentials object, with the specified AWS access key and AWS secret key.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with services.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with services.
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
         * The AWS region of the single-region account.
         */
        Builder credentialScope(String credentialScope);

        AwsCredentialsIdentity build();
    }
}
