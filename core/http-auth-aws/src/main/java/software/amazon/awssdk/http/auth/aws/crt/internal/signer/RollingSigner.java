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

import java.io.InputStream;
import java.nio.ByteBuffer;
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

    private static byte[] signChunk(ByteBuffer chunkBody, byte[] previousSignature, AwsSigningConfig signingConfig) {
        // All the config remains the same as signing config except the Signature Type.
        AwsSigningConfig configCopy = signingConfig.clone();
        configCopy.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_CHUNK);
        configCopy.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.NONE);
        configCopy.setSignedBodyValue(null);

        HttpRequestBodyStream crtBody = new CrtInputStream(() -> new ByteBufferBackedInputStream(chunkBody));
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
        configCopy.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.NONE);
        configCopy.setSignedBodyValue(null);

        return CompletableFutureUtils.joinLikeSync(AwsSigner.sign(httpHeaderList, previousSignature, configCopy));
    }

    /**
     * Using a template that incorporates the previous calculated signature, sign the string and return it.
     */
    public byte[] sign(ByteBuffer chunkBody) {
        previousSignature = signChunk(chunkBody, previousSignature, signingConfig);
        return previousSignature;
    }

    public byte[] sign(Map<String, List<String>> headerMap) {
        AwsSigningResult result = signTrailerHeaders(headerMap, previousSignature, signingConfig);
        previousSignature = result != null ? result.getSignature() : new byte[0];
        return previousSignature;
    }

    public void reset() {
        previousSignature = seedSignature;
    }

    private static final class ByteBufferBackedInputStream extends InputStream {
        private final ByteBuffer buf;

        private ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        @Override
        public int read() {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        @Override
        public int read(byte[] bytes, int off, int len) {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }
}
