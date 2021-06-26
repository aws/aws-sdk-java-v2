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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.Md5Utils;

public class S3TransferManagerDownloadIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3TransferManagerDownloadIntegrationTest.class);
    private static final String KEY = "key";
    private static S3TransferManager transferManager;
    private static File file;

    @BeforeClass
    public static void setup() throws IOException {
        createBucket(BUCKET);
        file = new RandomTempFile(10_000);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), file.toPath());
        transferManager = S3TransferManager.builder()
                                           .s3ClientConfiguration(b -> b.region(DEFAULT_REGION)
                                                                        .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN))
                                           .build();
    }

    @AfterClass
    public static void cleanup() {
        deleteBucketAndAllContents(BUCKET);
        transferManager.close();
        S3IntegrationTestBase.cleanUp();
    }

    @Test
    public void download_shouldWork() throws IOException {
        Path path = RandomTempFile.randomUncreatedFile().toPath();
        Download download = transferManager.download(b -> b.getObjectRequest(r -> r.bucket(BUCKET).key(KEY))
                                                           .destination(path));
        CompletedDownload completedDownload = download.completionFuture().join();
        assertThat(Md5Utils.md5AsBase64(path.toFile())).isEqualTo(Md5Utils.md5AsBase64(file));
        assertThat(completedDownload.response().responseMetadata().requestId()).isNotNull();
    }
}
