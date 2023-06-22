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
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.core.internal.interceptor.RequestCompressionInterceptor;

/**
 * Interface for compressors to be used by {@link RequestCompressionInterceptor} to compress requests.
 */
@SdkPublicApi
public interface Compressor {

    /**
     * The compression algorithm type.
     */
    String compressorType();

    /**
     * Compress an {@link InputStream}.
     */
    InputStream compress(InputStream inputStream);

    /**
     * Compress an async stream.
     */
    Publisher<ByteBuffer> compressAsyncStream(Publisher<ByteBuffer> publisher);

    /**
     * Maps the {@link CompressionType} to its corresponding {@link Compressor}.
     * TODO: Update mappings here when additional compressors are supported in the future
     */
    static Compressor forCompressorType(CompressionType compressionType) {
        switch (compressionType) {
            case GZIP:
                return new GzipCompressor();
            default:
                return null;
        }
    }
}
