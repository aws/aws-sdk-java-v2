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

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * A container for data produced during and as a result of the SigV4 request signing process.
 */
@SdkProtectedApi
@Immutable
public final class SigV4Context {
    private final String contentHash;
    private final byte[] signingKey;
    private final String signature;
    private final SdkHttpRequest signedRequest;

    public SigV4Context(String contentHash, byte[] signingKey, String signature,
                        SdkHttpRequest signedRequest) {
        this.contentHash = contentHash;
        this.signingKey = signingKey.clone();
        this.signature = signature;
        this.signedRequest = signedRequest;
    }

    public String getContentChecksum() {
        return contentHash;
    }

    public byte[] getSigningKey() {
        return signingKey.clone();
    }

    public String getSignature() {
        return signature;
    }

    public SdkHttpRequest getSignedRequest() {
        return signedRequest;
    }
}

