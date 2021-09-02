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

package software.amazon.awssdk.authcrt.signer.internal;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningResult;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * This class mirrors the publicly available API of the AwsSigner class in CRT.
 */
@SdkInternalApi
public class AwsCrt4aSigningAdapter {

    private final CrtHttpRequestConverter requestConverter;

    public AwsCrt4aSigningAdapter() {
        this.requestConverter = new CrtHttpRequestConverter();
    }

    public SdkHttpFullRequest signRequest(SdkHttpFullRequest request, AwsSigningConfig signingConfig) {
        HttpRequest crtRequest = requestConverter.requestToCrt(SigningUtils.sanitizeSdkRequestForCrtSigning(request));
        CompletableFuture<HttpRequest> future = AwsSigner.signRequest(crtRequest, signingConfig);
        try {
            HttpRequest signedRequest = future.get();
            return requestConverter.crtRequestToHttp(request, signedRequest);
        } catch (Exception e) {
            throw SdkClientException.builder()
                    .message("Unable to sign request: " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }

    public SdkSigningResult sign(SdkHttpFullRequest request, AwsSigningConfig signingConfig) {
        HttpRequest crtRequest = requestConverter.requestToCrt(SigningUtils.sanitizeSdkRequestForCrtSigning(request));
        CompletableFuture<AwsSigningResult> future = AwsSigner.sign(crtRequest, signingConfig);
        try {
            AwsSigningResult signingResult = future.get();
            return requestConverter.crtResultToAws(request, signingResult);
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to sign request: " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    public byte[] signChunk(byte[] chunkBody, byte[] previousSignature, AwsSigningConfig signingConfig) {
        HttpRequestBodyStream crtBody = requestConverter.toCrtStream(chunkBody);
        CompletableFuture<byte[]> future = AwsSigner.signChunk(crtBody, previousSignature, signingConfig);
        try {
            return future.get();
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to sign chunk: " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

}
