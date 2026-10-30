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
import software.amazon.awssdk.services.s3.crt.S3CrtResponseTransformerFactory;
import software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/** Verifies that borrowed streams coexist with ordinary downloads and uploads on a client with a direct buffer pool. */
@WireMockTest
@Timeout(60)
class S3CrtBorrowedBufferMixedWorkloadWireMockTest {
    // S3's multipart minimum. With a pool, uploads below it are rejected instead of resized, so the uploads here need at
    // least this part size.
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

        // Room for all three requests at once, so the test checks a held lease rather than pool exhaustion.
        try (S3AsyncClient client = newClient(wireMock, 8L * PART_SIZE)) {
            ResponseInputStream<GetObjectResponse> borrowed =
                client.getObject(r -> r.bucket("bucket").key("download"),
                                 S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool())
                      .get(20, TimeUnit.SECONDS);

            // Read one byte, so the first buffer stays leased for the rest of the test.
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

        // CRT may send one PUT or several UploadPart PUTs, so reassemble the parts in order before comparing.
        assertThat(reassembleUploadedParts()).containsExactly(UPLOAD_CONTENT);
    }

    @Test
    void borrowedAndOrdinaryDownloadsInterleaved_shouldEachSeeTheirOwnBytes(WireMockRuntimeInfo wireMock)
        throws Exception {
        stubDownload();

        // A one-part pool makes both downloads use the same slot, so this checks the borrowed stream returns it.
        try (S3AsyncClient client = newClient(wireMock, (long) PART_SIZE)) {
            for (int round = 0; round < 3; round++) {
                try (ResponseInputStream<GetObjectResponse> borrowed =
                         client.getObject(r -> r.bucket("bucket").key("download"),
                                          S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool())
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
                            // Without this, CRT sends aws-chunked bodies that don't match the source bytes.
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
