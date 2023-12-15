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

package software.amazon.awssdk.http.auth.aws.crt.internal.signer;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * A container for data produced during and as a result of the SigV4a request signing with CRT.
 */
@SdkInternalApi
public final class V4aRequestSigningResult {
    private final SdkHttpRequest.Builder signedRequest;
    private final byte[] signature;
    private final AwsSigningConfig signingConfig;

    public V4aRequestSigningResult(SdkHttpRequest.Builder signedRequest, byte[] signature, AwsSigningConfig signingConfig) {
        this.signedRequest = signedRequest;
        this.signature = signature.clone();
        this.signingConfig = signingConfig;
    }

    public SdkHttpRequest.Builder getSignedRequest() {
        return signedRequest;
    }

    public byte[] getSignature() {
        return signature;
    }

    public AwsSigningConfig getSigningConfig() {
        return signingConfig;
    }
}
