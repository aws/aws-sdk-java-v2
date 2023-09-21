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

package software.amazon.awssdk.core.checksum;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.internal.util.ChunkContentUtils.calculateChecksumTrailerLength;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.internal.io.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.core.internal.io.AwsUnsignedChunkedEncodingInputStream;
import software.amazon.awssdk.core.internal.util.ChunkContentUtils;

public class AwsChunkedEncodingInputStreamTest {

    private static final String CRLF = "\r\n";
    final private static Algorithm SHA256_ALGORITHM = Algorithm.SHA256;
    final private String SHA256_HEADER_NAME = "x-amz-checksum-sha-256";

    @Test
    public void readAwsUnsignedChunkedEncodingInputStream() throws IOException {
        String initialString = "Hello world";
        InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());

        final AwsChunkedEncodingInputStream checksumCalculatingInputStream =
                AwsUnsignedChunkedEncodingInputStream.builder()
                        .inputStream(targetStream)
                        .sdkChecksum(SdkChecksum.forAlgorithm(SHA256_ALGORITHM))
                        .checksumHeaderForTrailer(SHA256_HEADER_NAME)
                        .build();
        StringBuilder sb = new StringBuilder();
        for (int ch; (ch = checksumCalculatingInputStream.read()) != -1; ) {
            sb.append((char) ch);
        }
        assertThat(sb).hasToString("b" + CRLF + initialString +CRLF + "0" + CRLF
                + "x-amz-checksum-sha-256:ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=" + CRLF+CRLF);
    }

    @Test
    public void lengthsOfCalculateByChecksumCalculatingInputStream(){

        String initialString = "Hello world";
        long calculateChunkLength = ChunkContentUtils.calculateStreamContentLength(initialString.length(),
                                                                                   AwsChunkedEncodingInputStream.DEFAULT_CHUNK_SIZE);
        long checksumContentLength = calculateChecksumTrailerLength(SHA256_ALGORITHM, SHA256_HEADER_NAME);
        assertThat(calculateChunkLength).isEqualTo(19);
        assertThat(checksumContentLength).isEqualTo(71);
    }

    @Test
    public void markAndResetAwsChunkedEncodingInputStream() throws IOException {
        String initialString = "Hello world";
        InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
        final AwsChunkedEncodingInputStream checksumCalculatingInputStream =
                AwsUnsignedChunkedEncodingInputStream.builder()
                        .inputStream(targetStream)
                        .sdkChecksum(SdkChecksum.forAlgorithm(SHA256_ALGORITHM))
                        .checksumHeaderForTrailer(SHA256_HEADER_NAME)
                        .build();
         StringBuilder sb = new StringBuilder();
        checksumCalculatingInputStream.mark(3);
        boolean marked = true;
        int count = 0;
        for (int ch; (ch = checksumCalculatingInputStream.read()) != -1; ) {
            sb.append((char) ch);
            if(marked && count++ == 5){
                checksumCalculatingInputStream.reset();
                sb = new StringBuilder();
                marked = false;
            }
        }
        assertThat(sb).hasToString("b" + CRLF + initialString +CRLF + "0" + CRLF
                + "x-amz-checksum-sha-256:ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=" + CRLF+CRLF);    }
}
