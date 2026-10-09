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

package software.amazon.awssdk.services.s3.internal.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/** Verifies that borrowed streams coexist with ordinary downloads and uploads on a shared pooled client. */
@WireMockTest
@Timeout(60)
class S3CrtBorrowedBufferMixedWorkloadWireMockTest {
    // S3's multipart minimum. A smaller part size is fine for downloads, but with a direct buffer pool configured an
    // upload below this minimum is rejected rather than silently resized, so the mixed workload has to use a part size
    // that is legal for every operation on the client. See S3CrtBorrowedBufferUploadPartSizeWireMockTest.
    private static final int PART_SIZE = 5 * 1024 * 1024;
    private static final int OBJECT_SIZE = 2 * PART_SIZE;
    private static final byte[] DOWNLOAD_CONTENT = payload(0, OBJECT_SIZE);
    private static final byte[] UPLOAD_CONTENT = payload(7, 3 * PART_SIZE);
    private static final String E_TAG = "\"mixed-etag\"";

    @Test
    void heldOpenBorrowedStream_shouldNotBlockOrdinaryDownloadOrUploadOnSameClient(WireMockRuntimeInfo wireMock)
        throws Exception {
        stubDownload();
        stubUpload();

        // Eight parts' worth of pool. Deliberately not tight: this test is about a lease being held across unrelated
        // traffic, not about exhaustion, and a pool too small to serve all three at once would turn a correctness
        // assertion into a timing one.
        try (S3AsyncClient client = newClient(wireMock, 8L * PART_SIZE)) {
            ResponseInputStream<GetObjectResponse> borrowed =
                client.getObject(r -> r.bucket("bucket").key("download"),
                                 S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers())
                      .get(20, TimeUnit.SECONDS);

            // Read one byte and then stop. The first borrowed buffer is now leased to this reader and stays leased for
            // the rest of the test.
            int firstByte = borrowed.read();
            assertThat(firstByte).isEqualTo(Byte.toUnsignedInt(DOWNLOAD_CONTENT[0]));

            // Both of these run while the lease above is outstanding.
            CompletableFuture<byte[]> ordinary =
                client.getObject(r -> r.bucket("bucket").key("download"), AsyncResponseTransformer.toBytes())
                      .thenApply(bytes -> bytes.asByteArray());
            CompletableFuture<PutObjectResponse> upload =
                client.putObject(r -> r.bucket("bucket").key("upload").contentLength((long) UPLOAD_CONTENT.length),
                                 AsyncRequestBody.fromBytes(UPLOAD_CONTENT));

            assertThat(ordinary.get(20, TimeUnit.SECONDS)).containsExactly(DOWNLOAD_CONTENT);
            assertThat(upload.get(20, TimeUnit.SECONDS)).isNotNull();

            // Only now drain the borrowed stream. It must still deliver every remaining byte in order.
            byte[] rest = readAll(borrowed);
            assertThat(rest).containsExactly(Arrays.copyOfRange(DOWNLOAD_CONTENT, 1, DOWNLOAD_CONTENT.length));
            assertThat(borrowed.read()).isEqualTo(-1);
            borrowed.close();
        }

        // The upload really transferred its content rather than being short-circuited by a catch-all stub. CRT may send
        // this as one PUT or as several UploadPart PUTs, so the parts are reassembled in part-number order and compared
        // against the source bytes.
        assertThat(reassembleUploadedParts()).containsExactly(UPLOAD_CONTENT);
    }

    @Test
    void borrowedAndOrdinaryDownloadsInterleaved_shouldEachSeeTheirOwnBytes(WireMockRuntimeInfo wireMock)
        throws Exception {
        stubDownload();

        // A single-part pool forces the two downloads to take turns through the same pool slot. The borrowed reader is
        // fully drained before the ordinary download starts, so this verifies the slot is genuinely returned rather
        // than verifying that two leases can coexist.
        try (S3AsyncClient client = newClient(wireMock, (long) PART_SIZE)) {
            for (int round = 0; round < 3; round++) {
                try (ResponseInputStream<GetObjectResponse> borrowed =
                         client.getObject(r -> r.bucket("bucket").key("download"),
                                          S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers())
                               .get(20, TimeUnit.SECONDS)) {
                    assertThat(readAll(borrowed)).containsExactly(DOWNLOAD_CONTENT);
                }

                byte[] ordinary = client.getObject(r -> r.bucket("bucket").key("download"),
                                                   AsyncResponseTransformer.toBytes())
                                        .get(20, TimeUnit.SECONDS)
                                        .asByteArray();
                assertThat(ordinary).containsExactly(DOWNLOAD_CONTENT);
            }
        }
    }

