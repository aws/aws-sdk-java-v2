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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class HeadObjectIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(HeadObjectIntegrationTest.class);

    private static final String GZIPPED_KEY = "some-key";

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(baos);
        gzos.write("Test".getBytes(StandardCharsets.UTF_8));

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(GZIPPED_KEY)
                                     .contentEncoding("gzip")
                                     .build(),
                     RequestBody.fromBytes(baos.toByteArray()));
    }

    @Test
    public void asyncClientSupportsGzippedObjects() {
        HeadObjectResponse response = s3Async.headObject(r -> r.bucket(BUCKET).key(GZIPPED_KEY)).join();
        assertThat(response.contentEncoding()).isEqualTo("gzip");
    }

    @Test
    public void syncClientSupportsGzippedObjects() {
        HeadObjectResponse response = s3.headObject(r -> r.bucket(BUCKET).key(GZIPPED_KEY));
        assertThat(response.contentEncoding()).isEqualTo("gzip");
    }
}
