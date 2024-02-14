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

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class MpuRequestContext {
    private final Pair<PutObjectRequest, AsyncRequestBody> request;
    private final long contentLength;
    private final long partSize;
    private final long numPartsCompleted;
    private final String uploadId;
    private final Map<Integer, CompletedPart> existingParts;

    protected MpuRequestContext(Pair<PutObjectRequest, AsyncRequestBody> request,
                              long contentLength,
                              long partSize,
                              String uploadId,
                              Map<Integer, CompletedPart> existingParts,
                              long numPartsCompleted) {
        this.request = request;
        this.contentLength = contentLength;
        this.partSize = partSize;
        this.uploadId = uploadId;
        this.existingParts = existingParts;
        this.numPartsCompleted = numPartsCompleted;
    }

    public Pair<PutObjectRequest, AsyncRequestBody> request() {
        return request;
    }

    public long contentLength() {
        return contentLength;
    }

    public long partSize() {
        return partSize;
    }

    public long numPartsCompleted() {
        return numPartsCompleted;
    }

    public String uploadId() {
        return uploadId;
    }

    public Map<Integer, CompletedPart> existingParts() {
        return Collections.unmodifiableMap(existingParts);
    }
}
