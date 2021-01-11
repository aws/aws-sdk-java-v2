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

package software.amazon.awssdk.custom.s3.transfer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.custom.s3.transfer.DownloadRequest;
import software.amazon.awssdk.custom.s3.transfer.TransferOverrideConfiguration;
import software.amazon.awssdk.custom.s3.transfer.util.SizeConstant;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.custom.s3.transfer.DownloadObjectSpecification;
import software.amazon.awssdk.custom.s3.transfer.MultipartDownloadConfiguration;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Tests for {@link DownloadManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DownloadManagerTest {
    private static final String BUCKET = "foo";
    private static final String KEY = "key";
    private static final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(BUCKET)
            .key(KEY)
            .build();

    @Mock
    private S3AsyncClient s3Client;

    private DownloadManager dm;

    @Before
    public void methodSetup() {
        dm = new DownloadManager(s3Client, MultipartDownloadConfiguration.defaultConfig());

        when(s3Client.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
                .thenAnswer(i -> CompletableFuture.completedFuture(GetObjectResponse.builder().build()));
    }

    @Test
    public void sizeProvided_DoesNotCallHeadObject_SinglePart() {
        DownloadRequest req = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(1L)
                .build();

        AsyncResponseTransformer mockTransformer = mock(AsyncResponseTransformer.class);
        TransferResponseTransformer transformerCreator = mock(TransferResponseTransformer.class);
        when(transformerCreator.transformerForObject(any())).thenReturn(mockTransformer);
        dm.downloadObject(req, transformerCreator).completionFuture().join();

        verify(s3Client, times(0)).headObject(any(HeadObjectRequest.class));
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));
    }

    @Test
    public void sizeProvided_DoesNotCallHeadObject_Multipart() {
        DownloadRequest req = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(16 * SizeConstant.MiB)
                .build();

        AsyncResponseTransformer mockTransformer = mock(AsyncResponseTransformer.class);
        TransferResponseTransformer transformerCreator = mock(TransferResponseTransformer.class);
        when(transformerCreator.transformerForObjectPart(any())).thenReturn(mockTransformer);
        dm.downloadObject(req, transformerCreator).completionFuture().join();

        verify(s3Client, times(0)).headObject(any(HeadObjectRequest.class));
        verify(s3Client, times(3)).getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));
    }

    @Test
    public void sizeNotProvided_CallsHeadObject() {
        DownloadRequest req = DownloadRequest.forBucketAndKey(BUCKET, KEY);

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder()
                        .contentLength(1L)
                        .build()));

        AsyncResponseTransformer mockTransformer = mock(AsyncResponseTransformer.class);
        TransferResponseTransformer transformerCreator = mock(TransferResponseTransformer.class);
        when(transformerCreator.transformerForObject(any())).thenReturn(mockTransformer);
        dm.downloadObject(req, transformerCreator).completionFuture().join();

        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
        // 1byte object, should be single part
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));
    }

    @Test
    public void sizeNotProvided_CallsHeadObject_Multipart() {
        DownloadRequest req = DownloadRequest.forBucketAndKey(BUCKET, KEY);

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder()
                        .contentLength(16 * SizeConstant.MiB)
                        .build()));

        AsyncResponseTransformer mockTransformer = mock(AsyncResponseTransformer.class);
        TransferResponseTransformer transformerCreator = mock(TransferResponseTransformer.class);
        when(transformerCreator.transformerForObject(any())).thenReturn(mockTransformer);
        dm.downloadObject(req, transformerCreator).completionFuture().join();

        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
        verify(s3Client, times(3)).getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));
    }

    @Test
    public void sizeNotProvided_HeadObjectFails_PropagatedToCompletionFuture() {
        DownloadRequest req = DownloadRequest.forBucketAndKey(BUCKET, KEY);

        AwsServiceException expected = S3Exception.builder().build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(CompletableFutureUtils.failedFuture(expected));
        TransferResponseTransformer mockTransformerCreator = mock(TransferResponseTransformer.class);

        assertThatThrownBy(dm.downloadObject(req, mockTransformerCreator).completionFuture()::join)
                .hasCause(expected);
    }

    @Test
    public void singlePartDownload_FactoryThrows_PropagatedToCompletionFuture() {
        DownloadRequest req = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(1L)
                .build();

        UnsupportedOperationException expected = new UnsupportedOperationException("I don't support single part downloads!");
        TransferResponseTransformer transformerCreator = mock(TransferResponseTransformer.class);
        when(transformerCreator.transformerForObject(any())).thenThrow(expected);
        assertThatThrownBy(dm.downloadObject(req, transformerCreator).completionFuture()::join)
                .hasCause(expected);
    }

    @Test
    public void multiPartDownload_FactoryThrows_PropagatedToCompletionFuture() {
        DownloadRequest req = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(16 * SizeConstant.GiB)
                .build();

        UnsupportedOperationException expected = new UnsupportedOperationException("I don't support multipart downloads!");
        TransferResponseTransformer transformerCreator = mock(TransferResponseTransformer.class);
        when(transformerCreator.transformerForObjectPart(any())).thenThrow(expected);
        assertThatThrownBy(dm.downloadObject(req, transformerCreator).completionFuture()::join)
                .hasCause(expected);
    }

    @Test
    public void respectsOverrideConfiguration_MultipartDisabled() {
        MultipartDownloadConfiguration overrideDownloadConfig = MultipartDownloadConfiguration.defaultConfig()
                .toBuilder()
                .enableMultipartDownloads(false)
                .build();

        DownloadRequest downloadRequest = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(16 * SizeConstant.MiB)
                .overrideConfiguration(TransferOverrideConfiguration.builder()
                        .multipartDownloadConfiguration(overrideDownloadConfig)
                        .build())
                .build();


        TransferResponseTransformer mockTransformerCreator = mock(TransferResponseTransformer.class);
        AsyncResponseTransformer mockTransformer = mock(AsyncResponseTransformer.class);
        when(mockTransformerCreator.transformerForObject(any(SinglePartDownloadContext.class))).thenReturn(mockTransformer);

        dm.downloadObject(downloadRequest, mockTransformerCreator).completionFuture().join();

        verify(mockTransformerCreator, times(0)).transformerForObjectPart(any(MultipartDownloadContext.class));
    }

    @Test
    public void respectsOverrideConfiguration_MultipartEnabled() {
        MultipartDownloadConfiguration globalDownloadConfig = MultipartDownloadConfiguration.defaultConfig()
                .toBuilder()
                // Disable multipart downloads globally
                .enableMultipartDownloads(false)
                .build();

        dm = new DownloadManager(s3Client, globalDownloadConfig);

        DownloadRequest downloadRequest = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(16 * SizeConstant.MiB)
                .overrideConfiguration(TransferOverrideConfiguration.builder()
                        // Use the default config which enables multipart downloads
                        .multipartDownloadConfiguration(MultipartDownloadConfiguration.defaultConfig())
                        .build())
                .build();

        TransferResponseTransformer mockTransformerCreator = mock(TransferResponseTransformer.class);
        AsyncResponseTransformer mockTransformer = mock(AsyncResponseTransformer.class);
        when(mockTransformerCreator.transformerForObjectPart(any(MultipartDownloadContext.class))).thenReturn(mockTransformer);

        dm.downloadObject(downloadRequest, mockTransformerCreator).completionFuture().join();

        verify(mockTransformerCreator, times(0)).transformerForObject(any(SinglePartDownloadContext.class));
    }

    @Test
    public void respectsOverrideConfiguration_DifferentPartitioning() {
        MultipartDownloadConfiguration overrideConfig = MultipartDownloadConfiguration.builder()
                .minDownloadPartSize(8 * SizeConstant.MiB)
                .build();

        DownloadRequest downloadRequest = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(24 * SizeConstant.MiB)
                .overrideConfiguration(TransferOverrideConfiguration.builder()
                        .multipartDownloadConfiguration(overrideConfig)
                        .build())
                .build();

        MultipartDownloadContext[] expectedContexts = new MultipartDownloadContext[] {
                MultipartDownloadContext.builder()
                        .downloadRequest(downloadRequest)
                        .partNumber(1)
                        .partOffset(0)
                        .isLastPart(false)
                        .size(8 * SizeConstant.MiB)
                        .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest.toBuilder()
                                .range(TransferManagerUtilities.rangeHeaderValue(0, 8 * SizeConstant.MiB - 1))
                                .build()))
                        .build(),
                MultipartDownloadContext.builder()
                        .downloadRequest(downloadRequest)
                        .partNumber(2)
                        .partOffset(8 * SizeConstant.MiB)
                        .isLastPart(false)
                        .size(8 * SizeConstant.MiB)
                        .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest.toBuilder()
                                .range(TransferManagerUtilities.rangeHeaderValue(8 * SizeConstant.MiB, 16 * SizeConstant.MiB - 1))
                                .build()))
                        .build(),
                MultipartDownloadContext.builder()
                        .downloadRequest(downloadRequest)
                        .partNumber(3)
                        .partOffset(16 * SizeConstant.MiB)
                        .isLastPart(true)
                        .size(8 * SizeConstant.MiB)
                        .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest.toBuilder()
                                .range(TransferManagerUtilities.rangeHeaderValue(16 * SizeConstant.MiB, 24 * SizeConstant.MiB - 1))
                                .build()))
                        .build()
        };

        MultipartDownloadConfiguration globalConfig = MultipartDownloadConfiguration.builder()
                .maxDownloadPartCount(MultipartDownloadConfiguration.DEFAULT_MAX_DOWNLOAD_PART_COUNT- 1)
                .build();

        multipartDownloadPartitioningTest(downloadRequest, globalConfig, expectedContexts);
    }

    @Test
    public void multipartDownload_EvenSplit() {
        DownloadRequest downloadRequest = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(16 * SizeConstant.MiB)
                .build();

        MultipartDownloadConfiguration multipartDownloadConfiguration = MultipartDownloadConfiguration.builder()
                .enableMultipartDownloads(true)
                .multipartDownloadThreshold(16 * SizeConstant.MiB)
                .maxDownloadPartCount(2)
                .minDownloadPartSize(8 * SizeConstant.MiB)
                .build();

        MultipartDownloadContext[] expectedContexts = new MultipartDownloadContext[] {
                MultipartDownloadContext.builder()
                        .downloadRequest(downloadRequest)
                        .partNumber(1)
                        .partOffset(0)
                        .isLastPart(false)
                        .size(8 * SizeConstant.MiB)
                        .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest.toBuilder()
                                .range(TransferManagerUtilities.rangeHeaderValue(0, 8 * SizeConstant.MiB - 1))
                                .build()))
                        .build(),
                MultipartDownloadContext.builder()
                        .downloadRequest(downloadRequest)
                        .partNumber(2)
                        .partOffset(8 * SizeConstant.MiB)
                        .isLastPart(true)
                        .size(8 * SizeConstant.MiB)
                        .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest.toBuilder()
                                .range(TransferManagerUtilities.rangeHeaderValue(8 * SizeConstant.MiB, 16 * SizeConstant.MiB - 1))
                                .build()))
                        .build()
        };

        multipartDownloadPartitioningTest(downloadRequest, multipartDownloadConfiguration, expectedContexts);
    }

    @Test
    public void multipartDownload_UnevenSplit() {
        DownloadRequest downloadRequest = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(16 * SizeConstant.MiB + 1)
                .build();

        MultipartDownloadConfiguration multipartDownloadConfiguration = MultipartDownloadConfiguration.builder()
                .enableMultipartDownloads(true)
                .multipartDownloadThreshold(16 * SizeConstant.MiB)
                .maxDownloadPartCount(2)
                .minDownloadPartSize(8 * SizeConstant.MiB)
                .build();

        MultipartDownloadContext[] expectedContexts = new MultipartDownloadContext[] {
                MultipartDownloadContext.builder()
                        .downloadRequest(downloadRequest)
                        .partNumber(1)
                        .partOffset(0)
                        .isLastPart(false)
                        // DownloadManager takes the ceiling of the floating
                        // division when determining part size so we know the
                        // first part has the larger size
                        .size(8 * SizeConstant.MiB + 1)
                        .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest.toBuilder()
                                .range(TransferManagerUtilities.rangeHeaderValue(0, 8 * SizeConstant.MiB))
                                .build()))
                        .build(),
                MultipartDownloadContext.builder()
                        .downloadRequest(downloadRequest)
                        .partNumber(2)
                        .partOffset(8 * SizeConstant.MiB + 1)
                        .isLastPart(true)
                        .size(8 * SizeConstant.MiB)
                        .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest.toBuilder()
                                .range(TransferManagerUtilities.rangeHeaderValue(8 * SizeConstant.MiB + 1, 16 * SizeConstant.MiB))
                                .build()))
                        .build()
        };

        multipartDownloadPartitioningTest(downloadRequest, multipartDownloadConfiguration, expectedContexts);
    }

    @Test
    public void multipartDownload_HighMaxPartCount_RetainsMinPartSize() {
        DownloadRequest downloadRequest = DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest))
                .size(16 * SizeConstant.MiB)
                .build();

        MultipartDownloadConfiguration multipartDownloadConfiguration = MultipartDownloadConfiguration.builder()
                .enableMultipartDownloads(true)
                .multipartDownloadThreshold(16 * SizeConstant.MiB)
                .maxDownloadPartCount(32)
                .minDownloadPartSize(8 * SizeConstant.MiB)
                .build();

        MultipartDownloadContext[] expectedContexts = new MultipartDownloadContext[] {
                MultipartDownloadContext.builder()
                        .downloadRequest(downloadRequest)
                        .partNumber(1)
                        .partOffset(0)
                        .isLastPart(false)
                        .size(8 * SizeConstant.MiB)
                        .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest.toBuilder()
                                .range(TransferManagerUtilities.rangeHeaderValue(0, 8 * SizeConstant.MiB - 1))
                                .build()))
                        .build(),
                MultipartDownloadContext.builder()
                        .downloadRequest(downloadRequest)
                        .partNumber(2)
                        .partOffset(8 * SizeConstant.MiB)
                        .isLastPart(true)
                        .size(8 * SizeConstant.MiB)
                        .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(getObjectRequest.toBuilder()
                                .range(TransferManagerUtilities.rangeHeaderValue(8 * SizeConstant.MiB, 16 * SizeConstant.MiB - 1))
                                .build()))
                        .build()
        };

        multipartDownloadPartitioningTest(downloadRequest, multipartDownloadConfiguration, expectedContexts);
    }

    private void multipartDownloadPartitioningTest(DownloadRequest downloadRequest,
                                                   MultipartDownloadConfiguration multipartDownloadConfiguration,
                                                   MultipartDownloadContext[] expectedContexts) {

        TransferResponseTransformer transformerCreator = mock(TransferResponseTransformer.class);

        List<ContextAndTransformerPair> contextAndTransformerPairs = new ArrayList<>();
        List<MultipartDownloadContext> createdContexts = new ArrayList<>();
        when(transformerCreator.transformerForObjectPart(any(MultipartDownloadContext.class)))
                .thenAnswer((Answer<AsyncResponseTransformer>) invocationOnMock -> {
                    MultipartDownloadContext ctx = invocationOnMock.getArgumentAt(0, MultipartDownloadContext.class);
                    createdContexts.add(ctx);
                    AsyncResponseTransformer transformer = mock(AsyncResponseTransformer.class);
                    contextAndTransformerPairs.add(new ContextAndTransformerPair(ctx, transformer));
                    return transformer;
                });

        dm = new DownloadManager(s3Client, multipartDownloadConfiguration);

        dm.downloadObject(downloadRequest, transformerCreator).completionFuture().join();

        verify(transformerCreator, times(expectedContexts.length)).transformerForObjectPart(any(MultipartDownloadContext.class));

        for (int i = 0; i < createdContexts.size(); i++) {
            MultipartDownloadContext expected = expectedContexts[i];
            MultipartDownloadContext actualContext = createdContexts.get(i);
            assertThat(expected).isEqualToIgnoringGivenFields(actualContext, "partDownloadSpecification", "downloadRequest");
            assertThat(expected.partDownloadSpecification().asApiRequest().equalsBySdkFields(actualContext.partDownloadSpecification().asApiRequest())).isTrue();
            assertThat(expected.downloadRequest()).isEqualTo(actualContext.downloadRequest());
        }

        for (ContextAndTransformerPair pair : contextAndTransformerPairs) {
            verify(s3Client).getObject(any(GetObjectRequest.class), eq(pair.transformer));
        }
    }

    private static final class ContextAndTransformerPair {
        private final MultipartDownloadContext ctx;
        private final AsyncResponseTransformer transformer;

        ContextAndTransformerPair(MultipartDownloadContext ctx, AsyncResponseTransformer transformer) {
            this.ctx = ctx;
            this.transformer = transformer;
        }
    }
}
