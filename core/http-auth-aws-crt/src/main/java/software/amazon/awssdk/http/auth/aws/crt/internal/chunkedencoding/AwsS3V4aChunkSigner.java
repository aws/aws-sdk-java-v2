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

package software.amazon.awssdk.http.auth.aws.crt.internal.chunkedencoding;

import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtHttpRequestConverter.toCrtStream;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningResult;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.auth.aws.chunkedencoding.AwsChunkSigner;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * An implementation of AwsChunkSigner that can calculate a Sigv4a compatible chunk signature.
 */
@SdkInternalApi
public class AwsS3V4aChunkSigner implements AwsChunkSigner {

    private static final int SIGNATURE_LENGTH = 144;

    private final AwsSigningConfig signingConfig;

    public AwsS3V4aChunkSigner(AwsSigningConfig signingConfig) {
        this.signingConfig = signingConfig;
    }

    public static int getSignatureLength() {
        return SIGNATURE_LENGTH;
    }

    @Override
    public String signChunk(byte[] chunkData, String previousSignature) {
        byte[] chunkSignature = signChunk(chunkData,
            previousSignature.getBytes(StandardCharsets.UTF_8),
            signingConfig);
        return new String(chunkSignature, StandardCharsets.UTF_8);
    }

    @Override
    public String signChecksumChunk(byte[] calculatedChecksum, String previousSignature, String checksumHeaderForTrailer) {
        AwsSigningResult awsSigningResult =
            signTrailerHeaders(
                Collections.singletonMap(checksumHeaderForTrailer,
                    Collections.singletonList(BinaryUtils.toBase64(calculatedChecksum))),
                previousSignature.getBytes(StandardCharsets.UTF_8),
                signingConfig);
        return awsSigningResult != null ? new String(awsSigningResult.getSignature(), StandardCharsets.UTF_8) : null;
    }

    private byte[] signChunk(byte[] chunkBody, byte[] previousSignature, AwsSigningConfig signingConfig) {
        HttpRequestBodyStream crtBody = toCrtStream(chunkBody);
        CompletableFuture<byte[]> future = AwsSigner.signChunk(crtBody, previousSignature, signingConfig);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("The thread got interrupted while attempting to sign request: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign request: " + e.getMessage(), e);
        }
    }

    private AwsSigningResult signTrailerHeaders(Map<String, List<String>> headerMap, byte[] previousSignature,
                                                AwsSigningConfig signingConfig) {

        List<HttpHeader> httpHeaderList =
            headerMap.entrySet().stream().map(entry -> new HttpHeader(
                entry.getKey(), String.join(",", entry.getValue()))).collect(Collectors.toList());

        // All the config remains the same as signing config except the Signature Type.
        AwsSigningConfig configCopy = signingConfig.clone();
        configCopy.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_TRAILING_HEADERS);
        CompletableFuture<AwsSigningResult> future = AwsSigner.sign(httpHeaderList, previousSignature, configCopy);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("The thread got interrupted while attempting to sign request: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign request: " + e.getMessage(), e);
        }
    }
}
