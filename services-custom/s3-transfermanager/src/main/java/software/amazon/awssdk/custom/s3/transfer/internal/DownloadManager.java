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

import static software.amazon.awssdk.utils.CompletableFutureUtils.allOfCancelForwarded;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.custom.s3.transfer.CompletedDownload;
import software.amazon.awssdk.custom.s3.transfer.Download;
import software.amazon.awssdk.custom.s3.transfer.DownloadObjectSpecification;
import software.amazon.awssdk.custom.s3.transfer.DownloadRequest;
import software.amazon.awssdk.custom.s3.transfer.DownloadState;
import software.amazon.awssdk.custom.s3.transfer.MultipartDownloadConfiguration;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Performs download operations for {@link software.amazon.awssdk.custom.s3.transfer.S3TransferManager}.
 */
@ThreadSafe
@SdkInternalApi
final class DownloadManager {
    private static final Logger log = Logger.loggerFor(DownloadManager.class);

    private final S3AsyncClient s3Client;
    private final MultipartDownloadConfiguration globalDownloadConfig;
    private final ConfigHelper configHelper;

    DownloadManager(S3AsyncClient s3Client, MultipartDownloadConfiguration globalDownloadConfig) {
        this.s3Client = s3Client;
        this.globalDownloadConfig = globalDownloadConfig;
        this.configHelper = new ConfigHelper(this.globalDownloadConfig);
    }

    Download downloadObject(DownloadRequest downloadRequest, TransferResponseTransformer partTransformerCreator) {
        DownloadObjectSpecification spec = downloadRequest.downloadSpecification();
        if (spec.isPresignedUrl()) {
            throw new UnsupportedOperationException("Downloading Presigned URL not supported");
        }

        GetObjectRequest getObjectRequest = spec.asApiRequest();
        return doApiDownload(downloadRequest, getObjectRequest, partTransformerCreator);
    }

    private Download doApiDownload(DownloadRequest downloadRequest,
                                   GetObjectRequest getObjectRequest,
                                   TransferResponseTransformer partTransformerCreator) {

        CompletableFuture<Void> partsCompleteFuture;
        if (!configHelper.useMultipartDownloads(downloadRequest)) {
            log.debug(() -> String.format("Multipart downloads disabled for %s, downloading as a single part", downloadRequest));
            partsCompleteFuture = doSinglePartDownload(downloadRequest, getObjectRequest, partTransformerCreator);
        } else {
            CompletableFuture<Long> objectSize = determineObjectSize(downloadRequest);

            partsCompleteFuture = objectSize.thenCompose(size -> {
                long threshold = configHelper.multipartDownloadThreshold(downloadRequest);
                if (size >= threshold) {
                    log.debug(() -> String.format("Downloading %s in multiple parts", downloadRequest));
                    return doMultipartDownload(downloadRequest, getObjectRequest, size, partTransformerCreator);
                }
                log.debug(() -> String.format("Size %d of %s does not meet configured threshold of %d bytes. " +
                                "Downloading as a single part", size, downloadRequest, threshold));
                return doSinglePartDownload(downloadRequest, getObjectRequest, partTransformerCreator);
            });
        }
        return new DownloadImpl(partsCompleteFuture);
    }

