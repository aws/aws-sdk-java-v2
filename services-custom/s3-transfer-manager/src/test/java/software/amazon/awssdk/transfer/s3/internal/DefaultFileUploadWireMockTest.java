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

package software.amazon.awssdk.transfer.s3.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

/**
 * WireMock test for verifying the DefaultFileUpload codepath.
 */
public class DefaultFileUploadWireMockTest {
    private static final WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    private static final FileSystem testFs = Jimfs.newFileSystem(Configuration.unix());
    private static Path testFile;
    private static S3AsyncClient s3;

    @BeforeAll
    public static void setup() {
        testFile = testFs.getPath("/32mib.dat");
        writeTestFile(testFile, 32 * 1024 * 1024);
        wireMock.start();
    }

    @AfterAll
    public static void teardown() throws IOException {
        testFs.close();
        wireMock.stop();
    }

    @BeforeEach
    public void methodSetup() {
        s3 = S3AsyncClient.builder()
                          .credentialsProvider(StaticCredentialsProvider.create(
                              AwsBasicCredentials.create("akid", "skid")))
                          .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                          .region(Region.US_EAST_1)
                          .forcePathStyle(true)
                          .multipartEnabled(true)
                          .multipartConfiguration(c -> c.thresholdInBytes(16 * 1024 * 1024L))
                          .overrideConfiguration(o -> o.retryPolicy(RetryPolicy.defaultRetryPolicy().toBuilder()
                                                                               .numRetries(3)
                                                                               .build()))
                          .build();
    }

    @AfterEach
    public void methodTeardown() {
        s3.close();
    }

    @Test
    void retryableErrorDuringUpload_shouldSupportRetries() {
        S3TransferManager tm = S3TransferManager.builder().s3Client(s3).build();

        String mpuInitBody = ""
                             + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                             + "<InitiateMultipartUploadResult>\n"
                             + "   <Bucket>bucket</Bucket>\n"
                             + "   <Key>key</Key>\n"
                             + "   <UploadId>uploadId</UploadId>\n"
                             + "</InitiateMultipartUploadResult>";

        wireMock.stubFor(post(urlEqualTo("/bucket/key?uploads"))
                                 .willReturn(aResponse()
                                                 .withStatus(200)
                                                 .withBody(mpuInitBody)));

        wireMock.stubFor(put(anyUrl())
                             .willReturn(aResponse()
                                             .withStatus(500)
                                             .withBody("Internal Error")));

        UploadFileRequest request = UploadFileRequest.builder()
                                                     .source(testFile)
                                                     .putObjectRequest(put -> put.bucket("bucket").key("key"))
                                                     .build();

        assertThatThrownBy(() -> tm.uploadFile(request).completionFuture().join())
            .hasCauseInstanceOf(S3Exception.class);

        wireMock.verify(moreThanOrExactly(3),
                        putRequestedFor(urlPathMatching("/bucket/key"))
                            .withQueryParam("uploadId", matching("uploadId"))
                            .withQueryParam("partNumber", matching("1")));
    }

    private static void writeTestFile(Path file, long size) {
        try (OutputStream os = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
            byte[] buff = new byte[4096];
            long remaining = size;
            while (remaining != 0) {
                int writeLen = (int) Math.min(remaining, buff.length);
                os.write(buff, 0, writeLen);
                remaining -= writeLen;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
