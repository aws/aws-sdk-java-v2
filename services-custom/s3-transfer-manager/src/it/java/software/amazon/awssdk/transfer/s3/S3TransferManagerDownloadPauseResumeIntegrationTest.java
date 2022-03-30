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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

public class S3TransferManagerDownloadPauseResumeIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3TransferManagerDownloadPauseResumeIntegrationTest.class);
    private static final String KEY = "key";
    private static final int OBJ_SIZE = 16 * 1024 * 1024;
    private static S3TransferManager tm;
    private static File file;

    @BeforeAll
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(BUCKET);
        file = new RandomTempFile(OBJ_SIZE);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), file.toPath());
        tm = S3TransferManager.builder()
                              .s3ClientConfiguration(b -> b.region(DEFAULT_REGION)
                                                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN))
                              .build();
    }

    @AfterAll
    public static void cleanup() {
        deleteBucketAndAllContents(BUCKET);
        tm.close();
        S3IntegrationTestBase.cleanUp();
    }

    @Test
    void downloadToFile_pause_shouldReturnResumableDownload() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Path path = RandomTempFile.randomUncreatedFile().toPath();
        TestDownloadListener testDownloadListener = new TestDownloadListener(countDownLatch);
        DownloadFileRequest request = DownloadFileRequest.builder()
                                                         .getObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                                         .destination(path)
                                                         .overrideConfiguration(b -> b
                                                             .addListener(testDownloadListener))
                                                         .build();
        FileDownload download =
            tm.downloadFile(request);
        boolean count = countDownLatch.await(10, TimeUnit.SECONDS);
        if (!count) {
            throw new AssertionError("No data has been transferred within 5 seconds");
        }
        ResumableFileDownload pause = download.pause();
        assertThat(pause.downloadFileRequest()).isEqualTo(request);
        assertThat(testDownloadListener.getObjectResponse).isNotNull();
        assertThat(pause.lastModified()).isEqualTo(testDownloadListener.getObjectResponse.lastModified());
        assertThat(pause.bytesTransferred()).isLessThanOrEqualTo(path.toFile().length());
        assertThat(pause.transferSizeInBytes()).hasValue(file.length());
        assertThat(download.completionFuture()).isCancelled();
    }

    private static final class TestDownloadListener implements TransferListener {
        private final CountDownLatch countDownLatch;
        private GetObjectResponse getObjectResponse;

        private TestDownloadListener(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void bytesTransferred(Context.BytesTransferred context) {
            Optional<SdkResponse> sdkResponse = context.progressSnapshot().sdkResponse();
            if (sdkResponse.isPresent() && sdkResponse.get() instanceof GetObjectResponse) {
                getObjectResponse = (GetObjectResponse) sdkResponse.get();
            }
            countDownLatch.countDown();
        }
    }

}
