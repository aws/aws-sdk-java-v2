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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.PresignedDownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.PresignedDownloadRequest;

class CrtS3TransferManagerPresignedUrlDownloadTest {
    private static final String PRESIGNED_URL = "https://example.com/presigned-url";

    @TempDir
    Path tempDir;

    private S3AsyncClient crtClient;
    private S3TransferManager tm;
    private PresignedUrlDownloadRequest presignedUrlDownloadRequest;

    @BeforeEach
    void setUp() throws Exception {
        crtClient = S3AsyncClient.crtBuilder()
                                 .region(Region.US_EAST_1)
                                 .credentialsProvider(StaticCredentialsProvider.create(
                                     AwsBasicCredentials.create("key", "secret")))
                                 .build();
        tm = S3TransferManager.builder().s3Client(crtClient).build();
        presignedUrlDownloadRequest = PresignedUrlDownloadRequest.builder()
                                                                 .presignedUrl(new URL(PRESIGNED_URL))
                                                                 .build();
    }

    @AfterEach
    void tearDown() {
        tm.close();
        crtClient.close();
    }

    @Test
    void downloadFileWithPresignedUrl_crtClient_completesExceptionallyWithUnsupportedOperationException() {
        PresignedDownloadFileRequest request =
            PresignedDownloadFileRequest.builder()
                                        .presignedUrlDownloadRequest(presignedUrlDownloadRequest)
                                        .destination(tempDir.resolve("download.txt"))
                                        .build();

        assertThatThrownBy(() -> tm.downloadFileWithPresignedUrl(request).completionFuture().join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Presigned URL downloads are not supported by the AWS CRT-based S3 client");
    }

    @Test
    void downloadWithPresignedUrl_crtClient_completesExceptionallyWithUnsupportedOperationException() {
        PresignedDownloadRequest<ResponseBytes<GetObjectResponse>> request =
            PresignedDownloadRequest.builder()
                                    .presignedUrlDownloadRequest(presignedUrlDownloadRequest)
                                    .responseTransformer(AsyncResponseTransformer.toBytes())
                                    .build();

        assertThatThrownBy(() -> tm.downloadWithPresignedUrl(request).completionFuture().join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Presigned URL downloads are not supported by the AWS CRT-based S3 client");
    }
}
