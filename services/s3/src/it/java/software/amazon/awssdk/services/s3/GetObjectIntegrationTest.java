/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.ResponseInputStream;
import software.amazon.awssdk.core.sync.StreamingResponseHandler;
import software.amazon.awssdk.services.s3.GetObjectAsyncIntegrationTest.AssertingExecutionInterceptor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;

public class GetObjectIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(GetObjectIntegrationTest.class);

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
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), file.toPath());
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
        file.delete();
    }

    @Test
    public void toInputStream() throws Exception {
        try (ResponseInputStream<GetObjectResponse> content = s3.getObject(getObjectRequest)) {
            assertMd5MatchesEtag(content, content.response());
        }
    }

    @Test
    public void toFile() throws Exception {
        Path path = RandomTempFile.randomUncreatedFile().toPath();
        try {
            GetObjectResponse response = s3.getObject(getObjectRequest, path);
            assertMd5MatchesEtag(new FileInputStream(path.toFile()), response);
        } finally {
            path.toFile().delete();
        }
    }

    @Test
    public void toOutputStream() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GetObjectResponse response = s3.getObject(getObjectRequest, StreamingResponseHandler.toOutputStream(baos));
        assertMd5MatchesEtag(new ByteArrayInputStream(baos.toByteArray()), response);
    }

    @Test
    public void customResponseHandler_InterceptorRecievesResponsePojo() throws Exception {
        try (S3Client clientWithInterceptor = createClientWithInterceptor(new AssertingExecutionInterceptor())) {
            String result = clientWithInterceptor.getObject(getObjectRequest, (resp, in) -> {
                assertThat(resp.metadata()).hasEntrySatisfying("x-amz-assert",
                                                               s -> assertThat(s).isEqualTo("injected-value"));
                return "result";
            });
            assertThat(result).isEqualTo("result");
        }
    }

    private S3Client createClientWithInterceptor(ExecutionInterceptor interceptor) {
        return s3ClientBuilder().overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                                  .addLastExecutionInterceptor(interceptor)
                                                                                  .build())
                                .build();
    }

}
