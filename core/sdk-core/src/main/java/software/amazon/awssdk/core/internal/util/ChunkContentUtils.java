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

package software.amazon.awssdk.core.internal.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkInternalApi
public final class ChunkContentUtils {

    public static final String HEADER_COLON_SEPARATOR = ":";
    public static final String ZERO_BYTE = "0";
    public static final String CRLF = "\r\n";

    public static final String LAST_CHUNK = ZERO_BYTE + CRLF;
    public static final long LAST_CHUNK_LEN = LAST_CHUNK.length();

    private ChunkContentUtils() {
    }

    /**
     * The chunk format is: chunk-size CRLF chunk-data CRLF.
     *
     * @param originalContentLength Original Content length.
     * @return the length of this chunk
     */
    public static long calculateChunkLength(long originalContentLength) {
        if (originalContentLength == 0) {
            return 0;
        }
        return Long.toHexString(originalContentLength).length()
               + CRLF.length()
               + originalContentLength
               + CRLF.length();
    }

    /**
     * Calculates the content length for data that is divided into chunks.
     *
     * @param originalLength  original content length.
     * @param chunkSize chunk size
     * @return Content length of the trailer that will be appended at the end.
     */
    public static long calculateStreamContentLength(long originalLength, long chunkSize) {
        if (originalLength < 0 || chunkSize == 0) {
            throw new IllegalArgumentException(originalLength + ", " + chunkSize + "Args <= 0 not expected");
        }

        long maxSizeChunks = originalLength / chunkSize;
        long remainingBytes = originalLength % chunkSize;

        long allChunks = maxSizeChunks * calculateChunkLength(chunkSize);
        long remainingInChunk = remainingBytes > 0 ? calculateChunkLength(remainingBytes) : 0;
        // last byte is composed of a "0" and "\r\n"
        long lastByteSize = 1 + (long) CRLF.length();

        return allChunks +  remainingInChunk + lastByteSize;
    }

    /**
     * Calculates the content length for a given algorithm and header name.
     *
     * @param algorithm  Algorithm used.
     * @param headerName Header name.
     * @return Content length of the trailer that will be appended at the end.
     */
    public static long calculateChecksumTrailerLength(Algorithm algorithm, String headerName) {
        return headerName.length()
               + HEADER_COLON_SEPARATOR.length()
               + algorithm.base64EncodedLength().longValue()
               + CRLF.length()
               + CRLF.length();
    }

    /**
     * Creates Chunk encoded checksum trailer for a computedChecksum which is in Base64 encoded.
     * @param computedChecksum Base64 encoded computed checksum.
     * @param trailerHeader Header for the checksum data in the trailer.
     * @return Chunk encoded checksum trailer with given header.
     */
    public static ByteBuffer createChecksumTrailer(String computedChecksum, String trailerHeader) {
        StringBuilder headerBuilder = new StringBuilder(trailerHeader)
                .append(HEADER_COLON_SEPARATOR)
                .append(computedChecksum)
                .append(CRLF)
                .append(CRLF);
        return ByteBuffer.wrap(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates ChunkEncoded data for an given chunk data.
     * @param chunkData chunk data that needs to be converted to chunk encoded format.
     * @param isLastByte if true then additional CRLF will not be appended.
     * @return Chunk encoded format of a given data.
     */
    public static ByteBuffer createChunk(ByteBuffer chunkData, boolean isLastByte) {
        int chunkLength = chunkData.remaining();
        StringBuilder chunkHeader = new StringBuilder(Integer.toHexString(chunkLength));
        chunkHeader.append(CRLF);
        try {
            byte[] header = chunkHeader.toString().getBytes(StandardCharsets.UTF_8);
            byte[] trailer = !isLastByte ? CRLF.getBytes(StandardCharsets.UTF_8)
                    : "".getBytes(StandardCharsets.UTF_8);
            ByteBuffer chunkFormattedBuffer = ByteBuffer.allocate(header.length + chunkLength + trailer.length);
            chunkFormattedBuffer.put(header).put(chunkData).put(trailer);
            chunkFormattedBuffer.flip();
            return chunkFormattedBuffer;
        } catch (Exception e) {
            throw SdkClientException.builder()
                    .message("Unable to create chunked data. " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }
}