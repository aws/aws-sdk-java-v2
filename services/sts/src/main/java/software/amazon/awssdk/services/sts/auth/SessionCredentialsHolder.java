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

package software.amazon.awssdk.services.sts.auth;

import java.util.Date;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * Holder class used to atomically store a session with its expiration time.
 */
@SdkInternalApi
@ThreadSafe
final class SessionCredentialsHolder {

    private final AwsSessionCredentials sessionCredentials;
    private final Date sessionCredentialsExpiration;

    SessionCredentialsHolder(Credentials credentials) {
        this.sessionCredentials = AwsSessionCredentials.create(credentials.accessKeyId(),
                                                            credentials.secretAccessKey(),
                                                            credentials.sessionToken());
        this.sessionCredentialsExpiration = Date.from(credentials.expiration());
    }

    public AwsSessionCredentials getSessionCredentials() {
        return sessionCredentials;
    }

    public Date getSessionCredentialsExpiration() {
        return sessionCredentialsExpiration;
    }
}
