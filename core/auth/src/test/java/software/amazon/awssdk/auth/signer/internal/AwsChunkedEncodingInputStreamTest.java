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

package software.amazon.awssdk.auth.signer.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.utils.BinaryUtils;

public class AwsChunkedEncodingInputStreamTest {

    private static final byte[] SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SIGNING_KEY = "signingkey".getBytes(StandardCharsets.UTF_8);
    private static final String DATE_TIME = "20201216T011309Z";
    private static final String SCOPE = "20201216/us-west-2/s3/aws4_request";

    private static final String CRLF = "\r\n";
    private static final int DEFAULT_CHUNK_SIZE = 128 * 1024;

    @Test
    public void streamContentLength_smallObject_calculatedCorrectly() {
        long streamContentLength = AwsChunkedEncodingInputStream.calculateStreamContentLength(10);
        assertThat(streamContentLength).isEqualTo(182);
    }

    @Test
    public void streamContentLength_largeObject_calculatedCorrectly() {
        long defaultChunkSize = 128 * 1024;
        long streamContentLength = AwsChunkedEncodingInputStream.calculateStreamContentLength(defaultChunkSize + 10);
        assertThat(streamContentLength).isEqualTo(131344);
    }

    @Test(expected = IllegalArgumentException.class)
    public void streamContentLength_negative_throwsException() {
        AwsChunkedEncodingInputStream.calculateStreamContentLength(-1);
    }

    @Test
    public void chunkedEncodingStream_smallObject_createsCorrectChunks() throws IOException {
        String chunkData = "helloworld";
        String expectedChunkOutput = "a;chunk-signature=a0c7e7324d9c209c4a86a1c1452d60862591b7370a725364d52ba490210f2d9d" + CRLF
                                     + "helloworld" + CRLF
                                     + "0;chunk-signature=4722d8dac986015bdabeb5fafcb45db7b5585329befa485849c4938fed2aaaa2" + CRLF
                                     + CRLF;

        ByteArrayInputStream input = new ByteArrayInputStream(chunkData.getBytes());
        AwsChunkedEncodingInputStream stream = createChunkedEncodingInputStream(input);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
        assertThat(result).isEqualTo(expectedChunkOutput);
    }

    @Test
    public void chunkedEncodingStream_largeObject_createsCorrectChunks() throws IOException {
        String chunk1Data = StringUtils.repeat('a', DEFAULT_CHUNK_SIZE);
        String chunk2Data = "a";
        String expectedChunkOutput = "20000;chunk-signature=be3155d539d4f5daf575096ab6d7090070b5ad00be6b802a8eb4c8bacd6e7dcb" + CRLF
                                     + chunk1Data + CRLF
                                     + "1;chunk-signature=0362eef10ceccf47fc1bf944a9df45a3e7dd5ea936e118b00e3d649b1d825f2f" + CRLF
                                     + chunk2Data + CRLF
                                     + "0;chunk-signature=3d5a2da1f773f64fdde2c6ca7d18c4bfee2a5a76b8153854c2a722c3fe8549cc" + CRLF
                                     + CRLF;

        ByteArrayInputStream input = new ByteArrayInputStream(chunk1Data.concat(chunk2Data).getBytes());
        AwsChunkedEncodingInputStream stream = createChunkedEncodingInputStream(input);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
        assertThat(result).isEqualTo(expectedChunkOutput);
    }

    @Test
    public void chunkedEncodingStream_emptyString_createsCorrectChunks() throws IOException {
        String chunkData = "";
        String expectedChunkOutput = "0;chunk-signature=67a2a80cee05190f0376fa861594fcfa351606cfdcf932b45781ddd6e4d78501" + CRLF
                                     + CRLF;

        ByteArrayInputStream input = new ByteArrayInputStream(chunkData.getBytes());
        AwsChunkedEncodingInputStream stream = createChunkedEncodingInputStream(input);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
        assertThat(result).isEqualTo(expectedChunkOutput);
    }

    private AwsChunkedEncodingInputStream createChunkedEncodingInputStream(ByteArrayInputStream input) {
        return new AwsChunkedEncodingInputStream(input,
                                                 SIGNING_KEY,
                                                 DATE_TIME, SCOPE,
                                                 BinaryUtils.toHex(SIGNATURE),
                                                 AwsS3V4Signer.create());
    }
}
