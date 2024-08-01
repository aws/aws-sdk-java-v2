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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Tests to make sure all CRT resources are cleaned up for get object.
 */
@WireMockTest
@Timeout(10)
public class S3CrtClientGetObjectResourceManagementTest {

    private static final String BUCKET = "Example-Bucket";
    private static final String KEY = "Example-Object";
    private static final long PART_SIZE = 1024 * 1024 * 5L;
    private S3AsyncClient s3AsyncClient;

    @BeforeAll
    public static void setUpBeforeAll() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @BeforeEach
    public void setup(WireMockRuntimeInfo wiremock) {
        stubGetObjectCalls();
        s3AsyncClient = S3AsyncClient.crtBuilder()
                                     .region(Region.US_EAST_1)
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .credentialsProvider(
                                         StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                     .minimumPartSizeInBytes(PART_SIZE)
                                     .maxConcurrency(2)
                                     .initialReadBufferSizeInBytes(1024L)
                                     .build();
    }

    @AfterEach
    public void tearDown() {
        s3AsyncClient.close();
    }

    @Test
    void toBlockingInputStream_abortStream_shouldCloseResources() throws IOException {
        ResponseInputStream<GetObjectResponse> response = s3AsyncClient.getObject(
            r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBlockingInputStream()).join();
        response.read();
        response.abort();
    }

    @Test
    void toBlockingInputStream_closeStream_shouldCloseResources() throws IOException {
        ResponseInputStream<GetObjectResponse> response = s3AsyncClient.getObject(
            r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBlockingInputStream()).join();
        response.read();
        response.close();
    }

    @Test
    void toFile_cancelRequest_shouldCloseResource() throws IOException {
        CompletableFuture<GetObjectResponse> future = s3AsyncClient.getObject(
            r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toFile(Files.newTemporaryFile()));
        future.cancel(false);
    }

    @Test
    void toFile_happyCase_shouldCloseResource() throws IOException {
        File file = RandomTempFile.randomUncreatedFile();
        CompletableFuture<GetObjectResponse> future = s3AsyncClient.getObject(
            r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toFile(file));
        future.join();
    }

    @Test
    void toBlockingInputStream_happyCase_shouldCloseResource() throws IOException {
        try (ResponseInputStream<GetObjectResponse> response = s3AsyncClient.getObject(
            r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBlockingInputStream()).join()) {
            IoUtils.drainInputStream(response);
        }
    }

    private static void stubGetObjectCalls() {
        int numOfParts = 3;
        long finalPartSize = 1024 * 1024 * 4;
        long totalContentSize = PART_SIZE * (numOfParts - 1) + finalPartSize;

        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200)
                                                     .withHeader("content-length", String.valueOf(totalContentSize))
                                                     .withHeader("etag", "1234")));

        for (int i = 0; i < numOfParts - 1; i++) {
            int partNumberIndex = i + 1;
            String contentRange = "bytes " + PART_SIZE * i + "-" + (PART_SIZE * partNumberIndex  - 1) + "/" + totalContentSize;
            String range = "bytes=" + PART_SIZE * i + "-" + (PART_SIZE * partNumberIndex - 1);
            stubFor(get(anyUrl()).withHeader("Range", equalTo(range)).willReturn(aResponse().withStatus(200)
                                                                                                 .withHeader("content-length",
                                                                                                             String.valueOf(PART_SIZE))
                                                                                                 .withHeader("Content-Range",
                                                                                                             contentRange)
                                                                                                 .withHeader("etag", "1234")
                                                                                                 .withBodyFile("part" + partNumberIndex)));
        }

        // final part
        String contentRange = "bytes " + PART_SIZE * numOfParts + "-" + (totalContentSize - 1) + "/" + totalContentSize;
        String range = "bytes=" + PART_SIZE * (numOfParts - 1) + "-" + (totalContentSize - 1);
        stubFor(get(anyUrl()).withHeader("Range", equalTo(range)).willReturn(aResponse().withStatus(200)
                                                                                        .withHeader("content-length", String.valueOf(finalPartSize))
                                                                                        .withHeader("Content-Range",
                                                                                                    contentRange)
                                                                                        .withHeader("etag", "1234")
                                                                                        .withBodyFile("part" + (numOfParts - 1))));
    }
}
