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

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.checksum.ContentChecksum;
import software.amazon.awssdk.http.auth.aws.util.CanonicalRequestV2;

/**
 * A container for data produced during and as a result of the SigV4 request signing process.
 */
@SdkProtectedApi
public final class SigV4RequestContext {
    private final ContentChecksum contentChecksum;
    private final CanonicalRequestV2 canonicalRequest;
    private final String canonicalRequestHash;
    private final String stringToSign;
    private final byte[] signingKey;
    private final String signature;
    private final SdkHttpRequest signedRequest;

    public SigV4RequestContext(ContentChecksum contentChecksum, CanonicalRequestV2 canonicalRequest, String canonicalRequestHash,
                               String stringToSign,
                               byte[] signingKey, String signature, SdkHttpRequest signedRequest) {
        this.contentChecksum = contentChecksum;
        this.canonicalRequest = canonicalRequest;
        this.canonicalRequestHash = canonicalRequestHash;
        this.stringToSign = stringToSign;
        this.signingKey = signingKey.clone();
        this.signature = signature;
        this.signedRequest = signedRequest;
    }

    public ContentChecksum getContentChecksum() {
        return contentChecksum;
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

