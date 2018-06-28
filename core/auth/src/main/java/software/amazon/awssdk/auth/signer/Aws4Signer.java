/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.internal.AbstractAws4Signer;
import software.amazon.awssdk.auth.signer.internal.Aws4SignerRequestParams;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Signer implementation that signs requests with the AWS4 signing protocol.
 */
@SdkPublicApi
public final class Aws4Signer extends AbstractAws4Signer<Aws4SignerParams, Aws4PresignerParams> {

    private Aws4Signer() {
    }

    public static Aws4Signer create() {
        return new Aws4Signer();
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        final Aws4SignerParams signingParams = extractSignerParams(Aws4SignerParams.builder(), executionAttributes)
            .build();

        return sign(request, signingParams);
    }

    public SdkHttpFullRequest sign(SdkHttpFullRequest request, Aws4SignerParams signingParams) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(signingParams.awsCredentials())) {
            return request;
        }

        final Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signingParams);

        return doSign(request, requestParams, signingParams).build();
    }

    @Override
    public SdkHttpFullRequest presign(SdkHttpFullRequest requestToSign, ExecutionAttributes executionAttributes) {
        Aws4PresignerParams signingParams = extractPresignerParams(Aws4PresignerParams.builder(),
                                                                   executionAttributes)
            .build();

        return presign(requestToSign, signingParams);
    }

    public SdkHttpFullRequest presign(SdkHttpFullRequest request, Aws4PresignerParams signingParams) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(signingParams.awsCredentials())) {
            return request;
        }

        final Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signingParams);

        return doPresign(request, requestParams, signingParams).build();
    }

    /**
     * Subclass could override this method to perform any additional procedure
     * on the request payload, with access to the result from signing the
     * header. (e.g. Signing the payload by chunk-encoding). The default
     * implementation doesn't need to do anything.
     */
    @Override
    protected void processRequestPayload(SdkHttpFullRequest.Builder mutableRequest,
                                         byte[] signature,
                                         byte[] signingKey,
                                         Aws4SignerRequestParams signerRequestParams,
                                         Aws4SignerParams signerParams) {
    }

    /**
     * Calculate the hash of the request's payload. In case of pre-sign, the
     * existing code would generate the hash of an empty byte array and returns
     * it. This method can be overridden by sub classes to provide different
     * values (e.g) For S3 pre-signing, the content hash calculation is
     * different from the general implementation.
     */
    @Override
    protected String calculateContentHashPresign(SdkHttpFullRequest.Builder mutableRequest,
                                                 Aws4PresignerParams signerParams) {
        return calculateContentHash(mutableRequest, signerParams);
    }
}