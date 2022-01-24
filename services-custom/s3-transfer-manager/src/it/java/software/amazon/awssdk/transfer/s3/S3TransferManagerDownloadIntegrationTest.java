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
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.utils.Md5Utils;

public class S3TransferManagerDownloadIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3TransferManagerDownloadIntegrationTest.class);
    private static final String KEY = "key";
    private static final int OBJ_SIZE = 16 * 1024 * 1024;
    private static S3TransferManager tm;
    private static File file;

    @BeforeClass
    public static void setup() throws IOException {
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

    @AfterClass
    public static void cleanup() {
        deleteBucketAndAllContents(BUCKET);
        tm.close();
        S3IntegrationTestBase.cleanUp();
    }

    @Test
    public void download_toFile() throws IOException {
        Path path = RandomTempFile.randomUncreatedFile().toPath();
        FileDownload download =
            tm.downloadFile(DownloadFileRequest.builder()
                                               .getObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                               .destination(path)
                                               .overrideConfiguration(b -> b.addListener(LoggingTransferListener.create()))
                                               .build());
        CompletedFileDownload completedFileDownload = download.completionFuture().join();
        assertThat(Md5Utils.md5AsBase64(path.toFile())).isEqualTo(Md5Utils.md5AsBase64(file));
        assertThat(completedFileDownload.response().responseMetadata().requestId()).isNotNull();
    }

    @Test
    public void download_toBytes() throws Exception {
        Download<ResponseBytes<GetObjectResponse>> download =
            tm.download(DownloadRequest.builder()
                                       .getObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                       .responseTransformer(AsyncResponseTransformer.toBytes())
                                       .overrideConfiguration(b -> b.addListener(LoggingTransferListener.create()))
                                       .build());
        CompletedDownload<ResponseBytes<GetObjectResponse>> completedDownload = download.completionFuture().join();
        ResponseBytes<GetObjectResponse> result = completedDownload.result();
        assertThat(Md5Utils.md5AsBase64(result.asByteArray())).isEqualTo(Md5Utils.md5AsBase64(file));
        assertThat(result.response().responseMetadata().requestId()).isNotNull();
    }

    @Test
    public void download_toPublisher() throws Exception {
        Download<ResponsePublisher<GetObjectResponse>> download =
            tm.download(DownloadRequest.builder()
                                       .getObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                       .responseTransformer(AsyncResponseTransformer.toPublisher())
                                       .overrideConfiguration(b -> b.addListener(LoggingTransferListener.create()))
                                       .build());
        CompletedDownload<ResponsePublisher<GetObjectResponse>> completedDownload = download.completionFuture().join();
        ResponsePublisher<GetObjectResponse> responsePublisher = completedDownload.result();
        assertThat(responsePublisher.response().responseMetadata().requestId()).isNotNull();
        ByteBuffer buf = ByteBuffer.allocate(Math.toIntExact(responsePublisher.response().contentLength()));
        CompletableFuture<Void> drainPublisherFuture = responsePublisher.subscribe(buf::put);
        drainPublisherFuture.join();
        assertThat(Md5Utils.md5AsBase64(buf.array())).isEqualTo(Md5Utils.md5AsBase64(file));
    }
}
