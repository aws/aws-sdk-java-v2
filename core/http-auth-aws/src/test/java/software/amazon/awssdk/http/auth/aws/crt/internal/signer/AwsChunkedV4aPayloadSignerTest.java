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

package software.amazon.awssdk.http.auth.aws.crt.internal.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtUtils.toCredentials;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * Test the delegation of signing to the correct implementations.
 */
public class AwsChunkedV4aPayloadSignerTest {

    private static final int CHUNK_SIZE = 4;

    private static final CredentialScope CREDENTIAL_SCOPE = new CredentialScope("us-east-1", "s3", Instant.EPOCH);

    private static final byte[] DATA = "{\"TableName\": \"foo\"}".getBytes();

    private static final ContentStreamProvider PAYLOAD = () -> new ByteArrayInputStream(DATA);

    private SdkHttpRequest.Builder requestBuilder;

    @BeforeEach
    public void setUp() {
        requestBuilder = SdkHttpRequest
            .builder()
            .method(SdkHttpMethod.POST)
            .putHeader("Host", "demo.us-east-1.amazonaws.com")
            .putHeader("x-amz-archive-description", "test  test")
            .putHeader(Header.CONTENT_LENGTH, Integer.toString(DATA.length))
            .encodedPath("/")
            .uri(URI.create("http://demo.us-east-1.amazonaws.com"));
    }

