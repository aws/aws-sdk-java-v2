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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.util.ChecksumUtils;

public class S3TransferManagerUploadIntegrationTest extends S3IntegrationTestBase {
    private static final String TEST_BUCKET = temporaryBucketName(S3TransferManagerUploadIntegrationTest.class);
    private static final String TEST_KEY = "16mib_file.dat";
    private static final int OBJ_SIZE = 16 * 1024 * 1024;

    private static RandomTempFile testFile;

    @BeforeAll
    public static void setUp() throws Exception {
        createBucket(TEST_BUCKET);

        testFile = new RandomTempFile(TEST_KEY, OBJ_SIZE);
    }

    @AfterAll
    public static void teardown() throws IOException {
        Files.delete(testFile.toPath());
        deleteBucketAndAllContents(TEST_BUCKET);
    }

   @Test
    void upload_file_SentCorrectly() throws IOException {
        Map<String, String> metadata = new HashMap<>();
       CaptureTransferListener transferListener = new CaptureTransferListener();
       metadata.put("x-amz-meta-foobar", "FOO BAR");
        FileUpload fileUpload =
            tm.uploadFile(u -> u.putObjectRequest(p -> p.bucket(TEST_BUCKET).key(TEST_KEY).metadata(metadata).checksumAlgorithm(ChecksumAlgorithm.CRC32))
                                .source(testFile.toPath())
                                .addTransferListener(LoggingTransferListener.create())
                .addTransferListener(transferListener)
                                .build());

        CompletedFileUpload completedFileUpload = fileUpload.completionFuture().join();
        assertThat(completedFileUpload.response().responseMetadata().requestId()).isNotNull();
        assertThat(completedFileUpload.response().sdkHttpResponse()).isNotNull();

        ResponseInputStream<GetObjectResponse> obj = s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                ResponseTransformer.toInputStream());

        assertThat(ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath())))
                .isEqualTo(ChecksumUtils.computeCheckSum(obj));
        assertThat(obj.response().responseMetadata().requestId()).isNotNull();
        assertThat(obj.response().metadata()).containsEntry("foobar", "FOO BAR");
        assertThat(fileUpload.progress().snapshot().sdkResponse()).isPresent();
       assertListenerForSuccessfulTransferComplete(transferListener);
   }

    private static void assertListenerForSuccessfulTransferComplete(CaptureTransferListener transferListener) {
        assertThat(transferListener.isTransferInitiated()).isTrue();
        assertThat(transferListener.isTransferInitiated()).isTrue();
        assertThat(transferListener.getRatioTransferredList()).isNotEmpty();
        assertThat(transferListener.getRatioTransferredList().contains(0.0));
        assertThat(transferListener.getRatioTransferredList().contains(100.0));
        assertThat(transferListener.getExceptionCaught()).isNull();
    }

    @Test
    void upload_asyncRequestBody_SentCorrectly() throws IOException {
        String content = UUID.randomUUID().toString();
        CaptureTransferListener transferListener = new CaptureTransferListener();

        Upload upload =
            tm.upload(UploadRequest.builder()
                                   .putObjectRequest(b -> b.bucket(TEST_BUCKET).key(TEST_KEY))
                                   .requestBody(AsyncRequestBody.fromString(content))
                                   .addTransferListener(LoggingTransferListener.create())
                                   .addTransferListener(transferListener)
                                   .build());

        CompletedUpload completedUpload = upload.completionFuture().join();
        assertThat(completedUpload.response().responseMetadata().requestId()).isNotNull();
        assertThat(completedUpload.response().sdkHttpResponse()).isNotNull();

        ResponseInputStream<GetObjectResponse> obj = s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                  ResponseTransformer.toInputStream());

        assertThat(ChecksumUtils.computeCheckSum(content.getBytes(StandardCharsets.UTF_8)))
            .isEqualTo(ChecksumUtils.computeCheckSum(obj));
        assertThat(obj.response().responseMetadata().requestId()).isNotNull();
        assertThat(upload.progress().snapshot().sdkResponse()).isPresent();
        assertListenerForSuccessfulTransferComplete(transferListener);

    }

    @Test
    void upload_file_Interupted_CancelsTheListener() throws IOException, InterruptedException {
        Map<String, String> metadata = new HashMap<>();
        CaptureTransferListener transferListener = new CaptureTransferListener();
        metadata.put("x-amz-meta-foobar", "FOO BAR");
        FileUpload fileUpload =
            tm.uploadFile(u -> u.putObjectRequest(p -> p.bucket(TEST_BUCKET).key(TEST_KEY).metadata(metadata).checksumAlgorithm(ChecksumAlgorithm.CRC32))
                                .source(testFile.toPath())
                                .addTransferListener(LoggingTransferListener.create())
                                .addTransferListener(transferListener)
                                .build());

        fileUpload.completionFuture().cancel(true);
        assertThat(transferListener.isTransferInitiated()).isTrue();
        assertThat(transferListener.isTransferComplete()).isFalse();
        assertThat(transferListener.getExceptionCaught()).isInstanceOf(CancellationException.class);
        assertThat(transferListener.getRatioTransferredList().get(transferListener.getRatioTransferredList().size() - 1))
            .isNotEqualTo(100.0);
    }
}
