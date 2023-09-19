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

import java.util.Iterator;
import java.util.NoSuchElementException;
import software.amazon.awssdk.annotations.SdkInternalApi;
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
    private long remainingBytes;
    private int partNumber = 1;
    private long offset = 0;

    public UploadPartCopyRequestIterable(String uploadId,
                                         long partSize,
                                         CopyObjectRequest copyObjectRequest,
                                         long remainingBytes) {
        this.uploadId = uploadId;
        this.optimalPartSize = partSize;
        this.copyObjectRequest = copyObjectRequest;
        this.remainingBytes = remainingBytes;
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
            partNumber++;
            offset += partSize;
            remainingBytes -= partSize;
            return uploadPartCopyRequest;
        }

        private String range(long partSize) {
            return "bytes=" + offset + "-" + (offset + partSize - 1);
        }
    }
}
