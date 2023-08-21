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

package software.amazon.awssdk.http.auth.aws.signer;

import java.time.Clock;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;


/**
 * A class which contains "properties" relevant to SigV4. These properties can be derived {@link SignerProperty}'s on a
 * {@link SignRequest}.
 */
@SdkProtectedApi
@Immutable
// TODO: change to builder pattern
public final class V4Properties {
    private final AwsCredentialsIdentity credentials;
    private final CredentialScope credentialScope;
    private final Clock signingClock;
    private final boolean doubleUrlEncode;
    private final boolean normalizePath;


    public V4Properties(AwsCredentialsIdentity credentials, CredentialScope credentialScope,
                        Clock signingClock, boolean doubleUrlEncode, boolean normalizePath) {
        this.credentials = credentials;
        this.credentialScope = credentialScope;
        this.signingClock = signingClock;
        this.doubleUrlEncode = doubleUrlEncode;
        this.normalizePath = normalizePath;
    }

    public AwsCredentialsIdentity getCredentials() {
        return credentials;
    }

    public CredentialScope getCredentialScope() {
        return credentialScope;
    }

    public Clock getSigningClock() {
        return signingClock;
    }

    public boolean shouldDoubleUrlEncode() {
        return doubleUrlEncode;
    }

    public boolean shouldNormalizePath() {
        return normalizePath;
    }
}
