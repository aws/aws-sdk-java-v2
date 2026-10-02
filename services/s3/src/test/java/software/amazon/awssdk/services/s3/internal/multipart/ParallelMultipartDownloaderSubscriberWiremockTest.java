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

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.testng.collections.Sets;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartDownloadTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@WireMockTest
class ParallelMultipartDownloaderSubscriberWiremockTest {

    private final String testBucket = "test-bucket";
    private final String testKey = "test-key";

    private S3AsyncClient s3AsyncClient;
    private MultipartDownloadTestUtils utils;
    private FileSystem fileSystem;
    private Path testFile;

    @BeforeEach
    void init(WireMockRuntimeInfo wiremock) throws Exception {
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
        Files.createDirectories(testFile.getParent());
        Files.createFile(testFile);
    }

    @AfterEach
    void tearDown() throws Exception {
        fileSystem.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10, 49, // less than maxInFlightParts
                         50, // == maxInFlightParts
                         51, 100, 101 // more than  maxInFlightParts
    })
    void happyPath_multipartDownload(int numParts) throws Exception {
        int partSize = 1024;
        byte[] expectedBody = utils.stubAllParts(testBucket, testKey, numParts, partSize);

        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer =
            AsyncResponseTransformer.toFile(testFile, FileTransformerConfiguration.defaultCreateOrReplaceExisting());
        AsyncResponseTransformer.SplitResult<GetObjectResponse, GetObjectResponse> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());

        CompletableFuture<GetObjectResponse> resultFuture = new CompletableFuture<>();
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber =
            new ParallelMultipartDownloaderSubscriber(s3AsyncClient,
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

        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer =
            AsyncResponseTransformer.toFile(testFile, FileTransformerConfiguration.defaultCreateOrReplaceExisting());
        AsyncResponseTransformer.SplitResult<GetObjectResponse, GetObjectResponse> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());

        CompletableFuture<GetObjectResponse> resultFuture = new CompletableFuture<>();
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber =
            new ParallelMultipartDownloaderSubscriber(s3AsyncClient,
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

    @Test
    void whenPartsAlreadyCompleted_shouldDownloadOnlyMissingParts() throws Exception {
        int partSize = 1024;
        int numParts = 90;

        // make sure the file contains all zero
        Files.write(testFile, new byte[partSize * numParts]);

        Set<Integer> completedParts = Sets.newHashSet(5, 12, 23, 34, 45, 56, 67, 78, 81, 89);
        byte[] expectedBody = stubAllPartsWithCompleted(testBucket, testKey, numParts, partSize, completedParts);

        FileTransformerConfiguration fileTransformerConfiguration = FileTransformerConfiguration
            .builder()
            .fileWriteOption(FileTransformerConfiguration.FileWriteOption.WRITE_TO_POSITION)
            .failureBehavior(FileTransformerConfiguration.FailureBehavior.DELETE)
            .position(0L)
            .build();

        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer =
            AsyncResponseTransformer.toFile(testFile, fileTransformerConfiguration);

        AsyncResponseTransformer.SplitResult<GetObjectResponse, GetObjectResponse> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());

        MultipartDownloadResumeContext context = new MultipartDownloadResumeContext(completedParts, 0L);
        CompletableFuture<GetObjectResponse> resultFuture = new CompletableFuture<>();
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber =
            new ParallelMultipartDownloaderSubscriber(
                s3AsyncClient,
                GetObjectRequest.builder()
                                .overrideConfiguration(c -> c.putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT, context))
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

        verifyCorrectRequestsMade(numParts, completedParts);
    }

    private void verifyCorrectRequestsMade(int numParts, Set<Integer> completedParts) {
        String urlTemplate = ".*partNumber=%d\\b.*";
        for (int i = 1; i <= numParts; i++) {
            if (completedParts.contains(i)) {
                verify(0, getRequestedFor(urlMatching(String.format(urlTemplate, i))));
            } else {
                verify(getRequestedFor(urlMatching(String.format(urlTemplate, i))));
            }
        }
        verify(0, getRequestedFor(urlMatching(String.format(urlTemplate, numParts + 1))));

    }

    private byte[] stubAllPartsWithCompleted(String testBucket, String testKey, int amountOfPartToTest, int partSize,
                                             Set<Integer> completedParts) {
        byte[] expectedBody = new byte[amountOfPartToTest * partSize];
        for (int i = 0; i < amountOfPartToTest; i++) {
            if (completedParts.contains(i + 1)) {
                // fill with zero
                byte[] individualBody = new byte[partSize];
                System.arraycopy(individualBody, 0, expectedBody, i * partSize, individualBody.length);
            } else {
                byte[] individualBody = utils.stubForPart(testBucket, testKey, i + 1, amountOfPartToTest, partSize);
                System.arraycopy(individualBody, 0, expectedBody, i * partSize, individualBody.length);
            }
        }
        return expectedBody;

    }


}