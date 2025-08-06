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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlExtension;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

class MultipartS3AsyncClientTest {

    @Test
    void byteRangeManuallySpecified_shouldBypassMultipart() {
        S3AsyncClient mockDelegate = mock(S3AsyncClient.class);
        AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> mockTransformer =
            mock(AsyncResponseTransformer.class);
        GetObjectRequest req = GetObjectRequest.builder()
                                               .bucket("test-bucket")
                                               .key("test-key")
                                               .range("Range: bytes 0-499/1234")
                                               .build();
        S3AsyncClient s3AsyncClient = MultipartS3AsyncClient.create(mockDelegate, MultipartConfiguration.builder().build(), true);
        s3AsyncClient.getObject(req, mockTransformer);
        verify(mockTransformer, never()).split(any(SplittingTransformerConfiguration.class));
        verify(mockDelegate, times(1)).getObject(any(GetObjectRequest.class), eq(mockTransformer));
    }

    @Test
    void partManuallySpecified_shouldBypassMultipart() {
        S3AsyncClient mockDelegate = mock(S3AsyncClient.class);
        AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> mockTransformer =
            mock(AsyncResponseTransformer.class);
        GetObjectRequest req = GetObjectRequest.builder()
                                               .bucket("test-bucket")
                                               .key("test-key")
                                               .partNumber(1)
                                               .build();
        S3AsyncClient s3AsyncClient = MultipartS3AsyncClient.create(mockDelegate, MultipartConfiguration.builder().build(), true);
        s3AsyncClient.getObject(req, mockTransformer);
        verify(mockTransformer, never()).split(any(SplittingTransformerConfiguration.class));
        verify(mockDelegate, times(1)).getObject(any(GetObjectRequest.class), eq(mockTransformer));
    }

    @Test
    void presignedUrlExtension_rangeSpecified_shouldBypassMultipart() throws MalformedURLException {
        S3AsyncClient mockDelegate = mock(S3AsyncClient.class);
        AsyncPresignedUrlExtension mockDelegateExtension = mock(AsyncPresignedUrlExtension.class);
        AsyncResponseTransformer<GetObjectResponse, String> mockTransformer = mock(AsyncResponseTransformer.class);
        PresignedUrlDownloadRequest req = PresignedUrlDownloadRequest.builder()
                                                                     .presignedUrl(new URL("https://s3.amazonaws.com/bucket/key?signature=abc"))
                                                                     .range("bytes=0-1023")
                                                                     .build();
        when(mockDelegate.presignedUrlExtension()).thenReturn(mockDelegateExtension);
        S3AsyncClient s3AsyncClient = MultipartS3AsyncClient.create(mockDelegate, MultipartConfiguration.builder().build(), true);
        s3AsyncClient.presignedUrlExtension().getObject(req, mockTransformer);
        verify(mockTransformer, never()).split(any(SplittingTransformerConfiguration.class));
        verify(mockDelegateExtension, times(1)).getObject(eq(req), eq(mockTransformer));
    }

    @Test
    void presignedUrlExtension_noRange_shouldUseMultipart() throws MalformedURLException {
        S3AsyncClient mockDelegate = mock(S3AsyncClient.class);
        AsyncPresignedUrlExtension mockDelegateExtension = mock(AsyncPresignedUrlExtension.class);
        AsyncResponseTransformer<GetObjectResponse, String> mockTransformer = mock(AsyncResponseTransformer.class);
        AsyncResponseTransformer.SplitResult<GetObjectResponse, String> mockSplitResult = mock(AsyncResponseTransformer.SplitResult.class);
        PresignedUrlDownloadRequest req = PresignedUrlDownloadRequest.builder()
                                                                     .presignedUrl(new URL("https://s3.amazonaws.com/bucket/key?signature=abc"))
                                                                     .build();
        when(mockDelegate.presignedUrlExtension()).thenReturn(mockDelegateExtension);
        when(mockTransformer.split(any(SplittingTransformerConfiguration.class))).thenReturn(mockSplitResult);
        when(mockSplitResult.publisher()).thenReturn(mock(software.amazon.awssdk.core.async.SdkPublisher.class));
        S3AsyncClient s3AsyncClient = MultipartS3AsyncClient.create(mockDelegate, MultipartConfiguration.builder().build(), true);
        s3AsyncClient.presignedUrlExtension().getObject(req, mockTransformer);
        verify(mockTransformer, times(1)).split(any(SplittingTransformerConfiguration.class));
        verify(mockDelegateExtension, never()).getObject(any(PresignedUrlDownloadRequest.class), any(AsyncResponseTransformer.class));
    }

    @Test
    void presignedUrlExtension_shouldReturnMultipartExtension() {
        S3AsyncClient mockDelegate = mock(S3AsyncClient.class);
        AsyncPresignedUrlExtension mockDelegateExtension = mock(AsyncPresignedUrlExtension.class);
        when(mockDelegate.presignedUrlExtension()).thenReturn(mockDelegateExtension);

        S3AsyncClient s3AsyncClient = MultipartS3AsyncClient.create(mockDelegate, MultipartConfiguration.builder().build(), true);
        AsyncPresignedUrlExtension extension = s3AsyncClient.presignedUrlExtension();

        assertThat(extension).isInstanceOf(MultipartAsyncPresignedUrlExtension.class);
    }
}
