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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.sync.RequestBody;
import software.amazon.awssdk.sync.ResponseInputStream;
import software.amazon.awssdk.sync.StreamingResponseHandler;
import software.amazon.awssdk.test.util.RandomTempFile;

public class GetObjectIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = getBucketName(GetObjectIntegrationTest.class);

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
                                     .build(), RequestBody.of(file));
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
        file.delete();
    }

    @Test
    public void toInputStream() throws Exception {
        try (ResponseInputStream<GetObjectResponse> content =
                     s3.getObject(getObjectRequest, StreamingResponseHandler.toInputStream())) {
            assertMd5MatchesEtag(content, content.response());
        }
    }

    @Test
    public void toFile() throws Exception {
        Path path = RandomTempFile.randomUncreatedFile().toPath();
        try {
            GetObjectResponse response = s3.getObject(getObjectRequest, StreamingResponseHandler.toFile(path));
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

}
