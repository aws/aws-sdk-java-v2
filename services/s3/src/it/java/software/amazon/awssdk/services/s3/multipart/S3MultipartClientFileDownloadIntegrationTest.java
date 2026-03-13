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

package software.amazon.awssdk.services.s3.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.JavaSystemSetting;
import software.amazon.awssdk.utils.Logger;

@Timeout(value = 5, unit = TimeUnit.MINUTES)
public class S3MultipartClientFileDownloadIntegrationTest extends S3IntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3MultipartClientFileDownloadIntegrationTest.class);
    private static final int MIB = 1024 * 1024;
    private static final String TEST_BUCKET = temporaryBucketName(S3MultipartClientFileDownloadIntegrationTest.class);
    private static final String TEST_KEY = "testfile.dat";
    private static final String ZERO_BYTE_KEY = "zero.dat";
    private static final int OBJ_SIZE = 100 * MIB;
    private static final long PART_SIZE = 5 * MIB;

    private static RandomTempFile localFile;
    private S3AsyncClient s3Client;
    private TestInterceptor interceptor;

    @BeforeAll
    public static void setup() throws Exception {
        log.info(() -> "setup");
        setUp();
        log.info(() -> "create bucket");
        createBucket(TEST_BUCKET);
        localFile = new RandomTempFile(TEST_KEY, OBJ_SIZE);
        localFile.deleteOnExit();
        S3AsyncClient s3Client = S3AsyncClient.builder()
                                              .multipartEnabled(true)
                                              .multipartConfiguration(c -> c.minimumPartSizeInBytes(PART_SIZE))
                                              .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                              .region(DEFAULT_REGION)
                                              .build();
        log.info(() -> "put multipart object");
        s3Client.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), AsyncRequestBody.fromFile(localFile))
                .join();
        s3.close();
    }

    @BeforeEach
    public void init() {
        log.info(() -> "Initializing S3MultipartClientFileDownloadIntegrationTest");
        this.interceptor = new TestInterceptor();
        this.s3Client = S3AsyncClient.builder()
                                     .multipartEnabled(true)
                                     .overrideConfiguration(o -> o.addExecutionInterceptor(this.interceptor))
                                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                     .region(DEFAULT_REGION)
                                     .build();
    }

    @Test
    void download_defaultCreateNewFile_shouldSucceed() throws Exception {
        Path path = tmpPath().resolve(UUID.randomUUID().toString());
        CompletableFuture<GetObjectResponse> future = s3Client.getObject(
            req -> req.bucket(TEST_BUCKET).key(TEST_KEY),
            AsyncResponseTransformer.toFile(path, FileTransformerConfiguration.defaultCreateNew()));
        future.join();
        assertSameContentWithChecksum(path);
        int totalParts = OBJ_SIZE / (int) PART_SIZE;
        assertThat(interceptor.parts.size()).isEqualTo(totalParts);
        assertThat(interceptor.parts).hasSameElementsAs(IntStream.range(1, totalParts +1).boxed().collect(Collectors.toList()));
        path.toFile().delete();
    }

    @Test
    void download_emptyFile_shouldSucceed() throws Exception {
        Path path = tmpPath().resolve(UUID.randomUUID().toString());
        s3Client.putObject(b -> b.bucket(TEST_BUCKET).key(ZERO_BYTE_KEY), AsyncRequestBody.empty()).join();
        CompletableFuture<GetObjectResponse> future = s3Client.getObject(
            req -> req.bucket(TEST_BUCKET).key(ZERO_BYTE_KEY),
            AsyncResponseTransformer.toFile(path, FileTransformerConfiguration.defaultCreateNew()));
        future.join();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] downloadedHash = md.digest(Files.readAllBytes(path));
        md.reset();
        byte[] originalHash = md.digest(new byte[0]);
        assertThat(downloadedHash).isEqualTo(originalHash);
    }


    private Path tmpPath() {
        return Paths.get(JavaSystemSetting.TEMP_DIRECTORY.getStringValueOrThrow());
    }

    private static final class TestInterceptor implements ExecutionInterceptor {
        List<Integer> parts = new ArrayList<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SdkRequest request = context.request();
            if (request instanceof GetObjectRequest) {
                log.info(() -> "Received GetObjectRequest for request " + request);
                GetObjectRequest getObjectRequest = (GetObjectRequest) request;
                parts.add(getObjectRequest.partNumber());
            } else {
                log.warn(() -> "Unexpected request type: " + request.getClass());
            }
        }
    }

    private void assertSameContentWithChecksum(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] downloadedHash = md.digest(Files.readAllBytes(path));
        md.reset();
        byte[] originalHash = md.digest(Files.readAllBytes(localFile.toPath()));
        assertThat(downloadedHash).isEqualTo(originalHash);
    }
}