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

package software.amazon.awssdk.core.internal.io;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.chunked.AwsChunkedEncodingConfig;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * A wrapper class of InputStream that implements chunked-encoding.
 */
@SdkInternalApi
public class AwsUnsignedChunkedEncodingInputStream extends AwsChunkedEncodingInputStream {

    private AwsUnsignedChunkedEncodingInputStream(InputStream in, AwsChunkedEncodingConfig awsChunkedEncodingConfig,
                                                  SdkChecksum sdkChecksum,
                                                  String checksumHeaderForTrailer) {
        super(in, sdkChecksum, checksumHeaderForTrailer, awsChunkedEncodingConfig);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Calculates the content length for a given Algorithm and header name.
     *
     * @param algorithm  Algorithm used.
     * @param headerName Header name.
     * @return Content length of the trailer that will be appended at the end.
     */
    public static long calculateChecksumContentLength(Algorithm algorithm, String headerName) {
        return headerName.length()
               + HEADER_COLON_SEPARATOR.length()
               + algorithm.base64EncodedLength().longValue()
               + CRLF.length() + CRLF.length();
    }

    /**
     *
     * @param originalContentLength Original Content length.
     * @return Calculatec Chunk Length with the chunk encoding format.
     */
    private static long calculateChunkLength(long originalContentLength) {
        return Long.toHexString(originalContentLength).length()
                + CRLF.length()
                + originalContentLength
                + CRLF.length();
    }

    public static long calculateStreamContentLength(long originalLength, long defaultChunkSize) {
        if (originalLength < 0 || defaultChunkSize == 0) {
            throw new IllegalArgumentException(originalLength + ", " + defaultChunkSize + "Args <= 0 not expected");
        }

        long maxSizeChunks = originalLength / defaultChunkSize;
        long remainingBytes = originalLength % defaultChunkSize;

        long allChunks = maxSizeChunks * calculateChunkLength(defaultChunkSize);
        long remainingInChunk = remainingBytes > 0 ? calculateChunkLength(remainingBytes) : 0;
        // last byte is composed of a "0" and "\r\n"
        long lastByteSize = 1 + (long) CRLF.length();

        return allChunks +  remainingInChunk + lastByteSize;
    }

    @Override
    protected byte[] createFinalChunk(byte[] finalChunk) {
        StringBuilder chunkHeader = new StringBuilder();
        // chunk-size
        chunkHeader.append(Integer.toHexString(finalChunk.length));
        chunkHeader.append(CRLF);
        return chunkHeader.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected byte[] createChunk(byte[] chunkData) {
        StringBuilder chunkHeader = new StringBuilder();
        // chunk-size
        chunkHeader.append(Integer.toHexString(chunkData.length));
        chunkHeader.append(CRLF);
        try {
            byte[] header = chunkHeader.toString().getBytes(StandardCharsets.UTF_8);
            byte[] trailer = CRLF.getBytes(StandardCharsets.UTF_8);
            byte[] chunk = new byte[header.length + chunkData.length + trailer.length];
            System.arraycopy(header, 0, chunk, 0, header.length);
            System.arraycopy(chunkData, 0, chunk, header.length, chunkData.length);
            System.arraycopy(trailer, 0,
                    chunk, header.length + chunkData.length,
                    trailer.length);
            return chunk;
        } catch (Exception e) {
            throw SdkClientException.builder()
                    .message("Unable to create chunked data. " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }

    @Override
    protected byte[] createChecksumChunkHeader() {
        StringBuilder chunkHeader = new StringBuilder();
        chunkHeader.append(checksumHeaderForTrailer)
                .append(HEADER_COLON_SEPARATOR)
                .append(BinaryUtils.toBase64(calculatedChecksum))
                .append(CRLF);
        return chunkHeader.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static final class Builder extends AwsChunkedEncodingInputStream.Builder<Builder> {
        public AwsUnsignedChunkedEncodingInputStream build() {
            return new AwsUnsignedChunkedEncodingInputStream(
                    this.inputStream, this.awsChunkedEncodingConfig,
                    this.sdkChecksum, this.checksumHeaderForTrailer);
        }
    }
}
