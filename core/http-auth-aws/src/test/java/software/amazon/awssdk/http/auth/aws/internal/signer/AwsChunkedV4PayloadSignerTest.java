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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;

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

/**
 * Test the delegation of signing to the correct implementations.
 */
public class AwsChunkedV4PayloadSignerTest {

    int chunkSize = 4;

    CredentialScope credentialScope = new CredentialScope("us-east-1", "s3", Instant.EPOCH);

    byte[] data = "{\"TableName\": \"foo\"}".getBytes();

    ContentStreamProvider payload = () -> new ByteArrayInputStream(data);

    SdkHttpRequest.Builder requestBuilder;

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
        V4RequestSigningResult requestSigningResult = new V4RequestSigningResult(
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );
        AwsChunkedV4PayloadSigner signer = AwsChunkedV4PayloadSigner.builder()
                                                                    .credentialScope(credentialScope)
                                                                    .chunkSize(chunkSize)
                                                                    .build();

        signer.beforeSigning(requestBuilder, null);
        ContentStreamProvider signedPayload = signer.sign(payload, requestSigningResult);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));

        byte[] tmp = new byte[1024];
        int actualBytes = readAll(signedPayload.newStream(), tmp);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedContent, new String(tmp, 0, actualBytes));
    }

    @Test
    public void sign_withSignedPayloadAndChecksum_shouldChunkEncodeWithSigV4ExtAndSigV4Trailer() throws IOException {
        String expectedContent =
            "4;chunk-signature=082f5b0e588893570e152b401a886161ee772ed066948f68c8f01aee11cca4f8\r\n{\"Ta\r\n" +
            "4;chunk-signature=777b02ec61ce7934578b1efe6fbe08c21ae4a8cdf66a709d3b4fd320dddd2839\r\nbleN\r\n" +
            "4;chunk-signature=84abdae650f64dee4d703d41c7d87c8bc251c22b8c493c75ce24431b60b73937\r\name\"\r\n" +
            "4;chunk-signature=aff22ddad9d4388233fe9bc47e9c552a6e9ba9285af79555d2ce7fdaab726320\r\n: \"f\r\n" +
            "4;chunk-signature=30e55f4e1c1fd444c06e9be42d9594b8fd7ead436bc67a58b5350ffd58b6aaa5\r\noo\"}\r\n" +
            "0;chunk-signature=825ad80195cae47f54984835543ff2179c2c5a53c324059cd632e50259384ee3\r\n" +
            "x-amz-checksum-crc32:oL+a/g==\r\n" +
            "x-amz-trailer-signature:23457d04f4a8e279780cb91e28d4fbd1c6a2dd678d419705461a80514cea206c\r\n\r\n";

        requestBuilder.putHeader("x-amz-content-sha256", "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER");
        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER",
            new V4CanonicalRequest.Options(true, true)
        );
        V4RequestSigningResult requestSigningResult = new V4RequestSigningResult(
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );
        AwsChunkedV4PayloadSigner signer = AwsChunkedV4PayloadSigner.builder()
                                                                    .credentialScope(credentialScope)
                                                                    .chunkSize(chunkSize)
                                                                    .checksumAlgorithm(CRC32)
                                                                    .build();

        signer.beforeSigning(requestBuilder, payload);
        ContentStreamProvider signedPayload = signer.sign(payload, requestSigningResult);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-crc32");

        byte[] tmp = new byte[1024];
        int actualBytes = readAll(signedPayload.newStream(), tmp);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedContent, new String(tmp, 0, actualBytes));
    }

    @Test
    public void sign_withChecksum_shouldChunkEncodeWithChecksumTrailer() throws IOException {
        String expectedContent =
            "4\r\n{\"Ta\r\n" +
            "4\r\nbleN\r\n" +
            "4\r\name\"\r\n" +
            "4\r\n: \"f\r\n" +
            "4\r\noo\"}\r\n" +
            "0\r\n" +
            "x-amz-checksum-sha256:oVyCkrHRKru75BSGBfeHL732RWGP7lqw6AcqezTxVeI=\r\n\r\n";

        requestBuilder.putHeader("x-amz-content-sha256", "STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            new V4CanonicalRequest.Options(true, true)
        );
        V4RequestSigningResult requestSigningResult = new V4RequestSigningResult(
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );
        AwsChunkedV4PayloadSigner signer = AwsChunkedV4PayloadSigner.builder()
                                                                    .credentialScope(credentialScope)
                                                                    .chunkSize(chunkSize)
                                                                    .checksumAlgorithm(SHA256)
                                                                    .build();

        signer.beforeSigning(requestBuilder, payload);
        ContentStreamProvider signedPayload = signer.sign(payload, requestSigningResult);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-sha256");

        byte[] tmp = new byte[1024];
        int actualBytes = readAll(signedPayload.newStream(), tmp);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedContent, new String(tmp, 0, actualBytes));
    }

    @Test
    public void sign_withPreExistingTrailers_shouldChunkEncodeWithExistingTrailers() throws IOException {
        String expectedContent =
            "4\r\n{\"Ta\r\n" +
            "4\r\nbleN\r\n" +
            "4\r\name\"\r\n" +
            "4\r\n: \"f\r\n" +
            "4\r\noo\"}\r\n" +
            "0\r\n" +
            "PreExistingHeader1:someValue1,someValue2\r\n" +
            "PreExistingHeader2:someValue3\r\n\r\n";

        requestBuilder
            .putHeader("x-amz-content-sha256", "STREAMING-UNSIGNED-PAYLOAD-TRAILER")
            .appendHeader("PreExistingHeader1", "someValue1")
            .appendHeader("PreExistingHeader1", "someValue2")
            .appendHeader("PreExistingHeader2", "someValue3")
            .appendHeader("x-amz-trailer", "PreExistingHeader1")
            .appendHeader("x-amz-trailer", "PreExistingHeader2");

        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            new V4CanonicalRequest.Options(true, true)
        );
        V4RequestSigningResult requestSigningResult = new V4RequestSigningResult(
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );
        AwsChunkedV4PayloadSigner signer = AwsChunkedV4PayloadSigner.builder()
                                                                    .credentialScope(credentialScope)
                                                                    .chunkSize(chunkSize)
                                                                    .build();

        signer.beforeSigning(requestBuilder, payload);
        ContentStreamProvider signedPayload = signer.sign(payload, requestSigningResult);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));
        assertThat(requestBuilder.firstMatchingHeader("PreExistingHeader1")).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("PreExistingHeader2")).isNotPresent();
        assertThat(requestBuilder.matchingHeaders("x-amz-trailer")).contains("PreExistingHeader1", "PreExistingHeader2");

        byte[] tmp = new byte[1024];
        int actualBytes = readAll(signedPayload.newStream(), tmp);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedContent, new String(tmp, 0, actualBytes));
    }

    @Test
    public void sign_withPreExistingTrailersAndChecksum_shouldChunkEncodeWithTrailers() throws IOException {
        String expectedContent =
            "4\r\n{\"Ta\r\n" +
            "4\r\nbleN\r\n" +
            "4\r\name\"\r\n" +
            "4\r\n: \"f\r\n" +
            "4\r\noo\"}\r\n" +
            "0\r\n" +
            "PreExistingHeader1:someValue1,someValue2\r\n" +
            "PreExistingHeader2:someValue3\r\n" +
            "x-amz-checksum-crc32:oL+a/g==\r\n\r\n";

        requestBuilder
            .putHeader("x-amz-content-sha256", "STREAMING-UNSIGNED-PAYLOAD-TRAILER")
            .appendHeader("PreExistingHeader1", "someValue1")
            .appendHeader("PreExistingHeader1", "someValue2")
            .appendHeader("PreExistingHeader2", "someValue3")
            .appendHeader("x-amz-trailer", "PreExistingHeader1")
            .appendHeader("x-amz-trailer", "PreExistingHeader2");

        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            new V4CanonicalRequest.Options(true, true)
        );
        V4RequestSigningResult requestSigningResult = new V4RequestSigningResult(
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );
        AwsChunkedV4PayloadSigner signer = AwsChunkedV4PayloadSigner.builder()
                                                                    .credentialScope(credentialScope)
                                                                    .chunkSize(chunkSize)
                                                                    .checksumAlgorithm(CRC32)
                                                                    .build();

        signer.beforeSigning(requestBuilder, payload);
        ContentStreamProvider signedPayload = signer.sign(payload, requestSigningResult);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));
        assertThat(requestBuilder.firstMatchingHeader("PreExistingHeader1")).isNotPresent();
        assertThat(requestBuilder.firstMatchingHeader("PreExistingHeader2")).isNotPresent();
        assertThat(requestBuilder.matchingHeaders("x-amz-trailer")).contains(
            "PreExistingHeader1", "PreExistingHeader2", "x-amz-checksum-crc32"
        );

        byte[] tmp = new byte[1024];
        int actualBytes = readAll(signedPayload.newStream(), tmp);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedContent, new String(tmp, 0, actualBytes));
    }

    @Test
    public void sign_withPreExistingTrailersAndChecksumAndSignedPayload_shouldAwsChunkEncode() throws IOException {
        String expectedContent =
            "4;chunk-signature=082f5b0e588893570e152b401a886161ee772ed066948f68c8f01aee11cca4f8\r\n{\"Ta\r\n" +
            "4;chunk-signature=777b02ec61ce7934578b1efe6fbe08c21ae4a8cdf66a709d3b4fd320dddd2839\r\nbleN\r\n" +
            "4;chunk-signature=84abdae650f64dee4d703d41c7d87c8bc251c22b8c493c75ce24431b60b73937\r\name\"\r\n" +
            "4;chunk-signature=aff22ddad9d4388233fe9bc47e9c552a6e9ba9285af79555d2ce7fdaab726320\r\n: \"f\r\n" +
            "4;chunk-signature=30e55f4e1c1fd444c06e9be42d9594b8fd7ead436bc67a58b5350ffd58b6aaa5\r\noo\"}\r\n" +
            "0;chunk-signature=825ad80195cae47f54984835543ff2179c2c5a53c324059cd632e50259384ee3\r\n" +
            "zzz:123\r\n" +
            "PreExistingHeader1:someValue1\r\n" +
            "x-amz-checksum-crc32:oL+a/g==\r\n" +
            "x-amz-trailer-signature:3f65ab57ede6a5fb7c77b14b35faf2d9dd2c6d89828bdae189a04f3677bc16f2\r\n\r\n";

        requestBuilder
            .putHeader("x-amz-content-sha256", "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER")
            .appendHeader("PreExistingHeader1", "someValue1")
            .appendHeader("zzz", "123")
            .appendHeader("x-amz-trailer", "zzz")
            .appendHeader("x-amz-trailer", "PreExistingHeader1");

        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER",
            new V4CanonicalRequest.Options(true, true)
        );
        V4RequestSigningResult requestSigningResult = new V4RequestSigningResult(
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );
        AwsChunkedV4PayloadSigner signer = AwsChunkedV4PayloadSigner.builder()
                                                                    .credentialScope(credentialScope)
                                                                    .chunkSize(chunkSize)
                                                                    .checksumAlgorithm(CRC32)
                                                                    .build();

        signer.beforeSigning(requestBuilder, payload);
        ContentStreamProvider signedPayload = signer.sign(payload, requestSigningResult);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));
        assertThat(requestBuilder.firstMatchingHeader("PreExistingHeader1")).isNotPresent();
        assertThat(requestBuilder.matchingHeaders("x-amz-trailer")).contains("zzz", "PreExistingHeader1", "x-amz-checksum-crc32");

        byte[] tmp = new byte[1024];
        int actualBytes = readAll(signedPayload.newStream(), tmp);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedContent, new String(tmp, 0, actualBytes));
    }


    @Test
    public void sign_withoutContentLength_calculatesContentLengthFromPayload() throws IOException {
        String expectedContent =
            "4\r\n{\"Ta\r\n" +
            "4\r\nbleN\r\n" +
            "4\r\name\"\r\n" +
            "4\r\n: \"f\r\n" +
            "4\r\noo\"}\r\n" +
            "0\r\n" +
            "x-amz-checksum-sha256:oVyCkrHRKru75BSGBfeHL732RWGP7lqw6AcqezTxVeI=\r\n\r\n";

        requestBuilder.putHeader("x-amz-content-sha256", "STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        requestBuilder.removeHeader(Header.CONTENT_LENGTH);

        V4CanonicalRequest canonicalRequest = new V4CanonicalRequest(
            requestBuilder.build(),
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            new V4CanonicalRequest.Options(true, true)
        );
        V4RequestSigningResult requestSigningResult = new V4RequestSigningResult(
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );
        AwsChunkedV4PayloadSigner signer = AwsChunkedV4PayloadSigner.builder()
                                                                    .credentialScope(credentialScope)
                                                                    .chunkSize(chunkSize)
                                                                    .checksumAlgorithm(SHA256)
                                                                    .build();

        signer.beforeSigning(requestBuilder, payload);
        ContentStreamProvider signedPayload = signer.sign(payload, requestSigningResult);

        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).hasValue(Integer.toString(data.length));
        assertThat(requestBuilder.firstMatchingHeader("x-amz-trailer")).hasValue("x-amz-checksum-sha256");

        byte[] tmp = new byte[1024];
        int actualBytes = readAll(signedPayload.newStream(), tmp);

        assertThat(requestBuilder.firstMatchingHeader(Header.CONTENT_LENGTH)).hasValue(Integer.toString(actualBytes));
        assertEquals(expectedContent, new String(tmp, 0, actualBytes));
    }

    @Test
    public void sign_shouldReturnResettableContentStreamProvider() throws IOException {
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
        V4RequestSigningResult requestSigningResult = new V4RequestSigningResult(
            "STREAMING-AWS4-HMAC-SHA256-PAYLOAD",
            "key".getBytes(StandardCharsets.UTF_8),
            "sig",
            canonicalRequest,
            requestBuilder
        );
        AwsChunkedV4PayloadSigner signer = AwsChunkedV4PayloadSigner.builder()
                                                                    .credentialScope(credentialScope)
                                                                    .chunkSize(chunkSize)
                                                                    .build();

        signer.beforeSigning(requestBuilder, payload);
        ContentStreamProvider signedPayload = signer.sign(payload, requestSigningResult);

        // successive calls to newStream() should return a stream with the same data every time - this makes sure that state
        // isn't carried over to the new streams returned by newStream()
        byte[] tmp = new byte[1024];
        for (int i = 0; i < 2; i++) {
            int actualBytes = readAll(signedPayload.newStream(), tmp);
            assertEquals(expectedContent, new String(tmp, 0, actualBytes));
        }
    }

    @Test
    public void signAsync_throws() {
        AwsChunkedV4PayloadSigner signer = AwsChunkedV4PayloadSigner.builder()
                                                                    .credentialScope(credentialScope)
                                                                    .chunkSize(chunkSize)
                                                                    .build();

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
