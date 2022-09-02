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

package software.amazon.awssdk.services.s3.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;

/**
 * Factory class to create UploadPartCopyRequest
 */
@SdkInternalApi
public final class UploadPartCopyRequestProvider {

    private final String uploadId;
    private final long optimalPartSize;
    private final CopyObjectRequest copyObjectRequest;
    private long remainingBytes;
    private int partNumber = 1;
    private long offset = 0;

    public UploadPartCopyRequestProvider(String uploadId, long partSize, CopyObjectRequest copyObjectRequest, long remainingBytes) {
        this.uploadId = uploadId;
        this.optimalPartSize = partSize;
        this.copyObjectRequest = copyObjectRequest;
        this.remainingBytes = remainingBytes;
    }

    public boolean hasMoreRequests() {
        return remainingBytes > 0;
    }

    public UploadPartCopyRequest nextCopyPartRequest() {
        long partSize = Math.min(optimalPartSize, remainingBytes);
        UploadPartCopyRequest uploadPartCopyRequest = UploadPartCopyRequest.builder()
                                                                           .sourceBucket(copyObjectRequest.sourceBucket())
                                                                           .sourceKey(copyObjectRequest.sourceKey())
                                                                           .sourceVersionId(copyObjectRequest.sourceVersionId())
                                                                           .uploadId(uploadId)
                                                                           .partNumber(partNumber++)
                                                                           .destinationBucket(copyObjectRequest.destinationBucket())
                                                                           .destinationKey(copyObjectRequest.destinationKey())
                                                                           .copySourceIfMatch(copyObjectRequest.copySourceIfMatch())
                                                                           .copySourceIfNoneMatch(copyObjectRequest.copySourceIfNoneMatch())
                                                                           .copySourceIfUnmodifiedSince(copyObjectRequest.copySourceIfUnmodifiedSince())
                                                                           .copySourceRange(range(partSize))
                                                                           .copySourceSSECustomerAlgorithm(copyObjectRequest.copySourceSSECustomerAlgorithm())
                                                                           .copySourceSSECustomerKeyMD5(copyObjectRequest.copySourceSSECustomerKeyMD5())
                                                                           .copySourceIfModifiedSince(copyObjectRequest.copySourceIfModifiedSince())
                                                                           .copySourceSSECustomerKey(copyObjectRequest.copySourceSSECustomerKey())
                                                                           .expectedBucketOwner(copyObjectRequest.expectedBucketOwner())
                                                                           .expectedSourceBucketOwner(copyObjectRequest.expectedSourceBucketOwner())
                                                                           .requestPayer(copyObjectRequest.requestPayer())
                                                                           .build();

        offset += partSize;
        remainingBytes -= partSize;
        return uploadPartCopyRequest;
    }

    private String range(long partSize) {
        return "bytes=" + offset + "-" + (offset + partSize - 1);
    }
}