    /** Reassembles recorded single-part or multipart upload bodies. */
    private static byte[] reassembleUploadedParts() throws IOException {
        List<LoggedRequest> puts = new ArrayList<>(findAll(putRequestedFor(anyUrl())));
        puts.sort(Comparator.comparingInt(request -> {
            String partNumber = request.queryParameter("partNumber").isPresent()
                                ? request.queryParameter("partNumber").firstValue() : "0";
            return Integer.parseInt(partNumber);
        }));
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        for (LoggedRequest put : puts) {
            result.write(put.getBody());
        }
        return result.toByteArray();
    }

    private static S3AsyncClient newClient(WireMockRuntimeInfo wireMock, long poolBytes) {
        return S3AsyncClient.crtBuilder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(URI.create("http://localhost:" + wireMock.getHttpPort()))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("key", "secret")))
                            .minimumPartSizeInBytes((long) PART_SIZE)
                            .initialReadBufferSizeInBytes((long) PART_SIZE)
                            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                            // Keeps the uploaded parts as raw bytes on the wire. With checksums on, CRT sends
                            // aws-chunked frames and the recorded request bodies carry framing, which would make an
                            // exact byte comparison against the source content meaningless.
                            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                            .directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(poolBytes))
                            .build();
    }

    private static void stubDownload() {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("Content-Length", Integer.toString(OBJECT_SIZE))
                                                    .withHeader("ETag", E_TAG)));
        for (int offset = 0; offset < OBJECT_SIZE; offset += PART_SIZE) {
            stubRange(offset, Math.min(offset + PART_SIZE, OBJECT_SIZE) - 1);
        }
    }

    private static void stubRange(int from, int to) {
        byte[] body = Arrays.copyOfRange(DOWNLOAD_CONTENT, from, to + 1);
        stubFor(get(anyUrl()).withHeader("Range", equalTo("bytes=" + from + "-" + to))
                             .willReturn(aResponse().withStatus(206)
                                                   .withHeader("Content-Length", Integer.toString(body.length))
                                                   .withHeader("Content-Range",
                                                               "bytes " + from + "-" + to + "/" + OBJECT_SIZE)
                                                   .withHeader("ETag", E_TAG)
                                                   .withBody(body)));
    }

    /** Stubs both single-part and multipart upload responses. */
    private static void stubUpload() {
        stubFor(post(anyUrl()).withQueryParam("uploads", equalTo(""))
                              .willReturn(aResponse().withStatus(200)
                                                     .withHeader("Content-Type", "application/xml")
                                                     .withBody("<InitiateMultipartUploadResult>"
                                                               + "<Bucket>bucket</Bucket><Key>upload</Key>"
                                                               + "<UploadId>upload-id</UploadId>"
                                                               + "</InitiateMultipartUploadResult>")));
        stubFor(post(anyUrl()).withQueryParam("uploadId", equalTo("upload-id"))
                              .willReturn(aResponse().withStatus(200)
                                                     .withHeader("Content-Type", "application/xml")
                                                     .withBody("<CompleteMultipartUploadResult>"
                                                               + "<Location>http://localhost/bucket/upload</Location>"
                                                               + "<Bucket>bucket</Bucket><Key>upload</Key>"
                                                               + "<ETag>" + E_TAG + "</ETag>"
                                                               + "</CompleteMultipartUploadResult>")));
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", E_TAG)));
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            result.write(buffer, 0, read);
        }
        return result.toByteArray();
    }

    private static byte[] payload(int seed, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) ((i + seed) % 251);
        }
        return result;
    }
}
