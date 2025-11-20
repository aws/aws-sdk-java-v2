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
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * Test the delegation of signing to the correct implementations.
 */
public class AwsChunkedV4aPayloadSignerTest {

    private static final int CHUNK_SIZE = 4;

    private static final CredentialScope CREDENTIAL_SCOPE = new CredentialScope("us-east-1", "s3", Instant.EPOCH);

    private static final byte[] DATA = "{\"TableName\": \"foo\"}".getBytes();

    private static final ContentStreamProvider SYNC_PAYLOAD = () -> new ByteArrayInputStream(DATA);

    private SdkHttpRequest.Builder requestBuilder;

    @BeforeEach
    void setUp() {
        requestBuilder = SdkHttpRequest
            .builder()
            .method(SdkHttpMethod.POST)
            .putHeader("Host", "demo.us-east-1.amazonaws.com")
            .putHeader("x-amz-archive-description", "test  test")
            .putHeader(Header.CONTENT_LENGTH, Integer.toString(DATA.length))
            .encodedPath("/")
            .uri(URI.create("http://demo.us-east-1.amazonaws.com"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signingImpls")
    void sign_withSignedPayload_shouldChunkEncodeWithSigV4aExt(String name, SigningImplementation impl) throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD);
        V4aRequestSigningResult result = new V4aRequestSigningResult(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .build();

        Pair<SdkHttpRequest.Builder, byte[]> signingResult = impl.sign(signer, result);

        SdkHttpRequest.Builder finalResult = signingResult.left();
        byte[] payloadBytes = signingResult.right();

        assertThat(finalResult.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));

        int expectedBytes = expectedByteCount(DATA, CHUNK_SIZE);
        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(payloadBytes.length));
        assertEquals(expectedBytes, payloadBytes.length);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signingImpls")
    void sign_withSignedPayloadAndChecksum_shouldChunkEncodeWithSigV4aExtAndSigV4aTrailer(String name,
                                                                                                 SigningImplementation impl) throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER);
        V4aRequestSigningResult result = new V4aRequestSigningResult(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .checksumAlgorithm(CRC32)
                                                                      .build();

        Pair<SdkHttpRequest.Builder, byte[]> signingResult = impl.sign(signer, result);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");

        int actualBytes = signingResult.right().length;
        int expectedBytes = expectedByteCount(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (checksum-header + checksum-value + \r\n + trailer-sig-header + trailer-sig + \r\n)
        expectedBytes += 21 + 8 + 2 + 24 + 144 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signingImpls")
    void sign_withChecksum_shouldChunkEncodeWithChecksumTrailer(String name, SigningImplementation impl) throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        V4aRequestSigningResult result = new V4aRequestSigningResult(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .checksumAlgorithm(CRC32)
                                                                      .build();

        Pair<SdkHttpRequest.Builder, byte[]> signingResult = impl.sign(signer, result);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");

        int actualBytes = signingResult.right().length;
        int expectedBytes = expectedByteCountUnsigned(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (checksum-header + checksum-value + \r\n)
        expectedBytes += 21 + 8 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signingImpls")
    void sign_withPreExistingTrailers_shouldChunkEncodeWithExistingTrailers(String name, SigningImplementation impl) throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        V4aRequestSigningResult result = new V4aRequestSigningResult(
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

        Pair<SdkHttpRequest.Builder, byte[]> signingResult = impl.sign(signer, result);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("aTrailer")).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("aTrailer");

        int actualBytes = signingResult.right().length;
        int expectedBytes = expectedByteCountUnsigned(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (aTrailer: + aValue + \r\n)
        expectedBytes += 9 + 6 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signingImpls")
    void sign_withPreExistingTrailersAndChecksum_shouldChunkEncodeWithTrailers(String name, SigningImplementation impl) throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        V4aRequestSigningResult result = new V4aRequestSigningResult(
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

        Pair<SdkHttpRequest.Builder, byte[]> signingResult = impl.sign(signer, result);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("aTrailer")).isNotPresent();
        assertThat(requestBuilder.matchingHeaders("x-amz-trailer")).contains("aTrailer", "x-amz-checksum-crc32");

        int actualBytes = signingResult.right().length;
        int expectedBytes = expectedByteCountUnsigned(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (aTrailer: + aValue + \r\n + checksum-header + checksum-value + \r\n)
        expectedBytes += 9 + 6 + 2 + 21 + 8 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signingImpls")
    void sign_withPreExistingTrailersAndChecksumAndSignedPayload_shouldAwsChunkEncode(String name,
                                                                                             SigningImplementation impl) throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER);
        V4aRequestSigningResult result = new V4aRequestSigningResult(
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

        Pair<SdkHttpRequest.Builder, byte[]> signingResult = impl.sign(signer, result);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(DATA.length));
        assertThat(requestBuilder.firstMatchingHeader("aTrailer")).isNotPresent();
        assertThat(requestBuilder.matchingHeaders("x-amz-trailer")).contains("aTrailer", "x-amz-checksum-crc32");

        int actualBytes = signingResult.right().length;
        int expectedBytes = expectedByteCount(DATA, CHUNK_SIZE);
        // include trailer bytes in the count:
        // (aTrailer: + aValue + \r\n + checksum-header + checksum-value + \r\n + trailer-sig-header + trailer-sig + \r\n)
        expectedBytes += 9 + 6 + 2 + 21 + 8 + 2 + 24 + 144 + 2;

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    void sign_withoutContentLength_calculatesContentLengthFromPayload() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        V4aRequestSigningResult result = new V4aRequestSigningResult(
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
        signer.beforeSigning(requestBuilder, SYNC_PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(SYNC_PAYLOAD, result);

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
    void sign_shouldReturnResettableContentStreamProvider() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD);
        V4aRequestSigningResult result = new V4aRequestSigningResult(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );
        AwsChunkedV4aPayloadSigner signer = AwsChunkedV4aPayloadSigner.builder()
                                                                      .credentialScope(CREDENTIAL_SCOPE)
                                                                      .chunkSize(CHUNK_SIZE)
                                                                      .build();

        signer.beforeSigning(requestBuilder, SYNC_PAYLOAD, signingConfig.getSignedBodyValue());
        ContentStreamProvider signedPayload = signer.sign(SYNC_PAYLOAD, result);

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

    public static Stream<Arguments> signingImpls() {
        return Stream.of(
            Arguments.of("SYNC", (SigningImplementation) AwsChunkedV4aPayloadSignerTest::doSign),
            Arguments.of("ASYNC", (SigningImplementation) AwsChunkedV4aPayloadSignerTest::doSignAsync)

        );
    }

    private static Pair<SdkHttpRequest.Builder, byte[]> doSign(AwsChunkedV4aPayloadSigner signer,
                                                               V4aRequestSigningResult requestSigningResult) {
        SdkHttpRequest.Builder request = requestSigningResult.getSignedRequest();

        ContentStreamProvider payload = () -> new ByteArrayInputStream(DATA);
        signer.beforeSigning(request, payload, requestSigningResult.getSigningConfig().getSignedBodyValue());

        ContentStreamProvider signedPayload = signer.sign(payload, requestSigningResult);

        try {
            byte[] signedPayloadBytes = IoUtils.toByteArray(signedPayload.newStream());
            return Pair.of(request, signedPayloadBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Pair<SdkHttpRequest.Builder, byte[]> doSignAsync(AwsChunkedV4aPayloadSigner signer,
                                                               V4aRequestSigningResult requestSigningResult) {
        SdkHttpRequest.Builder request = requestSigningResult.getSignedRequest();

        ByteBuffer dataBuffer = ByteBuffer.wrap(DATA);
        dataBuffer.mark();
        // Ensure buffer is always reset before the downstream publisher receives it
        Publisher<ByteBuffer> payload = Flowable.just(dataBuffer).doOnNext(ByteBuffer::reset);

        Pair<SdkHttpRequest.Builder, Optional<Publisher<ByteBuffer>>> beforeSigningResult =
            signer.beforeSigningAsync(request, payload, requestSigningResult.getSigningConfig().getSignedBodyValue()).join();

        request = beforeSigningResult.left();

        Publisher<ByteBuffer> signedPayload = signer.signAsync(beforeSigningResult.right().get(), requestSigningResult);

        TestSubscriber<ByteBuffer> subscriber = new TestSubscriber<>();
        signedPayload.subscribe(subscriber);

        subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS);
        subscriber.assertComplete();

        List<ByteBuffer> signedData = subscriber.values();

        int signedDataSum = signedData.stream().mapToInt(ByteBuffer::remaining).sum();
        byte[] array = new byte[signedDataSum];

        ByteBuffer combined = ByteBuffer.wrap(array);
        signedData.forEach(combined::put);

        return Pair.of(request, array);
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

    interface SigningImplementation {
        Pair<SdkHttpRequest.Builder, byte[]> sign(AwsChunkedV4aPayloadSigner signer,
                                                  V4aRequestSigningResult requestSigningResult);
    }
}
