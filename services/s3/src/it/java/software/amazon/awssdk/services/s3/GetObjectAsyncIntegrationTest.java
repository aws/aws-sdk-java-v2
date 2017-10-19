/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;
import static utils.S3TestUtils.assertMd5MatchesEtag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseHandler;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.util.ImmutableMapParameter;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;

public class GetObjectAsyncIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(GetObjectAsyncIntegrationTest.class);

    private static final String KEY = "some-key";

    private final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                                      .bucket(BUCKET)
                                                                      .key(KEY)
                                                                      .build();

    private static File file;

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);
        file = new RandomTempFile(10_000);
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .build(), file.toPath()).join();
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
        file.delete();
    }

    @Test
    public void toFile() throws Exception {
        Path path = RandomTempFile.randomUncreatedFile().toPath();
        try {
            GetObjectResponse response = s3Async.getObject(getObjectRequest, path).join();
            assertMd5MatchesEtag(new FileInputStream(path.toFile()), response);
        } finally {
            path.toFile().delete();
        }
    }

    @Test
    public void dumpToString() throws IOException {
        String returned = s3Async.getObject(getObjectRequest, AsyncResponseHandler.toUtf8String()).join();
        assertThat(returned).isEqualTo(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void toByteArray() throws IOException {
        byte[] returned = s3Async.getObject(getObjectRequest, AsyncResponseHandler.toByteArray()).join();
        assertThat(returned).isEqualTo(Files.readAllBytes(file.toPath()));
    }

    @Test
    public void customResponseHandler_InterceptorRecievesResponsePojo() throws Exception {
        try (S3AsyncClient asyncWithInterceptor = createClientWithInterceptor(new AssertingExecutionInterceptor())) {
            String result = asyncWithInterceptor
                    .getObject(getObjectRequest, new AsyncResponseHandler<GetObjectResponse, String>() {

                        @Override
                        public void responseReceived(GetObjectResponse response) {
                            // POJO returned by modifyResponse should be delivered to the AsyncResponseHandler
                            assertThat(response.metadata()).hasEntrySatisfying("x-amz-assert",
                                                                               s -> assertThat(s).isEqualTo("injected-value"));
                        }

                        @Override
                        public void onStream(Publisher<ByteBuffer> publisher) {
                            publisher.subscribe(new SimpleSubscriber(b -> {
                            }));
                        }

                        @Override
                        public void exceptionOccurred(Throwable throwable) {
                        }

                        @Override
                        public String complete() {
                            return "result";
                        }
                    }).join();
            assertThat(result).isEqualTo("result");
        }
    }

    private S3AsyncClient createClientWithInterceptor(ExecutionInterceptor assertingInterceptor) {
        return s3AsyncClientBuilder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                  .addLastExecutionInterceptor(assertingInterceptor)
                                                                  .build())
                .build();
    }

    /**
     * Asserts that the {@link Context.AfterUnmarshalling#response()} object is an instance of {@link GetObjectResponse}. Also
     * modifies the {@link GetObjectResponse} in {@link ExecutionInterceptor#modifyResponse(Context.ModifyResponse,
     * ExecutionAttributes)} so we can verify the modified POJO is delivered to the streaming response handlers for sync and
     * async.
     */
    public static class AssertingExecutionInterceptor implements ExecutionInterceptor {
        @Override
        public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
            // The response object should be the pojo. Not the result type of the AsyncResponseHandler
            assertThat(context.response()).isInstanceOf(GetObjectResponse.class);
        }

        @Override
        public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
            return ((GetObjectResponse) context.response())
                    .toBuilder()
                    .metadata(ImmutableMapParameter.of("x-amz-assert", "injected-value"))
                    .build();
        }
    }
}
