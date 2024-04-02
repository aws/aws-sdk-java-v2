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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.identity.spi.internal.DefaultAwsSessionCredentialsIdentity;

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

    static AwsSessionCredentialsIdentity.Builder builder() {
        return DefaultAwsSessionCredentialsIdentity.builder();
    }

    /**
     * Constructs a new session credentials object, with the specified AWS access key, AWS secret key and AWS session token.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with services.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with services.
     * @param sessionToken The AWS session token, retrieved from an AWS token service, used for authenticating that this user has
     * received temporary permission to access some resource.
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
         * The AWS session token, retrieved from an AWS token service, used for authenticating that this user has
         * received temporary permission to access some resource.
         */
        Builder sessionToken(String sessionToken);

        @Override
        Builder providerName(String providerName);

        @Override
        AwsSessionCredentialsIdentity build();
    }
}
