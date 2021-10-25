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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.internal.chunked.AwsChunkedEncodingConfig;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsSignedChunkedEncodingInputStream;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsS3V4ChunkSigner;

/**
 * Runs unit tests that check that the class AwsChunkedEncodingInputStream supports params required for Sigv4 chunk
 * signing.
 */
@RunWith(MockitoJUnitRunner.class)
public class AwsChunkedEncodingInputStreamTest {

    private static final String REQUEST_SIGNATURE = "a0c7e7324d9c209c4a86a1c1452d60862591b7370a725364d52ba490210f2d9d";
    private static final String CHUNK_SIGNATURE_1 = "3d5a2da1f773f64fdde2c6ca7d18c4bfee2a5a76b8153854c2a722c3fe8549cc";
    private static final String CHUNK_SIGNATURE_2 = "0362eef10ceccf47fc1bf944a9df45a3e7dd5ea936e118b00e3d649b1d825f2f";
    private static final String CHUNK_SIGNATURE_3 = "4722d8dac986015bdabeb5fafcb45db7b5585329befa485849c4938fed2aaaa2";

    private static final String SIGNATURE_KEY = "chunk-signature=";
    private static final String EMPTY_STRING = "";
    private static final String CRLF = "\r\n";
    private static final int DEFAULT_CHUNK_SIZE = 128 * 1024;
    private static final int SIGV4_CHUNK_SIGNATURE_LENGTH = 64;

    @Mock
    AwsS3V4ChunkSigner chunkSigner;

    @Test
    public void streamContentLength_smallObject_calculatedCorrectly() {
        long streamContentLength =
            AwsSignedChunkedEncodingInputStream.calculateStreamContentLength(10,
                                                                             SIGV4_CHUNK_SIGNATURE_LENGTH,
                                                                             AwsChunkedEncodingConfig.create());
        assertThat(streamContentLength).isEqualTo(182);
    }

    @Test
    public void streamContentLength_largeObject_calculatedCorrectly() {

        long streamContentLength =
            AwsSignedChunkedEncodingInputStream.calculateStreamContentLength(DEFAULT_CHUNK_SIZE + 10,
                                                                             SIGV4_CHUNK_SIGNATURE_LENGTH,
                                                                             AwsChunkedEncodingConfig.create());
        assertThat(streamContentLength).isEqualTo(131344);
    }

    @Test
    public void streamContentLength_differentChunkSize_calculatedCorrectly() {
        int chunkSize = 64 * 1024;
        AwsChunkedEncodingConfig chunkConfig = AwsChunkedEncodingConfig.builder().chunkSize(chunkSize).build();

        long streamContentLength =
            AwsSignedChunkedEncodingInputStream.calculateStreamContentLength(chunkSize + 10,
                                                                             SIGV4_CHUNK_SIGNATURE_LENGTH,
                                                                             chunkConfig);
        assertThat(streamContentLength).isEqualTo(65808);
    }

    @Test(expected = IllegalArgumentException.class)
    public void streamContentLength_negative_throwsException() {
        AwsSignedChunkedEncodingInputStream.calculateStreamContentLength(-1,
                                                                         SIGV4_CHUNK_SIGNATURE_LENGTH,
                                                                         AwsChunkedEncodingConfig.create());
    }

    @Test
    public void chunkedEncodingStream_smallObject_createsCorrectChunks() throws IOException {
        when(chunkSigner.signChunk(any(), any())).thenReturn(CHUNK_SIGNATURE_1)
                                                 .thenReturn(CHUNK_SIGNATURE_2);

        String chunkData = "helloworld";

        ByteArrayInputStream input = new ByteArrayInputStream(chunkData.getBytes());


        AwsSignedChunkedEncodingInputStream stream = AwsSignedChunkedEncodingInputStream.builder()
                                                                                        .inputStream(input)
                                                                                        .awsChunkSigner(chunkSigner)
                                                                                        .headerSignature(REQUEST_SIGNATURE)
                                                                                        .awsChunkedEncodingConfig(AwsChunkedEncodingConfig.create())
                                                                                        .build();
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

        String chunk1Data = StringUtils.repeat('a', DEFAULT_CHUNK_SIZE);
        String chunk2Data = "a";

        ByteArrayInputStream input = new ByteArrayInputStream(chunk1Data.concat(chunk2Data).getBytes());

        AwsSignedChunkedEncodingInputStream stream = AwsSignedChunkedEncodingInputStream.builder()
                                                                                        .inputStream(input)
                                                                                        .headerSignature(REQUEST_SIGNATURE)
                                                                                        .awsChunkSigner(chunkSigner)
                                                                                        .awsChunkedEncodingConfig(AwsChunkedEncodingConfig.create())
                                                                                        .build();
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


        AwsSignedChunkedEncodingInputStream stream = AwsSignedChunkedEncodingInputStream.builder()
                                                                                        .inputStream(input)
                                                                                        .awsChunkSigner(chunkSigner)
                                                                                        .headerSignature(REQUEST_SIGNATURE)
                                                                                        .awsChunkedEncodingConfig(AwsChunkedEncodingConfig.create()).build();


        int expectedChunks = 1;
        consumeAndVerify(stream, expectedChunks);
        Mockito.verify(chunkSigner, times(1)).signChunk(chunkData.getBytes(StandardCharsets.UTF_8), REQUEST_SIGNATURE);
    }

    private void consumeAndVerify(AwsSignedChunkedEncodingInputStream stream, int numChunks) throws IOException {
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
            assertThat(signatureValue.length()).isEqualTo(SIGV4_CHUNK_SIGNATURE_LENGTH);
        }
    }

}
