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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class MultipartDownloaderTest {

    private S3AsyncClient s3AsyncClient;
    private MultipartDownloader<GetObjectResponse> downloader;

    @BeforeEach
    void init() {
        this.s3AsyncClient = mock(S3AsyncClient.class);
        this.downloader = new MultipartDownloader<>(s3AsyncClient, 16 * 1024);
    }

    @Test
    void downloadObject_multiplePartLessThanBuffer_shouldDownloadInMultipleChunk() {
        GetObjectRequest request = getObjectRequest();
        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer =
            mock(AsyncResponseTransformer.class); // todo make mock work
        GetObjectResponse response = GetObjectResponse.builder()
                                                      .partsCount(4)
                                                      .contentLength(1000L)
                                                      .build();
        when(s3AsyncClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
        downloader.getObject(request, transformer).join();
        verify(s3AsyncClient, times(4))
            .getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));
    }

    @Test
    void downloadObject_multiplePartMoreThanBuffer_shouldDownloadInMultipleChunk() {

    }

    @Test
    void downloadObject_singlePartLessThanBuffer_shouldDownloadInOneChunk() {

    }

    @Test
    void downloadObject_singlePartExceedsBuffer_shouldDownloadInOneChunk() {

    }

    private GetObjectRequest getObjectRequest() {
        return GetObjectRequest.builder().build();
    }
}
