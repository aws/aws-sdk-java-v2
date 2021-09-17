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

package software.amazon.awssdk.authcrt.signer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingConfig;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.authcrt.signer.internal.chunkedencoding.AwsS3V4aChunkSigner;

/**
 * Runs unit tests that check that the class AwsChunkedEncodingInputStream supports params required for Sigv4a chunk
 * signing.
 */
@RunWith(MockitoJUnitRunner.class)
public class AwsChunkedEncodingInputStreamTest {

    private static final String REQUEST_SIGNATURE;
    private static final String CHUNK_SIGNATURE_1;
    private static final String CHUNK_SIGNATURE_2;
    private static final String CHUNK_SIGNATURE_3;

    private static final String SIGNATURE_KEY = "chunk-signature=";
    private static final String EMPTY_STRING = "";
    private static final String CRLF = "\r\n";
    private static final int DEFAULT_CHUNK_SIZE = 128 * 1024;
    private static final int SIGV4A_SIGNATURE_LENGTH = 144;

    static {
        byte[] tmp = new byte[140];
        Arrays.fill(tmp, (byte) 0x2A);
        REQUEST_SIGNATURE = new String(tmp);

        tmp = new byte[144];
        Arrays.fill(tmp, (byte) 0x2B);
        CHUNK_SIGNATURE_1 = new String(tmp);

        tmp = new byte[144];
        Arrays.fill(tmp, (byte) 0x2C);
        CHUNK_SIGNATURE_2 = new String(tmp);

        tmp = new byte[144];
        Arrays.fill(tmp, (byte) 0x2E);
        CHUNK_SIGNATURE_3 = new String(tmp);
    }

    @Mock
    AwsS3V4aChunkSigner chunkSigner;

    /**
     * maxSizeChunks = 0, remainingBytes = 10;
     * chunklen(10) = 1 + 17 + 144 + 2 + 10 + 2 = 176
     * chunklen(0) = 1 + 17 + 144 + 2 + 0 + 2 = 166
     * total = 0 * chunklen(default_chunk_size) + chunklen(10) + chunklen(0) = 342
     */
    @Test
    public void streamContentLength_smallObject_calculatedCorrectly() {
        long streamContentLength =
            AwsChunkedEncodingInputStream.calculateStreamContentLength(10,
                                                                       AwsS3V4aChunkSigner.getSignatureLength(),
                                                                       AwsChunkedEncodingConfig.create());
        assertThat(streamContentLength).isEqualTo(342);
    }

    /**
     * maxSizeChunks = 1, remainingBytes = 1;
     * chunklen(131072) = 5 + 17 + 144 + 2 + 131072 + 2 = 131242
     * chunklen(1) = 1 + 17 + 144 + 2 + 10 + 2 = 176
     * chunklen(0) = 1 + 17 + 144 + 2 + 0 + 2 = 166
     * total = 1 * chunklen(default_chunk_size) + chunklen(10) + chunklen(0) = 131584
     */
    @Test
    public void streamContentLength_largeObject_calculatedCorrectly() {
        long streamContentLength =
            AwsChunkedEncodingInputStream.calculateStreamContentLength(DEFAULT_CHUNK_SIZE + 10,
                                                                       AwsS3V4aChunkSigner.getSignatureLength(),
                                                                       AwsChunkedEncodingConfig.create());
        assertThat(streamContentLength).isEqualTo(131584);
    }

    @Test
    public void streamContentLength_differentChunkSize_calculatedCorrectly() {
        int chunkSize = 64 * 1024;

        AwsChunkedEncodingConfig chunkConfig = AwsChunkedEncodingConfig.builder().chunkSize(chunkSize).build();
        long streamContentLength =
            AwsChunkedEncodingInputStream.calculateStreamContentLength(chunkSize + 10,
                                                                       AwsS3V4aChunkSigner.getSignatureLength(),
                                                                       chunkConfig);
        assertThat(streamContentLength).isEqualTo(66048);
    }

    @Test(expected = IllegalArgumentException.class)
    public void streamContentLength_negative_throwsException() {
        AwsChunkedEncodingInputStream.calculateStreamContentLength(-1,
                                                                   AwsS3V4aChunkSigner.getSignatureLength(),
                                                                   AwsChunkedEncodingConfig.create());
    }

    @Test
    public void chunkedEncodingStream_smallObject_createsCorrectChunks() throws IOException {
        when(chunkSigner.signChunk(any(), any())).thenReturn(CHUNK_SIGNATURE_1)
                                                 .thenReturn(CHUNK_SIGNATURE_2);

        String chunkData = "helloworld";

        ByteArrayInputStream input = new ByteArrayInputStream(chunkData.getBytes());
        AwsChunkedEncodingInputStream stream = new AwsChunkedEncodingInputStream(input,
                                                                                 REQUEST_SIGNATURE,
                                                                                 chunkSigner,
                                                                                 AwsChunkedEncodingConfig.create());
        int expectedChunks = 2;
        consumeAndVerify(stream, expectedChunks);
        Mockito.verify(chunkSigner, times(1)).signChunk(chunkData.getBytes(StandardCharsets.UTF_8), REQUEST_SIGNATURE);
        Mockito.verify(chunkSigner, times(1)).signChunk(EMPTY_STRING.getBytes(StandardCharsets.UTF_8), CHUNK_SIGNATURE_1);
    }

