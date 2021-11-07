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

package software.amazon.awssdk.auth.signer.internal.chunkedencoding;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.chunked.AwsChunkedEncodingConfig;
import software.amazon.awssdk.core.internal.io.AwsChunkedEncodingInputStream;

/**
 * A wrapper of InputStream that implements chunked encoding.
 * <p/>
 * Each chunk will be buffered for the calculation of the chunk signature
 * which is added at the head of each chunk. The request signature and the chunk signatures will
 * be assumed to be hex-encoded strings.
 * <p/>
 * This class will use the mark() & reset() of the wrapped InputStream if they
 * are supported, otherwise it will create a buffer for bytes read from
 * the wrapped stream.
 */
@SdkInternalApi
public final class AwsSignedChunkedEncodingInputStream extends AwsChunkedEncodingInputStream {

    private static final String CRLF = "\r\n";
    private static final String CHUNK_SIGNATURE_HEADER = ";chunk-signature=";
    private static final byte[] FINAL_CHUNK = new byte[0];

    private String previousChunkSignature;
    private String headerSignature;
    private final AwsChunkSigner chunkSigner;

    /**
     * Creates a chunked encoding input stream initialized with the originating stream, an http request seed signature
     * and a signer that can sign a chunk of bytes according to a chosen algorithm. The configuration allows
     * specification of the size of each chunk, as well as the buffer size. Use the same values as when
     * calculating total length of the stream {@link #calculateStreamContentLength(long, int, AwsChunkedEncodingConfig)}.
     *
     * @param in              The original InputStream.
     * @param headerSignature The signature of the signed headers of the request. This will be used for
     *                        calculating the signature of the first chunk. Observe that the format of
     *                        this parameter should be a hex-encoded string.
     * @param chunkSigner     The signer for each chunk of data, implementing the {@link AwsChunkSigner} interface.
     * @param config          The configuration allows the user to customize chunk size and buffer size.
     *                        See {@link AwsChunkedEncodingConfig} for default values.
     */
    private AwsSignedChunkedEncodingInputStream(InputStream in,
                                                String headerSignature,
                                                AwsChunkSigner chunkSigner,
                                                AwsChunkedEncodingConfig config) {
        super(in, config);
        this.chunkSigner = chunkSigner;
        this.previousChunkSignature = headerSignature;
        this.headerSignature = headerSignature;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AwsChunkedEncodingInputStream.Builder<Builder> {
        private AwsChunkSigner awsChunkSigner;
        private String headerSignature;


        /**
         * @param headerSignature The signature of the signed headers. This will be used for
         *                        calculating the signature of the first chunk
         * @return This builder for method chaining.
         */
        public Builder headerSignature(String headerSignature) {
            this.headerSignature = headerSignature;
            return this;
        }

        /**
         *
         * @param awsChunkSigner Chunk signer used to sign the data.
         * @return This builder for method chaining.
         */
        public Builder awsChunkSigner(AwsChunkSigner awsChunkSigner) {
            this.awsChunkSigner = awsChunkSigner;
            return this;
        }

        public AwsSignedChunkedEncodingInputStream build() {
            return new AwsSignedChunkedEncodingInputStream(this.inputStream(),
                                                           this.headerSignature,
                                                           this.awsChunkSigner,
                                                           this.chunkedEncodingConfig());
        }
    }

    /**
     * Calculates the expected total length of signed payload chunked stream.
     *
     * @param originalLength The length of the data
     * @param signatureLength The length of a calculated signature, dependent on which {@link AwsChunkSigner} is used
     * @param config The chunked encoding config determines the size of the chunks. Use the same values as when
     *               initializing the stream.
     */
    public static long calculateStreamContentLength(long originalLength,
                                                    int signatureLength,
                                                    AwsChunkedEncodingConfig config) {
        if (originalLength < 0) {
            throw new IllegalArgumentException("Nonnegative content length expected.");
        }
        int chunkSize = config.chunkSize();
        long maxSizeChunks = originalLength / chunkSize;
        long remainingBytes = originalLength % chunkSize;
        return maxSizeChunks * calculateSignedChunkLength(chunkSize, signatureLength)
                + (remainingBytes > 0 ? calculateSignedChunkLength(remainingBytes, signatureLength) : 0)
                + calculateSignedChunkLength(0, signatureLength);
    }

    private static long calculateSignedChunkLength(long chunkDataSize, int signatureLength) {
        return Long.toHexString(chunkDataSize).length()
                + CHUNK_SIGNATURE_HEADER.length()
                + signatureLength
                + CRLF.length()
                + chunkDataSize
                + CRLF.length();
    }

    private byte[] createSignedChunk(byte[] chunkData) {
        try {
            byte[] header = createSignedChunkHeader(chunkData);
            byte[] trailer = CRLF.getBytes(StandardCharsets.UTF_8);
            byte[] signedChunk = new byte[header.length + chunkData.length + trailer.length];
            System.arraycopy(header, 0, signedChunk, 0, header.length);
            System.arraycopy(chunkData, 0, signedChunk, header.length, chunkData.length);
            System.arraycopy(trailer, 0,
                    signedChunk, header.length + chunkData.length,
                    trailer.length);
            return signedChunk;
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to sign the chunked data. " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    private byte[] createSignedChunkHeader(byte[] chunkData) {
        String chunkSignature = chunkSigner.signChunk(chunkData, previousChunkSignature);
        previousChunkSignature = chunkSignature;

        StringBuilder chunkHeader = new StringBuilder();
        chunkHeader.append(Integer.toHexString(chunkData.length));
        chunkHeader.append(CHUNK_SIGNATURE_HEADER)
                   .append(chunkSignature)
                   .append(CRLF);
        return chunkHeader.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected byte[] createFinalChunk(byte[] finalChunk) {
        return createChunk(FINAL_CHUNK);
    }

    @Override
    protected byte[] createChunk(byte[] chunkData) {
        return createSignedChunk(chunkData);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        previousChunkSignature = headerSignature;
    }

}
