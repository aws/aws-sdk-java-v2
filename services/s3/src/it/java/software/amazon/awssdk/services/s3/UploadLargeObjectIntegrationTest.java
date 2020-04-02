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

import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.IoUtils;

public class UploadLargeObjectIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(UploadLargeObjectIntegrationTest.class);

    private static final String ASYNC_KEY = "async-key";
    private static final String SYNC_KEY = "sync-key";

    private static File file;

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);
        file = new RandomTempFile(500 * 1024 * 1024);
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
        file.delete();
    }

    @Test
    public void syncPutLargeObject() {
        s3.putObject(b -> b.bucket(BUCKET).key(SYNC_KEY), file.toPath());
        verifyResponse(SYNC_KEY);
    }

    @Test
    public void asyncPutLargeObject() {
        s3Async.putObject(b -> b.bucket(BUCKET).key(ASYNC_KEY), file.toPath()).join();
        verifyResponse(ASYNC_KEY);
    }

    private void verifyResponse(String key) {
        ResponseInputStream<GetObjectResponse> responseInputStream = s3.getObject(b -> b.bucket(BUCKET).key(key));
        IoUtils.drainInputStream(responseInputStream);
    }
}
