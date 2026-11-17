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

package software.amazon.awssdk.auth.credentials.internal.daemon;

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

/**
 * Credentials returned by the credential daemon, with the time they expire.
 */
@SdkInternalApi
public final class CredentialDaemonResponse {
    private final AwsCredentials credentials;
    private final Instant expiresAt;

    CredentialDaemonResponse(AwsCredentials credentials, Instant expiresAt) {
        this.credentials = credentials;
        this.expiresAt = expiresAt;
    }

    public AwsCredentials credentials() {
        return credentials;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
