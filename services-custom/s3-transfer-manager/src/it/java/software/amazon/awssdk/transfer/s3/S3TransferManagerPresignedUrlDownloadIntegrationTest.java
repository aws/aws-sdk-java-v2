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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.PresignedDownloadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.utils.Md5Utils;

public class S3TransferManagerPresignedUrlDownloadIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3TransferManagerPresignedUrlDownloadIntegrationTest.class);
    private static final String SMALL_KEY = "small-key";
    private static final String LARGE_KEY = "large-key";
    private static final int SMALL_OBJ_SIZE = 5 * 1024 * 1024;
    private static final int LARGE_OBJ_SIZE = 16 * 1024 * 1024;
    
    private static File smallFile;
    private static File largeFile;
    private static S3Presigner presigner;

    @BeforeAll
    public static void setup() throws IOException {
        createBucket(BUCKET);
        smallFile = new RandomTempFile(SMALL_OBJ_SIZE);
        largeFile = new RandomTempFile(LARGE_OBJ_SIZE);
        s3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(SMALL_KEY).build(), smallFile.toPath());
        s3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(LARGE_KEY).build(), largeFile.toPath());
        presigner = S3Presigner.builder()
                              .region(DEFAULT_REGION)
                              .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                              .build();
    }

    @AfterAll
    public static void cleanup() {
        if (presigner != null) {
            presigner.close();
        }
        deleteBucketAndAllContents(BUCKET);
    }

    @ParameterizedTest
    @MethodSource("javaTransferManagerOnly")
    void downloadFileWithPresignedUrl_smallFile_downloadedCorrectly(S3TransferManager tm) throws Exception {
        PresignedGetObjectRequest presignedRequest = createPresignedRequest(SMALL_KEY);
        Path downloadPath = RandomTempFile.randomUncreatedFile().toPath();
        PresignedDownloadFileRequest request = PresignedDownloadFileRequest.builder()
            .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                .presignedUrl(presignedRequest.url())
                .build())
            .destination(downloadPath)
            .addTransferListener(LoggingTransferListener.create())
            .build();

        FileDownload download = tm.downloadFileWithPresignedUrl(request);
        CompletedFileDownload completed = download.completionFuture().join();

        assertThat(Files.exists(downloadPath)).isTrue();
        assertThat(Md5Utils.md5AsBase64(downloadPath.toFile())).isEqualTo(Md5Utils.md5AsBase64(smallFile));
        assertThat(completed.response().responseMetadata().requestId()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("javaTransferManagerOnly")
    void downloadFileWithPresignedUrl_largeFile_downloadedCorrectly(S3TransferManager tm) throws Exception {
        PresignedGetObjectRequest presignedRequest = createPresignedRequest(LARGE_KEY);
        Path downloadPath = RandomTempFile.randomUncreatedFile().toPath();
        PresignedDownloadFileRequest request = PresignedDownloadFileRequest.builder()
            .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                .presignedUrl(presignedRequest.url())
                .build())
            .destination(downloadPath)
            .addTransferListener(LoggingTransferListener.create())
            .build();

        FileDownload download = tm.downloadFileWithPresignedUrl(request);
        CompletedFileDownload completed = download.completionFuture().join();

        assertThat(Files.exists(downloadPath)).isTrue();
        assertThat(Md5Utils.md5AsBase64(downloadPath.toFile())).isEqualTo(Md5Utils.md5AsBase64(largeFile));
        assertThat(completed.response().responseMetadata().requestId()).isNotNull();
    }

    private static PresignedGetObjectRequest createPresignedRequest(String key) {
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build())
            .build());
    }
}