    @Test
    public void sign_withSignedPayload_shouldChunkEncodeWithSigV4aExt() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD);
        V4aContext v4aContext = new V4aContext(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .build();

        signer.beforeSigning(requestBuilder, PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(PAYLOAD, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);

        int expectedBytes = expectedByteCount(DATA, CHUNK_SIZE);
        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_withSignedPayloadAndChecksum_shouldChunkEncodeWithSigV4aExtAndSigV4aTrailer() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER);
        V4aContext v4aContext = new V4aContext(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .checksumAlgorithm(CRC32)
                                                                      .build();

        signer.beforeSigning(requestBuilder, PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(PAYLOAD, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);
        int expectedBytes = expectedByteCount(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (checksum-header + checksum-value + \r\n + trailer-sig-header + trailer-sig + \r\n)
        expectedBytes += 21 + 8 + 2 + 24 + 144 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_withChecksum_shouldChunkEncodeWithChecksumTrailer() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        V4aContext v4aContext = new V4aContext(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .checksumAlgorithm(CRC32)
                                                                      .build();

        signer.beforeSigning(requestBuilder, PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(PAYLOAD, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);
        int expectedBytes = expectedByteCountUnsigned(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (checksum-header + checksum-value + \r\n)
        expectedBytes += 21 + 8 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_withPreExistingTrailers_shouldChunkEncodeWithExistingTrailers() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        V4aContext v4aContext = new V4aContext(
            requestBuilder
                .putHeader("x-amz-trailer", "aTrailer")
                .putHeader("aTrailer", "aValue"),
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .build();

        signer.beforeSigning(requestBuilder, PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(PAYLOAD, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("aTrailer")).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("aTrailer");

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);
        int expectedBytes = expectedByteCountUnsigned(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (aTrailer: + aValue + \r\n)
        expectedBytes += 9 + 6 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_withPreExistingTrailersAndChecksum_shouldChunkEncodeWithTrailers() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        V4aContext v4aContext = new V4aContext(
            requestBuilder
                .putHeader("x-amz-trailer", "aTrailer")
                .putHeader("aTrailer", "aValue"),
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .checksumAlgorithm(CRC32)
                                                                      .build();

        signer.beforeSigning(requestBuilder, PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(PAYLOAD, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("aTrailer")).isNotPresent();
        assertThat(requestBuilder.matchingHeaders("x-amz-trailer")).contains("aTrailer", "x-amz-checksum-crc32");

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);
        int expectedBytes = expectedByteCountUnsigned(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (aTrailer: + aValue + \r\n + checksum-header + checksum-value + \r\n)
        expectedBytes += 9 + 6 + 2 + 21 + 8 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_withPreExistingTrailersAndChecksumAndSignedPayload_shouldAwsChunkEncode() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER);
        V4aContext v4aContext = new V4aContext(
            requestBuilder
                .putHeader("x-amz-trailer", "aTrailer")
                .putHeader("aTrailer", "aValue"),
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .checksumAlgorithm(CRC32)
                                                                      .build();

        signer.beforeSigning(requestBuilder, PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(PAYLOAD, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("aTrailer")).isNotPresent();
        assertThat(requestBuilder.matchingHeaders("x-amz-trailer")).contains("aTrailer", "x-amz-checksum-crc32");

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);
        int expectedBytes = expectedByteCount(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (aTrailer: + aValue + \r\n + checksum-header + checksum-value + \r\n + trailer-sig-header + trailer-sig + \r\n)
        expectedBytes += 9 + 6 + 2 + 21 + 8 + 2 + 24 + 144 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_withoutContentLength_calculatesContentLengthFromPayload() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        V4aContext v4aContext = new V4aContext(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .checksumAlgorithm(CRC32)
                                                                      .build();

        requestBuilder.removeHeader(Header.CONTENT_LENGTH);
        signer.beforeSigning(requestBuilder, PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(PAYLOAD, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);
        int expectedBytes = expectedByteCountUnsigned(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (checksum-header + checksum-value + \r\n)
        expectedBytes += 21 + 8 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_shouldReturnResettableContentStreamProvider() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD);
        V4aContext v4aContext = new V4aContext(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .build();

        signer.beforeSigning(requestBuilder, PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(PAYLOAD, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));

        byte[] tmp = new byte[2048];
        int expectedBytes = expectedByteCount(DATA, CHUNK_SIZE);

        // successive calls to newStream() should return a stream with the same data every time - this makes sure that state
        // isn't carried over to the new streams returned by newStream()
        for (int i = 0; i < 2; i++) {
            int actualBytes = readAll(signedPayload.newStream(), tmp);
            assertEquals(expectedBytes, actualBytes);
        }
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

    private AwsSigningConfig basicSigningConfig() {
        AwsSigningConfig signingConfig = new AwsSigningConfig();

        signingConfig.setCredentials(toCredentials(AwsCredentialsIdentity.create("key", "secret")));
        signingConfig.setService("s3");
        signingConfig.setRegion("aws-global");
        signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        signingConfig.setTime(Instant.now().toEpochMilli());
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_CHUNK);

        return signingConfig;
    }

    private int expectedByteCount(byte[] data, int chunkSize) {
        int size = data.length;
        int ecdsaSignatureLength = 144;
        int chunkHeaderLength = 17 + Integer.toHexString(chunkSize).length();
        int numChunks = size / chunkSize;
        int expectedBytes = 0;

        // normal chunks
        // x;chunk-signature=<ecdsa>\r\n<data>\r\n
        expectedBytes += numChunks * (chunkHeaderLength + ecdsaSignatureLength + chunkSize + 4);

        // remaining chunk
        // n;chunk-signature=<ecdsa>\r\n\<remaining-data>\r\n
        int remainingBytes = size % chunkSize;
        if (remainingBytes > 0) {
            int remainingChunkHeaderLength = 17 + Integer.toHexString(remainingBytes).length();
            expectedBytes += remainingChunkHeaderLength + ecdsaSignatureLength + remainingBytes + 4;
        }

        // final chunk
        // 0;chunk-signature=<ecsda>\r\n\r\n
        int finalBytes = 0;
        int finalChunkHeaderLength = 17 + Integer.toHexString(finalBytes).length();
        expectedBytes += finalChunkHeaderLength + ecdsaSignatureLength + 4;

        return expectedBytes;
    }

    private int expectedByteCountUnsigned(byte[] data, int chunkSize) {
        int size = data.length;
        int chunkHeaderLength = Integer.toHexString(chunkSize).length();
        int numChunks = size / chunkSize;
        int expectedBytes = 0;

        // normal chunks
        // x\r\n<data>\r\n
        expectedBytes += numChunks * (chunkHeaderLength + chunkSize + 4);

        // remaining chunk
        // n\r\n\<remaining-data>\r\n
        int remainingBytes = size % chunkSize;
        if (remainingBytes > 0) {
            int remainingChunkHeaderLength = Integer.toHexString(remainingBytes).length();
            expectedBytes += remainingChunkHeaderLength + remainingBytes + 4;
        }

        // final chunk
        // 0\r\n\r\n
        int finalBytes = 0;
        int finalChunkHeaderLength = Integer.toHexString(finalBytes).length();
        expectedBytes += finalChunkHeaderLength + 4;

        return expectedBytes;
    }
}
