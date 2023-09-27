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

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningResult;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.auth.aws.crt.internal.io.CrtInputStream;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * A class which calculates a rolling signature of arbitrary data using HMAC-SHA256. Each time a signature is calculated, the
 * prior calculation is incorporated, hence "rolling".
 */
@SdkInternalApi
public final class RollingSigner {

    private final byte[] seedSignature;
    private final AwsSigningConfig signingConfig;
    private byte[] previousSignature;

    public RollingSigner(byte[] seedSignature, AwsSigningConfig signingConfig) {
        this.seedSignature = seedSignature.clone();
        this.previousSignature = seedSignature.clone();
        this.signingConfig = signingConfig;
    }

    private static byte[] signChunk(byte[] chunkBody, byte[] previousSignature, AwsSigningConfig signingConfig) {
        // All the config remains the same as signing config except the Signature Type.
        AwsSigningConfig configCopy = signingConfig.clone();
        configCopy.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_CHUNK);

        HttpRequestBodyStream crtBody = new CrtInputStream(() -> new ByteArrayInputStream(chunkBody));
        return CompletableFutureUtils.joinLikeSync(AwsSigner.signChunk(crtBody, previousSignature, configCopy));
    }

    private static AwsSigningResult signTrailerHeaders(Map<String, List<String>> headerMap, byte[] previousSignature,
                                                       AwsSigningConfig signingConfig) {

        List<HttpHeader> httpHeaderList =
            headerMap.entrySet().stream().map(entry -> new HttpHeader(
                entry.getKey(), String.join(",", entry.getValue()))).collect(Collectors.toList());

        // All the config remains the same as signing config except the Signature Type.
        AwsSigningConfig configCopy = signingConfig.clone();
        configCopy.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_TRAILING_HEADERS);

        return CompletableFutureUtils.joinLikeSync(AwsSigner.sign(httpHeaderList, previousSignature, configCopy));
    }

    /**
     * Using a template that incorporates the previous calculated signature, sign the string and return it.
     */
    public byte[] sign(byte[] chunkBody) {
        return signChunk(chunkBody, previousSignature, signingConfig);
    }

    public byte[] sign(Map<String, List<String>> headerMap) {
        AwsSigningResult result = signTrailerHeaders(headerMap, previousSignature, signingConfig);
        return result != null ? result.getSignature() : new byte[0];
    }

    public void reset() {
        previousSignature = seedSignature;
    }
}
