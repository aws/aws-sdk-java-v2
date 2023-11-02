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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * A container for data produced during and as a result of the SigV4 request signing process.
 */
@SdkInternalApi
// TODO(sra-identity-auth): This is currently not @Immutable because signedRequest is a Builder. Is Builder needed? If it could
//  hold reference to SdkHttpRequest instead, this class would be @Immutable.
public final class V4RequestSigningResult {
    private final String contentHash;
    private final byte[] signingKey;
    private final String signature;
    private final V4CanonicalRequest canonicalRequest;
    private final SdkHttpRequest.Builder signedRequest;

    public V4RequestSigningResult(String contentHash, byte[] signingKey, String signature,
                                  V4CanonicalRequest canonicalRequest, SdkHttpRequest.Builder signedRequest) {
        this.contentHash = contentHash;
        this.signingKey = signingKey.clone();
        this.signature = signature;
        this.canonicalRequest = canonicalRequest;
        this.signedRequest = signedRequest;
    }

    public String getContentHash() {
        return contentHash;
    }

    public byte[] getSigningKey() {
        return signingKey.clone();
    }

    public String getSignature() {
        return signature;
    }

    public V4CanonicalRequest getCanonicalRequest() {
        return canonicalRequest;
    }

    public SdkHttpRequest.Builder getSignedRequest() {
        return signedRequest;
    }
}

