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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtUtils.toCredentials;
import static software.amazon.awssdk.http.auth.aws.internal.util.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.util.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.util.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;

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

    int chunkSize = 4;

    CredentialScope credentialScope = new CredentialScope("us-east-1", "s3", Instant.EPOCH);

    byte[] data = "{\"TableName\": \"foo\"}".getBytes();

    ContentStreamProvider payload = () -> new ByteArrayInputStream(data);

    SdkHttpRequest.Builder requestBuilder;

    AwsChunkedV4aPayloadSigner signer = new AwsChunkedV4aPayloadSigner(credentialScope, chunkSize);

    @BeforeEach
    public void setUp() {
        requestBuilder = SdkHttpRequest
            .builder()
            .method(SdkHttpMethod.POST)
            .putHeader("Host", "demo.us-east-1.amazonaws.com")
            .putHeader("x-amz-archive-description", "test  test")
            .putHeader(Header.CONTENT_LENGTH, Integer.toString(data.length))
            .encodedPath("/")
            .uri(URI.create("http://demo.us-east-1.amazonaws.com"));
    }

    @Test
    public void sign_withSignedPayload_shouldChunkEncodeWithSigV4Ext() throws IOException {
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD);
        V4aContext v4aContext = new V4aContext(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );

        ContentStreamProvider signedPayload = signer.sign(payload, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);

        int expectedBytes = expectedByteCount(data, chunkSize);
        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_withSignedPayloadAndTrailer_shouldChunkEncodeWithSigV4ExtAndSigV4Trailer() throws IOException {
        // TODO: Update trailer here when flexible checksums is implemented
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER);
        V4aContext v4aContext = new V4aContext(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );

        ContentStreamProvider signedPayload = signer.sign(payload, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);
        int expectedBytes = expectedByteCount(data, chunkSize);
        // include trailer size + trailer signature size
        expectedBytes += 26 + 144;

        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_withTrailer_shouldChunkEncodeWithTrailer() throws IOException {
        // TODO: Update trailer here when flexible checksums is implemented
        AwsSigningConfig signingConfig = basicSigningConfig();
        signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        V4aContext v4aContext = new V4aContext(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            signingConfig
        );

        ContentStreamProvider signedPayload = signer.sign(payload, v4aContext);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));

        byte[] tmp = new byte[2048];
        int actualBytes = readAll(signedPayload.newStream(), tmp);
        int expectedBytes = expectedByteCountUnsigned(data, chunkSize);

        assertEquals(expectedBytes, actualBytes);
    }

    @Test
    public void sign_withoutContentLength_throws() {
        V4aContext v4aContext = new V4aContext(
            requestBuilder,
            "sig".getBytes(StandardCharsets.UTF_8),
            null
        );
        requestBuilder.removeHeader(Header.CONTENT_LENGTH);

        assertThrows(IllegalArgumentException.class, () -> signer.sign(payload, v4aContext));
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
