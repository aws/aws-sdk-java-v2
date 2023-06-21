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

package software.amazon.awssdk.core.internal.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.compression.Compressor;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkInternalApi
public class GzipCompressor implements Compressor {

    private static final String CONTENT_TYPE = "gzip";

    @Override
    public String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public byte[] compress(byte[] content) {
        try {
            ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(compressedOutputStream);
            gzipOutputStream.write(content);
            gzipOutputStream.close();

            return compressedOutputStream.toByteArray();
        } catch (IOException e) {
            throw SdkClientException.create(e.getMessage(), e);
        }
    }

    @Override
    public InputStream compressSyncStream(InputStream inputStream) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public Publisher<ByteBuffer> compressAsyncStream(InputStream inputStream) {
        //TODO
        throw new UnsupportedOperationException();
    }
}
