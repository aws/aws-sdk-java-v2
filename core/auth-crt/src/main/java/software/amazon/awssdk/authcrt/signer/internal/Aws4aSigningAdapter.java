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
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * This class mirrors the publicly available API of the AwsSigner class in CRT.
 */
@SdkInternalApi
public class Aws4aSigningAdapter {

    private final CrtHttpRequestConverter requestConverter;

    public Aws4aSigningAdapter() {
        requestConverter = new CrtHttpRequestConverter();
    }

    public SdkHttpFullRequest signWithCrt(SdkHttpFullRequest request, AwsSigningConfig signingConfig) {
        HttpRequest crtRequest = requestConverter.createCrtRequest(SigningUtils.sanitizeSdkRequestForCrtSigning(request));
        CompletableFuture<HttpRequest> future = AwsSigner.signRequest(crtRequest, signingConfig);
        try {
            HttpRequest signedRequest = future.get();
            return requestConverter.createSignedSdkRequest(request, signedRequest);
        } catch (Exception e) {
            throw SdkClientException.builder()
                    .message("Unable to sign request: " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }

}
