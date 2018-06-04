/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A special type of {@link AwsCredentials} that also provides a session token to be used in service authentication. Session
 * tokens are typically provided by a token broker service, like AWS Security Token Service, and provide temporary access to an
 * AWS service.
 */
public final class AwsSessionCredentials extends AwsCredentials {
    private final String sessionToken;

    private AwsSessionCredentials(String accessKey, String secretKey, String sessionToken) {
        super(accessKey, secretKey);
        this.sessionToken = Validate.notNull(sessionToken, "Session token cannot be null.");
    }

    /**
     * Constructs a new session credentials object, with the specified AWS access key, AWS secret key and AWS session token.
     *
     * @param accessKey The AWS access key, used to identify the user interacting with AWS.
     * @param secretKey The AWS secret access key, used to authenticate the user interacting with AWS.
     * @param sessionToken The AWS session token, retrieved from an AWS token service, used for authenticating that this user has
     *                     received temporary permission to access some resource.
     */
    public static AwsSessionCredentials create(String accessKey, String secretKey, String sessionToken) {
        return new AwsSessionCredentials(accessKey, secretKey, sessionToken);
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
        if (!super.equals(o)) {
            return false;
        }
        final AwsSessionCredentials that = (AwsSessionCredentials) o;
        return Objects.equals(sessionToken, that.sessionToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sessionToken);
    }
}
