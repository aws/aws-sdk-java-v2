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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import org.junit.Test;

public class GzipCompressorTest {
    private static final Compressor gzipCompressor = new GzipCompressor();
    private static final String COMPRESSABLE_STRING =
        "RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest";

    @Test
    public void compressedData_decompressesCorrectly() throws IOException {
        byte[] originalData = COMPRESSABLE_STRING.getBytes(StandardCharsets.UTF_8);
        byte[] compressedData = gzipCompressor.compress(originalData);

        int uncompressedSize = originalData.length;
        int compressedSize = compressedData.length;
        assertThat(compressedSize, lessThan(uncompressedSize));

        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        GZIPInputStream gzipInputStream = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        gzipInputStream.close();
        byte[] decompressedData = baos.toByteArray();

        assertThat(decompressedData, is(originalData));
    }
}
