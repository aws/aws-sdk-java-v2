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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;

class MultipartDownloadJavaBasedTest {
    private S3AsyncClient mockDelegate;
    private MultipartS3AsyncClient s3Multi;
    private S3TransferManager tm;
    private UploadDirectoryHelper uploadDirectoryHelper;
    private DownloadDirectoryHelper downloadDirectoryHelper;
    private TransferManagerConfiguration configuration;

    @BeforeEach
    public void methodSetup() {
        mockDelegate = mock(S3AsyncClient.class);
        s3Multi = MultipartS3AsyncClient.create(mockDelegate, MultipartConfiguration.builder().build());
        uploadDirectoryHelper = mock(UploadDirectoryHelper.class);
        configuration = mock(TransferManagerConfiguration.class);
        downloadDirectoryHelper = mock(DownloadDirectoryHelper.class);
        tm = new GenericS3TransferManager(s3Multi, uploadDirectoryHelper, configuration, downloadDirectoryHelper);
    }

    @Test
    void usingMultipartDownload_shouldNotThrowException() {
        GetObjectResponse response = GetObjectResponse.builder().build();
        when(mockDelegate.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletedFileDownload completedFileDownload = tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("bucket")
                                                                                                    .key("key"))
                                                                            .destination(Paths.get(".")))
                                                        .completionFuture()
                                                        .join();
        assertThat(completedFileDownload.response()).isEqualTo(response);
    }
}
