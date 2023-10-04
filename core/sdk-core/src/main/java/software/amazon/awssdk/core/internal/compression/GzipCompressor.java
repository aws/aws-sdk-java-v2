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

import static software.amazon.awssdk.utils.IoUtils.closeQuietly;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;

@SdkInternalApi
public final class GzipCompressor implements Compressor {

    private static final String COMPRESSOR_TYPE = "gzip";
    private static final Logger log = LoggerFactory.getLogger(GzipCompressor.class);

    @Override
    public String compressorType() {
        return COMPRESSOR_TYPE;
    }

    @Override
    public SdkBytes compress(SdkBytes content) {
        GZIPOutputStream gzipOutputStream = null;
        try {
            ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();
            gzipOutputStream = new GZIPOutputStream(compressedOutputStream);
            gzipOutputStream.write(content.asByteArray());
            gzipOutputStream.close();
            return SdkBytes.fromByteArray(compressedOutputStream.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            closeQuietly(gzipOutputStream, log);
        }
    }
}