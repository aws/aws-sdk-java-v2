/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.Md5Utils;

public class S3CrtDirectBufferPoolGetObjectIntegrationTest extends S3IntegrationTestBase {
    private static final int MIB = 1024 * 1024;
    private static final int PART_SIZE = 8 * MIB;
    private static final int OBJECT_SIZE = 18 * MIB;
    private static final String BUCKET = temporaryBucketName(S3CrtDirectBufferPoolGetObjectIntegrationTest.class);
    private static final String OBJECT_KEY = "object";
    private static final String EMPTY_OBJECT_KEY = "empty-object";
    private static final String UPLOADED_OBJECT_KEY = "uploaded-object";
    private static File object;

    @BeforeAll
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(BUCKET);
        object = new RandomTempFile(OBJECT_SIZE);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(OBJECT_KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .build(), object.toPath());
        s3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(EMPTY_OBJECT_KEY).build(), RequestBody.empty());
    }

    @AfterAll
    public static void cleanup() {
        try {
            deleteBucketAndAllContents(BUCKET);
        } finally {
            try {
                S3IntegrationTestBase.cleanUpResources();
            } finally {
                CrtResource.waitForNoResources();
            }
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("poolConfigurations")
    void getObject_multipartObject_shouldMatchContent(S3CrtDirectBufferPoolConfiguration poolConfiguration) throws Exception {
        try (S3AsyncClient client = pooledClient(poolConfiguration);
             ResponseInputStream<GetObjectResponse> stream = client.getObject(
                 b -> b.bucket(BUCKET).key(OBJECT_KEY).checksumMode(ChecksumMode.ENABLED),
                 S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool())
                                                        .get(1, TimeUnit.MINUTES)) {
            assertThat(stream.response().contentLength()).isEqualTo((long) OBJECT_SIZE);
            assertStreamMatchesObject(stream);
        }
    }

    @Test
    void getObject_emptyObject_shouldReturnEmptyStream() throws Exception {
        try (S3AsyncClient client = pooledClient(fixedPool());
             ResponseInputStream<GetObjectResponse> stream = client.getObject(
                 b -> b.bucket(BUCKET).key(EMPTY_OBJECT_KEY),
                 S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool())
                                                        .get(1, TimeUnit.MINUTES)) {
            assertThat(stream.response().contentLength()).isZero();
            assertThat(stream.read()).isEqualTo(-1);
            assertThat(stream.read()).isEqualTo(-1);
        }
    }

    @Test
    void getObject_closeEarly_shouldReleasePool() throws Exception {
        try (S3AsyncClient client = pooledClient(fixedPool())) {
            // If every close leaked its lease, the two-part pool would be empty before the third request.
            for (int i = 0; i < 3; i++) {
                try (ResponseInputStream<GetObjectResponse> stream = client.getObject(
                    b -> b.bucket(BUCKET).key(OBJECT_KEY),
                    S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool())
                                                                   .get(1, TimeUnit.MINUTES)) {
                    assertThat(stream.read()).isGreaterThanOrEqualTo(0);
                }
            }
        }
    }

    @Test
    void sharedClient_ordinaryRequests_shouldSucceed() throws Exception {
        try (S3AsyncClient client = pooledClient(fixedPool())) {
            ResponseBytes<GetObjectResponse> responseBytes = client.getObject(
                b -> b.bucket(BUCKET).key(OBJECT_KEY), AsyncResponseTransformer.toBytes()).get(1, TimeUnit.MINUTES);
            assertThat(Md5Utils.md5AsBase64(responseBytes.asByteArray())).isEqualTo(Md5Utils.md5AsBase64(object));

            client.putObject(PutObjectRequest.builder().bucket(BUCKET).key(UPLOADED_OBJECT_KEY).build(),
                             AsyncRequestBody.fromFile(object.toPath()))
                  .get(1, TimeUnit.MINUTES);

            assertThat(client.headObject(b -> b.bucket(BUCKET).key(UPLOADED_OBJECT_KEY))
                             .get(1, TimeUnit.MINUTES)
                             .contentLength()).isEqualTo((long) OBJECT_SIZE);
        }
    }

    private static Stream<S3CrtDirectBufferPoolConfiguration> poolConfigurations() {
        return Stream.of(S3CrtDirectBufferPoolConfiguration.auto(),
                         fixedPool(),
                         S3CrtDirectBufferPoolConfiguration.elastic(0, 2L * PART_SIZE));
    }

    private static S3CrtDirectBufferPoolConfiguration fixedPool() {
        return S3CrtDirectBufferPoolConfiguration.fixed(2L * PART_SIZE);
    }

    private static S3AsyncClient pooledClient(S3CrtDirectBufferPoolConfiguration poolConfiguration) {
        return S3AsyncClient.crtBuilder()
                            .region(DEFAULT_REGION)
                            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                            .minimumPartSizeInBytes((long) PART_SIZE)
                            .initialReadBufferSizeInBytes((long) PART_SIZE)
                            .directBufferPoolConfiguration(poolConfiguration)
                            .build();
    }

    private static void assertStreamMatchesObject(ResponseInputStream<GetObjectResponse> stream) throws IOException {
        Path downloadedObject = RandomTempFile.randomUncreatedFile().toPath();
        try {
            Files.copy(stream, downloadedObject);
            assertThat(Md5Utils.md5AsBase64(downloadedObject.toFile())).isEqualTo(Md5Utils.md5AsBase64(object));
            assertThat(stream.read()).isEqualTo(-1);
            assertThat(stream.read()).isEqualTo(-1);
        } finally {
            Files.deleteIfExists(downloadedObject);
        }
    }
}
