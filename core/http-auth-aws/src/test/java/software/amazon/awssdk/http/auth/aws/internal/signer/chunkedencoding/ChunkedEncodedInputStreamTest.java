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

package software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding;

import static java.util.Arrays.copyOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.http.auth.aws.internal.signer.V4CanonicalRequest.getCanonicalHeadersString;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.deriveSigningKey;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.hash;
import static software.amazon.awssdk.utils.BinaryUtils.toHex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.RollingSigner;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.Pair;

public class ChunkedEncodedInputStreamTest {

    @Test
    public void ChunkEncodedInputStream_withBasicParams_returnsEncodedChunks() throws IOException {
        byte[] data = "abcdefghij".getBytes();
        InputStream payload = new ByteArrayInputStream(data);
        int chunkSize = 3;

        ChunkedEncodedInputStream inputStream = ChunkedEncodedInputStream
            .builder()
            .inputStream(payload)
            .chunkSize(chunkSize)
            .header(chunk -> Integer.toHexString(chunk.length).getBytes())
            .build();

        byte[] tmp = new byte[64];
        int bytesRead = readAll(inputStream, tmp);

        int expectedBytesRead = 35;
        byte[] expected = new byte[expectedBytesRead];
        System.arraycopy(
            "3\r\nabc\r\n3\r\ndef\r\n3\r\nghi\r\n1\r\nj\r\n0\r\n\r\n".getBytes(),
            0,
            expected,
            0,
            expectedBytesRead
        );
        byte[] actual = copyOf(tmp, bytesRead);

        assertEquals(expectedBytesRead, bytesRead);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void ChunkEncodedInputStream_withExtensions_returnsEncodedExtendedChunks() throws IOException {
        byte[] data = "abcdefghij".getBytes();
        InputStream payload = new ByteArrayInputStream(data);
        int chunkSize = 3;

        ChunkExtensionProvider helloWorldExt = chunk -> Pair.of(
            "hello".getBytes(StandardCharsets.UTF_8),
            "world!".getBytes(StandardCharsets.UTF_8)
        );

        ChunkedEncodedInputStream inputStream = ChunkedEncodedInputStream
            .builder()
            .inputStream(payload)
            .chunkSize(chunkSize)
            .header(chunk -> Integer.toHexString(chunk.length).getBytes())
            .extensions(Collections.singletonList(helloWorldExt))
            .build();

        byte[] tmp = new byte[128];
        int bytesRead = readAll(inputStream, tmp);

        int expectedBytesRead = 100;
        byte[] expected = new byte[expectedBytesRead];
        System.arraycopy(
            ("3;hello=world!\r\nabc\r\n3;hello=world!\r\ndef\r\n3;hello=world!\r\nghi\r\n"
             + "1;hello=world!\r\nj\r\n0;hello=world!\r\n\r\n").getBytes(),
            0,
            expected,
            0,
            expectedBytesRead
        );
        byte[] actual = copyOf(tmp, expected.length);

        assertEquals(expectedBytesRead, bytesRead);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void ChunkEncodedInputStream_withTrailers_returnsEncodedChunksAndTrailerChunk() throws IOException {
        byte[] data = "abcdefghij".getBytes();
        InputStream payload = new ByteArrayInputStream(data);
        int chunkSize = 3;

        TrailerProvider helloWorldTrailer = () -> Pair.of(
            "hello",
            Collections.singletonList("world!")
        );

        ChunkedEncodedInputStream inputStream = ChunkedEncodedInputStream
            .builder()
            .inputStream(payload)
            .chunkSize(chunkSize)
            .header(chunk -> Integer.toHexString(chunk.length).getBytes())
            .trailers(Collections.singletonList(helloWorldTrailer))
            .build();

        byte[] tmp = new byte[64];
        int bytesRead = readAll(inputStream, tmp);

        int expectedBytesRead = 49;
        byte[] expected = new byte[expectedBytesRead];
        System.arraycopy(
            "3\r\nabc\r\n3\r\ndef\r\n3\r\nghi\r\n1\r\nj\r\n0\r\nhello:world!\r\n\r\n".getBytes(),
            0,
            expected,
            0,
            expectedBytesRead
        );
        byte[] actual = copyOf(tmp, expected.length);

        assertEquals(expectedBytesRead, bytesRead);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void ChunkEncodedInputStream_withExtensionsAndTrailers_EncodedExtendedChunksAndTrailerChunk() throws IOException {
        byte[] data = "abcdefghij".getBytes();
        InputStream payload = new ByteArrayInputStream(data);
        int chunkSize = 3;

        ChunkExtensionProvider aExt = chunk -> Pair.of("a".getBytes(StandardCharsets.UTF_8),
                                                       "1".getBytes(StandardCharsets.UTF_8));
        ChunkExtensionProvider bExt = chunk -> Pair.of("b".getBytes(StandardCharsets.UTF_8),
                                                       "2".getBytes(StandardCharsets.UTF_8));

        TrailerProvider aTrailer = () -> Pair.of("a", Collections.singletonList("1"));
        TrailerProvider bTrailer = () -> Pair.of("b", Collections.singletonList("2"));

        ChunkedEncodedInputStream inputStream = ChunkedEncodedInputStream
            .builder()
            .inputStream(payload)
            .chunkSize(chunkSize)
            .header(chunk -> Integer.toHexString(chunk.length).getBytes())
            .addExtension(aExt)
            .addExtension(bExt)
            .addTrailer(aTrailer)
            .addTrailer(bTrailer)
            .build();

        byte[] tmp = new byte[128];
        int bytesRead = readAll(inputStream, tmp);

        int expectedBytesRead = 85;
        byte[] expected = new byte[expectedBytesRead];
        System.arraycopy(
            "3;a=1;b=2\r\nabc\r\n3;a=1;b=2\r\ndef\r\n3;a=1;b=2\r\nghi\r\n1;a=1;b=2\r\nj\r\n0;a=1;b=2\r\na:1\r\nb:2\r\n\r\n".getBytes(),
            0,
            expected,
            0,
            expectedBytesRead
        );
        byte[] actual = copyOf(tmp, expected.length);

        assertEquals(expectedBytesRead, bytesRead);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void ChunkEncodedInputStream_withAwsParams_returnsAwsSignedAndEncodedChunks() throws IOException {
        byte[] data = new byte[65 * 1024];
        Arrays.fill(data, (byte) 'a');
        String seedSignature = "106e2a8a18243abcf37539882f36619c00e2dfc72633413f02d3b74544bfeb8e";
        CredentialScope credentialScope =
            new CredentialScope("us-east-1", "s3", Instant.parse("2013-05-24T00:00:00Z"));
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        byte[] signingKey = deriveSigningKey(credentials, credentialScope);
        InputStream payload = new ByteArrayInputStream(data);
        int chunkSize = 64 * 1024;

        RollingSigner signer = new RollingSigner(signingKey, seedSignature);

        ChunkExtensionProvider ext = chunk -> Pair.of(
            "chunk-signature".getBytes(StandardCharsets.UTF_8),
            signer.sign(previousSignature ->
                            "AWS4-HMAC-SHA256-PAYLOAD" + SignerConstant.LINE_SEPARATOR +
                            credentialScope.getDatetime() + SignerConstant.LINE_SEPARATOR +
                            credentialScope.scope() + SignerConstant.LINE_SEPARATOR +
                            previousSignature + SignerConstant.LINE_SEPARATOR +
                            toHex(hash("")) + SignerConstant.LINE_SEPARATOR +
                            toHex(hash(chunk)))
                  .getBytes(StandardCharsets.UTF_8)
        );

        TrailerProvider checksumTrailer = () -> Pair.of(
            "x-amz-checksum-crc32c",
            Collections.singletonList("wdBDMA==")
        );

        List<Pair<String, List<String>>> trailers = Collections.singletonList(checksumTrailer.get());
        Function<String, String> template =
            previousSignature ->
                "AWS4-HMAC-SHA256-TRAILER" + SignerConstant.LINE_SEPARATOR +
                credentialScope.getDatetime() + SignerConstant.LINE_SEPARATOR +
                credentialScope.scope() + SignerConstant.LINE_SEPARATOR +
                previousSignature + SignerConstant.LINE_SEPARATOR +
                toHex(hash(getCanonicalHeadersString(trailers)));

        TrailerProvider signatureTrailer = () -> Pair.of(
            "x-amz-trailer-signature",
            Collections.singletonList(signer.sign(template))
        );

        ChunkedEncodedInputStream inputStream = ChunkedEncodedInputStream
            .builder()
            .inputStream(payload)
            .chunkSize(chunkSize)
            .header(chunk -> Integer.toHexString(chunk.length).getBytes())
            .extensions(Collections.singletonList(ext))
            .trailers(Arrays.asList(checksumTrailer, signatureTrailer))
            .build();

        byte[] tmp = new byte[chunkSize * 4];
        int bytesRead = readAll(inputStream, tmp);

        int expectedBytesRead = 66946;
        byte[] actualBytes = copyOf(tmp, expectedBytesRead);
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write(
            "10000;chunk-signature=b474d8862b1487a5145d686f57f013e54db672cee1c953b3010fb58501ef5aa2\r\n".getBytes(
                StandardCharsets.UTF_8)
        );
        expected.write(data, 0, chunkSize);
        expected.write(
            "\r\n400;chunk-signature=1c1344b170168f8e65b41376b44b20fe354e373826ccbbe2c1d40a8cae51e5c7\r\n".getBytes(
                StandardCharsets.UTF_8)
        );
        expected.write(data, chunkSize, 1024);
        expected.write(
            "\r\n0;chunk-signature=2ca2aba2005185cf7159c6277faf83795951dd77a3a99e6e65d5c9f85863f992\r\n".getBytes(
                StandardCharsets.UTF_8)
        );
        expected.write((
                           "x-amz-checksum-crc32c:wdBDMA==\r\n" +
                           "x-amz-trailer-signature:ce306fa4cdf73aa89071b78358f0d22ea79c43117314c8ed68017f7d6f91048e\r\n" +
                           "\r\n").getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(expectedBytesRead, bytesRead);
        assertArrayEquals(expected.toByteArray(), actualBytes);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 8, 13, 21, 24, 45, 69, 104})
    void ChunkEncodedInputStream_withVariableChunkSize_shouldCorrectlyChunkData(int chunkSize) throws IOException {
        int size = 100;
        byte[] data = new byte[size];
        Arrays.fill(data, (byte) 'a');

        ChunkedEncodedInputStream inputStream = ChunkedEncodedInputStream
            .builder()
            .inputStream(new ByteArrayInputStream(data))
            .header(chunk -> new byte[] {'0'})
            .chunkSize(chunkSize)
            .build();

        int expectedBytesRead = 0;
        int numChunks = size / chunkSize;

        // 0\r\n<data>\r\n
        expectedBytesRead += numChunks * (5 + chunkSize);

        if (size % chunkSize != 0) {
            // 0\r\n\<left-over>\r\n
            expectedBytesRead += 5 + (size % chunkSize);
        }

        // 0\r\n\r\n
        expectedBytesRead += 5;

        byte[] tmp = new byte[expectedBytesRead];
        int bytesRead = readAll(inputStream, tmp);

        assertEquals(expectedBytesRead, bytesRead);
    }

    private int readAll(InputStream src, byte[] dst) throws IOException {
        int read = 0;
        int offset = 0;
        while (read >= 0) {
            read = src.read();
            if (read >= 0) {
                dst[offset] = (byte) read;
                offset += 1;
            }
        }
        return offset;
    }
}
