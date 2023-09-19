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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.core.util.FileUtils.generateRandomAsciiFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;

public class AwsCompressionInputStreamTest {
    private static Compressor compressor;

    @BeforeClass
    public static void setup() throws IOException {
        compressor = new GzipCompressor();
    }

    @Test
    public void nonMarkSupportedInputStream_marksAndResetsCorrectly() throws IOException {
        File file = generateRandomAsciiFile(100);
        InputStream is = new FileInputStream(file);
        assertFalse(is.markSupported());

        AwsCompressionInputStream compressionInputStream = AwsCompressionInputStream.builder()
                                                                                    .inputStream(is)
                                                                                    .compressor(compressor)
                                                                                    .build();

        compressionInputStream.mark(100);
        compressionInputStream.reset();
        String read1 = readInputStream(compressionInputStream);
        compressionInputStream.reset();
        String read2 = readInputStream(compressionInputStream);
        assertThat(read1).isEqualTo(read2);
    }

    @Test
    public void markSupportedInputStream_marksAndResetsCorrectly() throws IOException {
        InputStream is = new ByteArrayInputStream(generateRandomBody(100));
        assertTrue(is.markSupported());
        AwsCompressionInputStream compressionInputStream = AwsCompressionInputStream.builder()
                                                                                    .inputStream(is)
                                                                                    .compressor(compressor)
                                                                                    .build();
        compressionInputStream.mark(100);
        compressionInputStream.reset();
        String read1 = readInputStream(compressionInputStream);
        compressionInputStream.reset();
        String read2 = readInputStream(compressionInputStream);
        assertThat(read1).isEqualTo(read2);
    }

    private byte[] generateRandomBody(int size) {
        byte[] randomData = new byte[size];
        new Random().nextBytes(randomData);
        return randomData;
    }

    private String readInputStream(InputStream is) throws IOException {
        byte[] buffer = new byte[512];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream.toString();
    }
}
