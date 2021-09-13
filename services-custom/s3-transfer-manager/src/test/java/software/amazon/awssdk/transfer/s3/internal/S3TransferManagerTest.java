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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.transfer.s3.CompletedDownload;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.DownloadRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.UploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3TransferManagerTest {
    private S3CrtAsyncClient mockS3Crt;
    private S3TransferManager tm;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void methodSetup() {
        mockS3Crt = mock(S3CrtAsyncClient.class);
        tm = new DefaultS3TransferManager(mockS3Crt);
    }

    @After
    public void methodTeardown() {
        tm.close();
    }

    @Test
    public void upload_returnsResponse() {
        PutObjectResponse response = PutObjectResponse.builder().build();
        when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        CompletedUpload completedUpload = tm.upload(UploadRequest.builder()
                                                                 .putObjectRequest(r -> r.bucket("bucket")
                                                                                         .key("key"))
                                                                 .source(Paths.get("."))
                                                                 .build())
                                            .completionFuture()
                                            .join();

        assertThat(completedUpload.response()).isEqualTo(response);
    }

    @Test
    public void upload_cancel_shouldForwardCancellation() {
        CompletableFuture<PutObjectResponse> s3CrtFuture = new CompletableFuture<>();
        when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(s3CrtFuture);

        CompletableFuture<CompletedUpload> future = tm.upload(UploadRequest.builder()
                                                                                    .putObjectRequest(r -> r.bucket("bucket")
                                                                                         .key("key"))
                                                                                    .source(Paths.get("."))
                                                                                    .build())
                                                               .completionFuture();

        future.cancel(true);
        assertThat(s3CrtFuture).isCancelled();
    }

    @Test
    public void download_returnsResponse() {
        GetObjectResponse response = GetObjectResponse.builder().build();
        when(mockS3Crt.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletedDownload completedDownload = tm.download(DownloadRequest.builder()
                                                                         .getObjectRequest(r -> r.bucket("bucket")
                                                                                                 .key("key"))
                                                                         .destination(Paths.get("."))
                                                                         .build())
                                                .completionFuture()
                                                .join();
        assertThat(completedDownload.response()).isEqualTo(response);
    }

    @Test
    public void download_cancel_shouldForwardCancellation() {
        CompletableFuture<GetObjectResponse> s3CrtFuture = new CompletableFuture<>();
        when(mockS3Crt.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(s3CrtFuture);

        CompletableFuture<CompletedDownload> future = tm.download(DownloadRequest.builder()
                                                                                 .getObjectRequest(r -> r.bucket("bucket")
                                                                                                         .key("key"))
                                                                                 .destination(Paths.get("."))
                                                                                 .build())
                                                        .completionFuture();
        future.cancel(true);
        assertThat(s3CrtFuture).isCancelled();
    }


}
