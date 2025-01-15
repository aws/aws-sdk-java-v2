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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;

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
}
