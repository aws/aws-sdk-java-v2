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

package software.amazon.awssdk.services.sso.internal;

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

/**
 * Holder class used to atomically store a session with its expiration time.
 */
@SdkInternalApi
@ThreadSafe
public final class SessionCredentialsHolder {

    private final AwsSessionCredentials sessionCredentials;
    private final Instant sessionCredentialsExpiration;

    public SessionCredentialsHolder(AwsSessionCredentials credentials, Instant expiration) {
        this.sessionCredentials = credentials;
        this.sessionCredentialsExpiration = expiration;
    }

    public AwsSessionCredentials sessionCredentials() {
        return sessionCredentials;
    }

    public Instant sessionCredentialsExpiration() {
        return sessionCredentialsExpiration;
    }
}
