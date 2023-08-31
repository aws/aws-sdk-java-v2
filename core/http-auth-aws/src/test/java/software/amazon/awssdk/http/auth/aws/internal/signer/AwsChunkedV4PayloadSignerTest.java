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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.signer.V4CanonicalRequest;
import software.amazon.awssdk.http.auth.aws.signer.V4Context;

/**
 * Test the delegation of signing to the correct implementations.
 */
public class AwsChunkedV4PayloadSignerTest {

    int chunkSize = 4;

    CredentialScope credentialScope = new CredentialScope("us-east-1", "s3", Instant.EPOCH);

    byte[] data = "{\"TableName\": \"foo\"}".getBytes();

    ContentStreamProvider payload = () -> new ByteArrayInputStream(data);

    SdkHttpRequest.Builder requestBuilder;

    AwsChunkedV4PayloadSigner signer = new AwsChunkedV4PayloadSigner(credentialScope, chunkSize);

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
        String expectedContent =
            "4;chunk-signature=082f5b0e588893570e152b401a886161ee772ed066948f68c8f01aee11cca4f8\r\n{\"Ta\r\n" +
            "4;chunk-signature=777b02ec61ce7934578b1efe6fbe08c21ae4a8cdf66a709d3b4fd320dddd2839\r\nbleN\r\n" +
            "4;chunk-signature=84abdae650f64dee4d703d41c7d87c8bc251c22b8c493c75ce24431b60b73937\r\name\"\r\n" +
            "4;chunk-signature=aff22ddad9d4388233fe9bc47e9c552a6e9ba9285af79555d2ce7fdaab726320\r\n: \"f\r\n" +
            "4;chunk-signature=30e55f4e1c1fd444c06e9be42d9594b8fd7ead436bc67a58b5350ffd58b6aaa5\r\noo\"}\r\n" +
            "0;chunk-signature=825ad80195cae47f54984835543ff2179c2c5a53c324059cd632e50259384ee3\r\n\r\n";

        requestBuilder.putHeader("x-amz-content-sha256", "STREAMING-AWS4-HMAC-SHA256-PAYLOAD");
        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD",
            new V4CanonicalRequest.Options(true, true)
        );
        V4Context v4Context = new V4Context(
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );

        ContentStreamProvider signedPayload = signer.sign(payload, v4Context);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));

        byte[] tmp = new byte[1024];
        readAll(signedPayload.newStream(), tmp);

        assertEquals(expectedContent, new String(tmp, 0, expectedContent.length()));
    }

    @Test
    public void sign_withSignedPayloadAndTrailer_shouldChunkEncodeWithSigV4ExtAndSigV4Trailer() throws IOException {
        // TODO(sra-identity-and-auth): Update trailer here when flexible checksums is implemented
        String expectedContent =
            "4;chunk-signature=082f5b0e588893570e152b401a886161ee772ed066948f68c8f01aee11cca4f8\r\n{\"Ta\r\n" +
            "4;chunk-signature=777b02ec61ce7934578b1efe6fbe08c21ae4a8cdf66a709d3b4fd320dddd2839\r\nbleN\r\n" +
            "4;chunk-signature=84abdae650f64dee4d703d41c7d87c8bc251c22b8c493c75ce24431b60b73937\r\name\"\r\n" +
            "4;chunk-signature=aff22ddad9d4388233fe9bc47e9c552a6e9ba9285af79555d2ce7fdaab726320\r\n: \"f\r\n" +
            "4;chunk-signature=30e55f4e1c1fd444c06e9be42d9594b8fd7ead436bc67a58b5350ffd58b6aaa5\r\noo\"}\r\n" +
            "0;chunk-signature=825ad80195cae47f54984835543ff2179c2c5a53c324059cd632e50259384ee3\r\n" +
            "x-amz-trailer-signature:4cf8567e47f3f5b4404edce2ff8719ce99434b092a34f8faac804d63f9700871\r\n\r\n";

        requestBuilder.putHeader("x-amz-content-sha256", "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER");
        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER",
            new V4CanonicalRequest.Options(true, true)
        );
        V4Context v4Context = new V4Context(
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );

        ContentStreamProvider signedPayload = signer.sign(payload, v4Context);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));

        byte[] tmp = new byte[1024];
        readAll(signedPayload.newStream(), tmp);

        assertEquals(expectedContent, new String(tmp, 0, expectedContent.length()));
    }

    @Test
    public void sign_withTrailer_shouldChunkEncodeWithTrailer() throws IOException {
        // TODO(sra-identity-and-auth): Update trailer here when flexible checksums is implemented
        String expectedContent =
            "4\r\n{\"Ta\r\n" +
            "4\r\nbleN\r\n" +
            "4\r\name\"\r\n" +
            "4\r\n: \"f\r\n" +
            "4\r\noo\"}\r\n" +
            "0\r\n";

        requestBuilder.putHeader("x-amz-content-sha256", "STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            new V4CanonicalRequest.Options(true, true)
        );
        V4Context v4Context = new V4Context(
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );

        ContentStreamProvider signedPayload = signer.sign(payload, v4Context);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));

        byte[] tmp = new byte[1024];
        readAll(signedPayload.newStream(), tmp);

        assertEquals(expectedContent, new String(tmp, 0, expectedContent.length()));
    }

    @Test
    public void sign_withoutContentLength_throws() {
        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            new V4CanonicalRequest.Options(true, true)
        );
        V4Context v4Context = new V4Context(
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );
        v4Context.getSignedRequest().removeHeader(Header.CONTENT_LENGTH);

        assertThrows(IllegalArgumentException.class, () -> signer.sign(payload, v4Context));
    }

    @Test
    public void signAsync_throws() {
        assertThrows(UnsupportedOperationException.class, () -> signer.signAsync(null, null));
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