    @Test
    public void chunkedEncodingStream_largeObject_createsCorrectChunks() throws IOException {
        when(chunkSigner.signChunk(any(), any())).thenReturn(CHUNK_SIGNATURE_1)
                                                 .thenReturn(CHUNK_SIGNATURE_2)
                                                 .thenReturn(CHUNK_SIGNATURE_3);

        String chunk1Data = StringUtils.repeat("a", DEFAULT_CHUNK_SIZE);
        String chunk2Data = "a";
        ByteArrayInputStream input = new ByteArrayInputStream(chunk1Data.concat(chunk2Data).getBytes());
        AwsChunkedEncodingInputStream stream = new AwsChunkedEncodingInputStream(input,
                                                                                 REQUEST_SIGNATURE,
                                                                                 chunkSigner,
                                                                                 AwsChunkedEncodingConfig.create());
        int expectedChunks = 3;
        consumeAndVerify(stream, expectedChunks);
        Mockito.verify(chunkSigner, times(1)).signChunk(chunk1Data.getBytes(StandardCharsets.UTF_8), REQUEST_SIGNATURE);
        Mockito.verify(chunkSigner, times(1)).signChunk(chunk2Data.getBytes(StandardCharsets.UTF_8), CHUNK_SIGNATURE_1);
        Mockito.verify(chunkSigner, times(1)).signChunk(EMPTY_STRING.getBytes(StandardCharsets.UTF_8), CHUNK_SIGNATURE_2);
    }

    @Test
    public void chunkedEncodingStream_differentChunkSize_createsCorrectChunks() throws IOException {
        when(chunkSigner.signChunk(any(), any())).thenReturn(CHUNK_SIGNATURE_1)
                                                 .thenReturn(CHUNK_SIGNATURE_2)
                                                 .thenReturn(CHUNK_SIGNATURE_3);
        int chunkSize = 64 * 1024;
        AwsChunkedEncodingConfig chunkConfig = AwsChunkedEncodingConfig.builder().chunkSize(chunkSize).build();

        String chunk1Data = StringUtils.repeat("a", chunkSize);
        String chunk2Data = "a";
        ByteArrayInputStream input = new ByteArrayInputStream(chunk1Data.concat(chunk2Data).getBytes());
        AwsChunkedEncodingInputStream stream = new AwsChunkedEncodingInputStream(input,
                                                                                 REQUEST_SIGNATURE,
                                                                                 chunkSigner,
                                                                                 chunkConfig);
        int expectedChunks = 3;
        consumeAndVerify(stream, expectedChunks);
        Mockito.verify(chunkSigner, times(1)).signChunk(chunk1Data.getBytes(StandardCharsets.UTF_8), REQUEST_SIGNATURE);
        Mockito.verify(chunkSigner, times(1)).signChunk(chunk2Data.getBytes(StandardCharsets.UTF_8), CHUNK_SIGNATURE_1);
        Mockito.verify(chunkSigner, times(1)).signChunk(EMPTY_STRING.getBytes(StandardCharsets.UTF_8), CHUNK_SIGNATURE_2);
    }

    @Test
    public void chunkedEncodingStream_emptyString_createsCorrectChunks() throws IOException {
        when(chunkSigner.signChunk(any(), any())).thenReturn(CHUNK_SIGNATURE_1);

        String chunkData = EMPTY_STRING;

        ByteArrayInputStream input = new ByteArrayInputStream(chunkData.getBytes());
        AwsChunkedEncodingInputStream stream = new AwsChunkedEncodingInputStream(input,
                                                                                 REQUEST_SIGNATURE,
                                                                                 chunkSigner,
                                                                                 AwsChunkedEncodingConfig.create());
        int expectedChunks = 1;
        consumeAndVerify(stream, expectedChunks);
        Mockito.verify(chunkSigner, times(1)).signChunk(chunkData.getBytes(StandardCharsets.UTF_8), REQUEST_SIGNATURE);
    }

    private void consumeAndVerify(AwsChunkedEncodingInputStream stream, int numChunks) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
        assertChunks(result, numChunks);
    }

    private void assertChunks(String result, int numExpectedChunks) {
        List<String> lines = Stream.of(result.split(CRLF)).collect(Collectors.toList());
        assertThat(lines.size()).isEqualTo(numExpectedChunks * 2 - 1);
        for (int i = 0; i < lines.size(); i = i + 2) {
            String chunkMetadata = lines.get(i);
            String signatureValue = chunkMetadata.substring(chunkMetadata.indexOf(SIGNATURE_KEY) + SIGNATURE_KEY.length());
            assertThat(signatureValue.length()).isEqualTo(SIGV4A_SIGNATURE_LENGTH);
        }
    }
}
