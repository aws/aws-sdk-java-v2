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

package software.amazon.awssdk.core.compression;

import java.io.InputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.core.internal.interceptor.RequestCompressionInterceptor;

/**
 * Interface for compressors to be used by {@link RequestCompressionInterceptor} to compress requests.
 */
@SdkInternalApi
public interface Compressor {

    /**
     * The compression algorithm type.
     *
     * @return The {@link String} compression algorithm type.
     */
    String compressorType();

    /**
     * Compress a {@link SdkBytes} payload.
     *
     * @param content
     * @return The compressed {@link SdkBytes}.
     */
    SdkBytes compress(SdkBytes content);

    /**
     * Compress a byte[] payload.
     *
     * @param content
     * @return The compressed byte array.
     */
    default byte[] compress(byte[] content) {
        return compress(SdkBytes.fromByteArray(content)).asByteArray();
    }

    /**
     * Compress an {@link InputStream} payload.
     *
     * @param content
     * @return The compressed {@link InputStream}.
     */
    default InputStream compress(InputStream content) {
        return compress(SdkBytes.fromInputStream(content)).asInputStream();
    }

    /**
     * Compress an {@link ByteBuffer} payload.
     *
     * @param content
     * @return The compressed {@link ByteBuffer}.
     */
    default ByteBuffer compress(ByteBuffer content) {
        return compress(SdkBytes.fromByteBuffer(content)).asByteBuffer();
    }

    /**
     * Maps the {@link CompressionType} to its corresponding {@link Compressor}.
     * TODO: Update mappings here when additional compressors are supported in the future
     */
    static Compressor forCompressorType(CompressionType compressionType) {
        switch (compressionType) {
            case GZIP:
                return new GzipCompressor();
            default:
                throw new IllegalArgumentException("The compresssion type " + compressionType + "does not have an implemenation"
                                                   + " of Compressor.");
        }
    }
}
