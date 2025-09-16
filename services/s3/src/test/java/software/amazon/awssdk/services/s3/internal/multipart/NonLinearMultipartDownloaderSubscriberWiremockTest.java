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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartDownloadTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@WireMockTest
class NonLinearMultipartDownloaderSubscriberWiremockTest {

    private final String testBucket = "test-bucket";
    private final String testKey = "test-key";

    private S3AsyncClient s3AsyncClient;
    private MultipartDownloadTestUtils utils;
    private FileSystem fileSystem;
    private Path testFile;

    @BeforeEach
    public void init(WireMockRuntimeInfo wiremock) throws Exception {
        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(StaticCredentialsProvider.create(
                                         AwsBasicCredentials.create("key", "secret")))
                                     .region(Region.US_WEST_2)
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .serviceConfiguration(S3Configuration.builder()
                                                                          .pathStyleAccessEnabled(true)
                                                                          .build())
                                     .build();
        utils = new MultipartDownloadTestUtils(testBucket, testKey, "test-etag");
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        testFile = fileSystem.getPath("/test-file.txt");
    }

    @AfterEach
    void tearDown() throws Exception {
        fileSystem.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10})
    void happyPath_multipartDownload(int numParts) throws Exception {
        int partSize = 1024;
        byte[] expectedBody = utils.stubAllParts(testBucket, testKey, numParts, partSize);

        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer = AsyncResponseTransformer.toFile(testFile);
        AsyncResponseTransformer.SplitResult<GetObjectResponse, GetObjectResponse> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());

        CompletableFuture<GetObjectResponse> resultFuture = new CompletableFuture<>();
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber =
            new NonLinearMultipartDownloaderSubscriber(s3AsyncClient,
                                                       GetObjectRequest.builder()
                                                                       .bucket(testBucket)
                                                                       .key(testKey)
                                                                       .build(),
                                                       resultFuture,
                                                       50);

        split.publisher().subscribe(subscriber);
        GetObjectResponse getObjectResponse = resultFuture.join();

        assertThat(Files.exists(testFile)).isTrue();
        byte[] actualBody = Files.readAllBytes(testFile);
        assertThat(actualBody).isEqualTo(expectedBody);
        assertThat(getObjectResponse).isNotNull();
        utils.verifyCorrectAmountOfRequestsMade(numParts);
    }

    @Test
    void singlePartObject_shouldCompleteWithoutMultipart() throws Exception {
        int partSize = 1024;
        byte[] expectedBody = utils.stubSinglePart(testBucket, testKey, partSize);

        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer = AsyncResponseTransformer.toFile(testFile);
        AsyncResponseTransformer.SplitResult<GetObjectResponse, GetObjectResponse> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());

        CompletableFuture<GetObjectResponse> resultFuture = new CompletableFuture<>();
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber =
            new NonLinearMultipartDownloaderSubscriber(s3AsyncClient,
                                                       GetObjectRequest.builder()
                                                                       .bucket(testBucket)
                                                                       .key(testKey)
                                                                       .build(),
                                                       resultFuture,
                                                       50);

        split.publisher().subscribe(subscriber);
        GetObjectResponse getObjectResponse = resultFuture.join();

        assertThat(Files.exists(testFile)).isTrue();
        byte[] actualBody = Files.readAllBytes(testFile);
        assertThat(actualBody).isEqualTo(expectedBody);
        assertThat(getObjectResponse).isNotNull();
        utils.verifyCorrectAmountOfRequestsMade(1);
    }

}