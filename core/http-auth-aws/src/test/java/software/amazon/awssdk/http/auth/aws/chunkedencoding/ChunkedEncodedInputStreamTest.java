package software.amazon.awssdk.http.auth.aws.chunkedencoding;

import static java.util.Arrays.copyOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.deriveSigningKey;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.hash;
import static software.amazon.awssdk.utils.BinaryUtils.toHex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.auth.aws.internal.signer.RollingSigner;
import software.amazon.awssdk.http.auth.aws.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.util.SignerConstant;
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

        ChunkExtension helloWorldExt = chunk -> Pair.of(
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

        Trailer helloWorldTrailer = chunk -> Pair.of(
            "hello".getBytes(StandardCharsets.UTF_8),
            "world!".getBytes(StandardCharsets.UTF_8)
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

        ChunkExtension aExt = chunk -> Pair.of("a".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8));
        ChunkExtension bExt = chunk -> Pair.of("b".getBytes(StandardCharsets.UTF_8), "2".getBytes(StandardCharsets.UTF_8));

        Trailer aTrailer = chunk -> Pair.of("a".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8));
        Trailer bTrailer = chunk -> Pair.of("b".getBytes(StandardCharsets.UTF_8), "2".getBytes(StandardCharsets.UTF_8));

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

        ChunkExtension ext = chunk -> Pair.of(
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

        Trailer checksumTrailer = chunk -> Pair.of(
            "x-amz-checksum-crc32c".getBytes(StandardCharsets.UTF_8),
            "wdBDMA==".getBytes(StandardCharsets.UTF_8)
        );

        Trailer signatureTrailer = chunk -> Pair.of(
            "x-amz-trailer-signature".getBytes(StandardCharsets.UTF_8),
            signer.sign(previousSignature ->
                            "AWS4-HMAC-SHA256-TRAILER" + SignerConstant.LINE_SEPARATOR +
                            credentialScope.getDatetime() + SignerConstant.LINE_SEPARATOR +
                            credentialScope.scope() + SignerConstant.LINE_SEPARATOR +
                            previousSignature + SignerConstant.LINE_SEPARATOR +
                            toHex(hash(chunk)))
                  .getBytes(StandardCharsets.UTF_8)
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
                           "x-amz-trailer-signature:4473a2a8e96dc7a3dd547ee4f63fcfa4c87c15f9078c3d69927873a340c8daa8\r\n" +
                           "\r\n").getBytes(StandardCharsets.UTF_8));

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
        expectedBytesRead += (numChunks * (5 + chunkSize));

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
