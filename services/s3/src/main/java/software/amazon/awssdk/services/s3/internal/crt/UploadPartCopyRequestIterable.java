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

package software.amazon.awssdk.services.s3.internal.crt;

import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.JAVA_PROGRESS_LISTENER;

import java.util.Iterator;
import java.util.NoSuchElementException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.s3.internal.multipart.SdkPojoConversionUtils;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;

/**
 * Iterable class to generate {@link UploadPartCopyRequest}s
 */
@SdkInternalApi
public final class UploadPartCopyRequestIterable implements SdkIterable<UploadPartCopyRequest> {

    private final String uploadId;
    private final long optimalPartSize;
    private final CopyObjectRequest copyObjectRequest;
    private final PublisherListener<Long> progressListener;
    private final long numberOfParts;
    private long remainingBytes;
    private int partNumber = 1;
    private long offset = 0;

    // 10000 is the reference value set in the TransferManager, since we don't know the content length yet
    private long progressUpdaterRemainingBytes = 10000;
    private long scaledPartValue;

    public UploadPartCopyRequestIterable(String uploadId,
                                         long partSize,
                                         CopyObjectRequest copyObjectRequest,
                                         long remainingBytes) {
        this.uploadId = uploadId;
        this.optimalPartSize = partSize;
        this.copyObjectRequest = copyObjectRequest;
        this.numberOfParts = (long) Math.ceil((double) remainingBytes / partSize);
        double partPercentage = (double) 1 / numberOfParts;
        this.scaledPartValue = (long) (partPercentage * 10000);
        this.remainingBytes = remainingBytes;
        this.progressListener = copyObjectRequest.overrideConfiguration().map(c -> c.executionAttributes()
                                                                                   .getAttribute(JAVA_PROGRESS_LISTENER))
                                                .orElseGet(PublisherListener::noOp);
    }

    @Override
    public Iterator<UploadPartCopyRequest> iterator() {
        return new UploadPartCopyRequestIterator();
    }

    private class UploadPartCopyRequestIterator implements Iterator<UploadPartCopyRequest> {
        @Override
        public boolean hasNext() {
            return remainingBytes > 0;
        }

        @Override
        public UploadPartCopyRequest next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No UploadPartCopyRequest available");
            }

            long partSize = Math.min(optimalPartSize, remainingBytes);
            String range = range(partSize);
            UploadPartCopyRequest uploadPartCopyRequest =
                SdkPojoConversionUtils.toUploadPartCopyRequest(copyObjectRequest,
                                                               partNumber,
                                                               uploadId,
                                                               range);
            updateProgressListener();
            partNumber++;
            offset += partSize;
            remainingBytes -= partSize;
            return uploadPartCopyRequest;
        }

        /**
         * Report progress to the listener as we send each part. Since we didn't know the content length when initializing the
         * TransferProgressUpdater in the TransferManager, we set it to a reference value of 10000. We need to scale the part
         * value based on the reference value to report the progress accurately.
         */
        private void updateProgressListener() {
            if (partNumber == numberOfParts) {
                progressListener.subscriberOnNext(progressUpdaterRemainingBytes);
                progressUpdaterRemainingBytes = 0;
            } else {
                progressListener.subscriberOnNext(scaledPartValue);
                progressUpdaterRemainingBytes -= scaledPartValue;
            }
        }

        private String range(long partSize) {
            return "bytes=" + offset + "-" + (offset + partSize - 1);
        }
    }
}
