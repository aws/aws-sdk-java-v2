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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

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
     * Constructs a new credentials object, with the specified AWS access key and AWS secret key.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with services.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with services.
     */
    static AwsCredentialsIdentity create(String accessKeyId, String secretAccessKey) {
        Validate.paramNotNull(accessKeyId, "accessKeyId");
        Validate.paramNotNull(secretAccessKey, "secretAccessKey");

        return new AwsCredentialsIdentity() {
            @Override
            public String accessKeyId() {
                return accessKeyId;
            }

            @Override
            public String secretAccessKey() {
                return secretAccessKey;
            }

            @Override
            public String toString() {
                return ToString.builder("AwsCredentialsIdentity")
                               .add("accessKeyId", accessKeyId)
                               .build();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                AwsCredentialsIdentity that = (AwsCredentialsIdentity) o;
                return Objects.equals(accessKeyId, that.accessKeyId()) &&
                       Objects.equals(secretAccessKey, that.secretAccessKey());
            }

            @Override
            public int hashCode() {
                int hashCode = 1;
                hashCode = 31 * hashCode + Objects.hashCode(accessKeyId());
                hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey());
                return hashCode;
            }
        };
    }
}