    private CompletableFuture<Void> doSinglePartDownload(DownloadRequest downloadRequest,
                                                         GetObjectRequest getObjectRequest,
                                                         TransferResponseTransformer transformerCreator) {
        SinglePartDownloadContext ctx = SinglePartDownloadContext.builder()
                .downloadRequest(downloadRequest)
                .getObjectRequest(getObjectRequest.toBuilder()
                        .overrideConfiguration(o -> o.addApiName(TransferManagerUtilities.apiName()))
                        .build())
                .build();
        try {
            AsyncResponseTransformer<GetObjectResponse, ?> transformer = transformerCreator.transformerForObject(ctx);
            return s3Client.getObject(getObjectRequest, transformer).thenApply(r -> null);
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    private CompletableFuture<Void> doMultipartDownload(DownloadRequest downloadRequest,
                                                        GetObjectRequest getObjectRequest,
                                                        long size,
                                                        TransferResponseTransformer transformerCreator) {
        try {
            List<MultipartDownloadContext> contexts = multipartContexts(downloadRequest, size, getObjectRequest);
            // Ensure we resolve the transformers first in case the callback throws an error while creating one of them
            List<TransformerContextPair> transformersWithContext =
                    createPartTransformersForContexts(contexts, transformerCreator);

            return allOfCancelForwarded(transformersWithContext.stream()
                    .map(pair -> s3Client.getObject(pair.context.partDownloadSpecification().asApiRequest(), pair.transformer))
                    .toArray(CompletableFuture[]::new));
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    private List<TransformerContextPair> createPartTransformersForContexts(List<MultipartDownloadContext> contexts,
                                                                           TransferResponseTransformer transformerCreator) {
        return contexts.stream()
                .map(ctx -> new TransformerContextPair(transformerCreator.transformerForObjectPart(ctx), ctx))
                .collect(Collectors.toList());
    }

    private List<MultipartDownloadContext> multipartContexts(DownloadRequest downloadRequest,
                                                             long size,
                                                             GetObjectRequest getObjectRequest) {
        int maxPartCount = configHelper.maxDownloadPartCount(downloadRequest);

        // First calculate maximum part count based on min part size. This
        // should always be positive since we enforce that min part size is <=
        // threshold
        long partCount = size / configHelper.minDownloadPartSize(downloadRequest);

        // Clamp to configured max part count
        long finalPartCount = Math.min(partCount, maxPartCount);

        // Calculate final part size
        long partSize = (long) Math.ceil((double) size / finalPartCount);

        log.debug(() -> String.format("Downloading %s in %d parts of size %d", downloadRequest, finalPartCount, partSize));

        List<MultipartDownloadContext> contexts = new ArrayList<>((int) finalPartCount);

        GetObjectRequest.Builder requestBuilder = getObjectRequest.toBuilder()
                .overrideConfiguration(o -> o.addApiName(TransferManagerUtilities.apiName()));

        long startByte = 0L;
        long remaining = size;
        for (int p = 0; p < finalPartCount; ++p) {
            int partNum = p + 1;
            boolean lastPart = partNum == finalPartCount;
            long sz = remaining > partSize ? partSize : remaining;
            // range is inclusive
            long a = startByte;
            long b = a + sz - 1;

            String range = TransferManagerUtilities.rangeHeaderValue(a, b);
            GetObjectRequest request = requestBuilder.range(range)
                    .build();

            MultipartDownloadContext ctx = MultipartDownloadContext.builder()
                    .downloadRequest(downloadRequest)
                    .partDownloadSpecification(DownloadObjectSpecification.fromApiRequest(request))
                    .partOffset(a)
                    .partNumber(partNum)
                    .size(sz)
                    .isLastPart(lastPart)
                    .build();

            contexts.add(ctx);

            startByte = a + sz;
            remaining -= sz;
        }

        return contexts;
    }

    /**
     * Determine the size of the object being requested. First checks if this
     * information is provided by the user, otherwise it gets it from S3.
     */
    // TODO: How do we want to deal with a request or presigned URL that has the Range header set?
    private CompletableFuture<Long> determineObjectSize(DownloadRequest downloadRequest) {
        Optional<Long> providedSize = downloadRequest.size();

        if (providedSize.isPresent()) {
            return CompletableFuture.completedFuture(providedSize.get());
        }

        DownloadObjectSpecification spec = downloadRequest.downloadSpecification();
        if (spec.isApiRequest()) {
            log.debug(() -> String.format("Object size not given for %s, retrieving size from S3", downloadRequest));
            return getObjectSizeFromS3(spec.asApiRequest());
        }

        return CompletableFutureUtils.failedFuture(
                new UnsupportedOperationException("Don't know how to get the size for spec " + spec));
    }

    /**
     * Perform a HeadObject call to determine the size of the object.
     */
    private CompletableFuture<Long> getObjectSizeFromS3(GetObjectRequest getObjectRequest) {
        HeadObjectRequest headObjectRequest = RequestConversionUtils.toHeadObjectRequest(getObjectRequest).toBuilder()
                .overrideConfiguration(o -> o.addApiName(TransferManagerUtilities.apiName()))
                .build();
        return s3Client.headObject(headObjectRequest).thenApply(HeadObjectResponse::contentLength);
    }

    private static final class DownloadImpl implements Download {
        private final CompletableFuture<CompletedDownload> completionFuture;
        private final CompletableFuture<?> partsCompleteFuture;

        DownloadImpl(CompletableFuture<?> partsCompleteFuture) {
            this.partsCompleteFuture = partsCompleteFuture;
            this.completionFuture = this.partsCompleteFuture.thenApply(ignored -> null);
            CompletableFutureUtils.forwardExceptionTo(this.completionFuture, this.partsCompleteFuture);
        }

        @Override
        public CompletableFuture<CompletedDownload> completionFuture() {
            return completionFuture;
        }

        @Override
        public DownloadState pause() {
            // TODO: Implement download pause
            throw new UnsupportedOperationException();
        }
    }

    private static class TransformerContextPair {
        private final AsyncResponseTransformer<GetObjectResponse, ?> transformer;
        private final MultipartDownloadContext context;

        TransformerContextPair(AsyncResponseTransformer<GetObjectResponse, ?> transformer,
                                      MultipartDownloadContext context) {
            this.transformer = transformer;
            this.context = context;
        }
    }
}
