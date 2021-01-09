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

package software.amazon.awssdk.auth.signer;

import static software.amazon.awssdk.auth.signer.internal.SignerConstant.X_AMZ_CONTENT_SHA256;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.signer.internal.BaseAws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Exactly the same as {@link Aws4Signer} except if the request is being sent
 * over HTTPS, then it returns the string <code>UNSIGNED-PAYLOAD</code> as the
 * content SHA-256 so services that support it can avoid needing to calculate
 * the value when authorizing the request.
 * <p>
 * Payloads are still signed for requests over HTTP to preserve the request
 * integrity over a non-secure transport.
 */
@SdkPublicApi
public final class Aws4UnsignedPayloadSigner extends BaseAws4Signer {

    private Aws4UnsignedPayloadSigner() {
    }

    public static Aws4UnsignedPayloadSigner create() {
        return new Aws4UnsignedPayloadSigner();
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        request = addContentSha256Header(request);
        return super.sign(request, executionAttributes);
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, Aws4SignerParams signingParams) {
        request = addContentSha256Header(request);
        return super.sign(request, signingParams);
    }

    @Override
    protected String calculateContentHash(SdkHttpFullRequest.Builder mutableRequest, Aws4SignerParams signerParams) {
        if ("https".equals(mutableRequest.protocol())) {
            return "UNSIGNED-PAYLOAD";
        }
        return super.calculateContentHash(mutableRequest, signerParams);
    }

    private SdkHttpFullRequest addContentSha256Header(SdkHttpFullRequest request) {
        return request.toBuilder().putHeader(X_AMZ_CONTENT_SHA256, "required").build();
    }
}
