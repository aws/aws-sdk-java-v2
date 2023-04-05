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
 * A special type of {@link AwsCredentialsIdentity} that provides a session token to be used in service authentication. Session
 * tokens are typically provided by a token broker service, like AWS Security Token Service, and provide temporary access to an
 * AWS service.
 */
@SdkPublicApi
@ThreadSafe
public interface AwsSessionCredentialsIdentity extends AwsCredentialsIdentity {

    /**
     * Retrieve the AWS session token. This token is retrieved from an AWS token service, and is used for authenticating that this
     * user has received temporary permission to access some resource.
     */
    String sessionToken();

    /**
     * Constructs a new session credentials object, with the specified AWS access key, AWS secret key and AWS session token.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with services.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with services.
     * @param sessionToken The AWS session token, retrieved from an AWS token service, used for authenticating that this user has
     * received temporary permission to access some resource.
     */
    static AwsSessionCredentialsIdentity create(String accessKeyId, String secretAccessKey, String sessionToken) {
        Validate.paramNotNull(accessKeyId, "accessKeyId");
        Validate.paramNotNull(secretAccessKey, "secretAccessKey");
        Validate.paramNotNull(sessionToken, "sessionToken");

        return new AwsSessionCredentialsIdentity() {
            @Override
            public String accessKeyId() {
                return accessKeyId;
            }

            @Override
            public String secretAccessKey() {
                return secretAccessKey;
            }

            @Override
            public String sessionToken() {
                return sessionToken;
            }

            @Override
            public String toString() {
                return ToString.builder("AwsSessionCredentialsIdentity")
                               .add("accessKeyId", accessKeyId())
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

                AwsSessionCredentialsIdentity that = (AwsSessionCredentialsIdentity) o;
                return Objects.equals(accessKeyId, that.accessKeyId()) &&
                       Objects.equals(secretAccessKey, that.secretAccessKey()) &&
                       Objects.equals(sessionToken, that.sessionToken());
            }

            @Override
            public int hashCode() {
                int hashCode = 1;
                hashCode = 31 * hashCode + Objects.hashCode(accessKeyId());
                hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey());
                hashCode = 31 * hashCode + Objects.hashCode(sessionToken());
                return hashCode;
            }
        };
    }
}
