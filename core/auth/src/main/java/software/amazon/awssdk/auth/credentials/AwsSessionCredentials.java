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

import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A special type of {@link AwsCredentials} that provides a session token to be used in service authentication. Session
 * tokens are typically provided by a token broker service, like AWS Security Token Service, and provide temporary access to an
 * AWS service.
 */
@Immutable
@SdkPublicApi
public final class AwsSessionCredentials implements AwsCredentials {

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;

    private AwsSessionCredentials(String accessKey, String secretKey, String sessionToken) {
        this.accessKeyId = Validate.paramNotNull(accessKey, "accessKey");
        this.secretAccessKey = Validate.paramNotNull(secretKey, "secretKey");
        this.sessionToken = Validate.paramNotNull(sessionToken, "sessionToken");
    }

    /**
     * Constructs a new session credentials object, with the specified AWS access key, AWS secret key and AWS session token.
     *
     * @param accessKey The AWS access key, used to identify the user interacting with AWS.
     * @param secretKey The AWS secret access key, used to authenticate the user interacting with AWS.
     * @param sessionToken The AWS session token, retrieved from an AWS token service, used for authenticating that this user has
     * received temporary permission to access some resource.
     */
    public static AwsSessionCredentials create(String accessKey, String secretKey, String sessionToken) {
        return new AwsSessionCredentials(accessKey, secretKey, sessionToken);
    }

    /**
     * Retrieve the AWS access key, used to identify the user interacting with AWS.
     */
    @Override
    public String accessKeyId() {
        return accessKeyId;
    }

    /**
     * Retrieve the AWS secret access key, used to authenticate the user interacting with AWS.
     */
    @Override
    public String secretAccessKey() {
        return secretAccessKey;
    }

    /**
     * Retrieve the AWS session token. This token is retrieved from an AWS token service, and is used for authenticating that this
     * user has received temporary permission to access some resource.
     */
    public String sessionToken() {
        return sessionToken;
    }

    @Override
    public String toString() {
        return ToString.builder("AwsSessionCredentials")
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

        AwsSessionCredentials that = (AwsSessionCredentials) o;
        return Objects.equals(accessKeyId, that.accessKeyId) &&
               Objects.equals(secretAccessKey, that.secretAccessKey) &&
               Objects.equals(sessionToken, that.sessionToken);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(accessKeyId());
        hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey());
        hashCode = 31 * hashCode + Objects.hashCode(sessionToken());
        return hashCode;
    }
}
