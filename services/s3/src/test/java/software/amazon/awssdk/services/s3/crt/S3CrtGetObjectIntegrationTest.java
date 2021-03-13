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

package software.amazon.awssdk.services.s3.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.services.s3.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.Md5Utils;

public class S3CrtGetObjectIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3CrtGetObjectIntegrationTest.class);
    private static final String KEY = "key";
    private static S3CrtAsyncClient crtClient;
    private static File file;

    @BeforeClass
    public static void setup() throws IOException {
        createBucket(BUCKET);
        crtClient = S3CrtAsyncClient.builder()
                                    .region(DEFAULT_REGION)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .build();
        file = new RandomTempFile(10_000);
        s3.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .build(), file.toPath());
    }

    @AfterClass
    public static void cleanup() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void getObject_toFiles() throws IOException {
        Path path = RandomTempFile.randomUncreatedFile().toPath();

        GetObjectResponse response =
            crtClient.getObject(b -> b.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toFile(path)).join();

        assertThat(Md5Utils.md5AsBase64(path.toFile())).isEqualTo(Md5Utils.md5AsBase64(file));
    }

    @Test
    public void getObject_toBytes() throws IOException {
        byte[] bytes =
            crtClient.getObject(b -> b.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join().asByteArray();
        assertThat(bytes).isEqualTo(Files.readAllBytes(file.toPath()));
    }

    @Test
    public void getObject_customResponseTransformer() {
        crtClient.getObject(b -> b.bucket(BUCKET).key(KEY),
                            new TestResponseTransformer()).join();

    }

    private static final class TestResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, Void> {
        private CompletableFuture<Void> future;

        @Override
        public CompletableFuture<Void> prepare() {
            future = new CompletableFuture<>();
            return future;
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            assertThat(response).isNotNull();
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            publisher.subscribe(new SimpleSubscriber(b -> {
            }) {
                @Override
                public void onComplete() {
                    super.onComplete();
                    future.complete(null);
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            future.completeExceptionally(error);
        }
    }
}
