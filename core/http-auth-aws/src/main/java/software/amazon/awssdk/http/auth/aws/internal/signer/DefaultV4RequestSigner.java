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

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.AWS4_SIGNING_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.addHostHeader;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.deriveSigningKey;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.hashCanonicalRequest;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * The default implementation of a v4-request-signer. It performs each step of the SigV4 signing process, but does not add the
 * signature or auth information to the request itself.
 * <p>
 * All signing information, such as signature, signing key, canonical request, etc. is present in context object that is returned.
 * This can be used by the caller to add the auth info to the request, such as adding the signature as a query parameter or
 * building an authorization header using the signature and canonical request headers.
 */
@SdkInternalApi
public final class DefaultV4RequestSigner implements V4RequestSigner {

    private static final Logger LOG = Logger.loggerFor(DefaultV4RequestSigner.class);

    private final V4Properties properties;

    public DefaultV4RequestSigner(V4Properties properties) {
        this.properties = properties;
    }

    @Override
    public V4Context sign(SdkHttpRequest.Builder requestBuilder) {
        // Step 0: Pre-requisites
        String contentHash = getContentHash(requestBuilder);
        addHostHeader(requestBuilder);

        // Step 1: Create a canonical request
        V4CanonicalRequest canonicalRequest = createCanonicalRequest(requestBuilder.build(), contentHash);

        // Step 2: Create a hash of the canonical request
        String canonicalRequestHash = hashCanonicalRequest(canonicalRequest.getCanonicalRequestString());

        // Step 2: Create a hash of the canonical request
        String stringToSign = createSignString(canonicalRequestHash);

        // Step 4: Calculate the signature
        byte[] signingKey = createSigningKey();

        String signature = createSignature(stringToSign, signingKey);

        // Step 5: Return the signature to be added to the request
        return new V4Context(contentHash, signingKey, signature, canonicalRequest, requestBuilder);
    }

    private String getContentHash(SdkHttpRequest.Builder requestBuilder) {
        return requestBuilder.firstMatchingHeader(X_AMZ_CONTENT_SHA256).orElseThrow(
            () -> new IllegalArgumentException("Content hash must be present in the '" + X_AMZ_CONTENT_SHA256 + "' header!")
        );
    }

    private V4CanonicalRequest createCanonicalRequest(SdkHttpRequest request, String contentHash) {
        return new V4CanonicalRequest(request, contentHash, new V4CanonicalRequest.Options(
            properties.shouldDoubleUrlEncode(),
            properties.shouldNormalizePath()
        ));
    }

    private String createSignString(String canonicalRequestHash) {
        LOG.debug(() -> "AWS4 Canonical Request Hash: " + canonicalRequestHash);

        String stringToSign = AWS4_SIGNING_ALGORITHM +
                              SignerConstant.LINE_SEPARATOR +
                              properties.getCredentialScope().getDatetime() +
                              SignerConstant.LINE_SEPARATOR +
                              properties.getCredentialScope().scope() +
                              SignerConstant.LINE_SEPARATOR +
                              canonicalRequestHash;

        LOG.debug(() -> "AWS4 String to sign: " + stringToSign);
        return stringToSign;
    }

    private byte[] createSigningKey() {
        return deriveSigningKey(properties.getCredentials(), properties.getCredentialScope());
    }

    private String createSignature(String stringToSign, byte[] signingKey) {
        return BinaryUtils.toHex(SignerUtils.computeSignature(stringToSign, signingKey));
    }
}
