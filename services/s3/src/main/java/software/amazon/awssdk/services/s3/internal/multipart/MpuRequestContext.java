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
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class MpuRequestContext {

    private final Pair<PutObjectRequest, AsyncRequestBody> request;
    private final Long contentLength;
    private final Long partSize;
    private final Long numPartsCompleted;
    private final String uploadId;
    private final Map<Integer, CompletedPart> existingParts;

    protected MpuRequestContext(Builder builder) {
        this.request = builder.request;
        this.contentLength = builder.contentLength;
        this.partSize = builder.partSize;
        this.uploadId = builder.uploadId;
        this.existingParts = builder.existingParts;
        this.numPartsCompleted = builder.numPartsCompleted;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MpuRequestContext that = (MpuRequestContext) o;

        return Objects.equals(request, that.request) && Objects.equals(contentLength, that.contentLength)
               && Objects.equals(partSize, that.partSize) && Objects.equals(numPartsCompleted, that.numPartsCompleted)
               && Objects.equals(uploadId, that.uploadId) && Objects.equals(existingParts, that.existingParts);
    }

    @Override
    public int hashCode() {
        int result = request != null ? request.hashCode() : 0;
        result = 31 * result + (uploadId != null ? uploadId.hashCode() : 0);
        result = 31 * result + (existingParts != null ? existingParts.hashCode() : 0);
        result = 31 * result + (contentLength != null ? contentLength.hashCode() : 0);
        result = 31 * result + (partSize != null ? partSize.hashCode() : 0);
        result = 31 * result + (numPartsCompleted != null ? numPartsCompleted.hashCode() : 0);
        return result;
    }

    public Pair<PutObjectRequest, AsyncRequestBody> request() {
        return request;
    }

    public Long contentLength() {
        return contentLength;
    }

    public Long partSize() {
        return partSize;
    }

    public Long numPartsCompleted() {
        return numPartsCompleted;
    }

    public String uploadId() {
        return uploadId;
    }

    public Map<Integer, CompletedPart> existingParts() {
        return existingParts != null ? Collections.unmodifiableMap(existingParts) : null;
    }

    public static final class Builder {
        private Pair<PutObjectRequest, AsyncRequestBody> request;
        private Long contentLength;
        private Long partSize;
        private Long numPartsCompleted;
        private String uploadId;
        private Map<Integer, CompletedPart> existingParts;

        private Builder() {
        }

        public Builder request(Pair<PutObjectRequest, AsyncRequestBody> request) {
            this.request = request;
            return this;
        }

        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public Builder partSize(Long partSize) {
            this.partSize = partSize;
            return this;
        }

        public Builder numPartsCompleted(Long numPartsCompleted) {
            this.numPartsCompleted = numPartsCompleted;
            return this;
        }

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder existingParts(Map<Integer, CompletedPart> existingParts) {
            this.existingParts = existingParts;
            return this;
        }

        public MpuRequestContext build() {
            return new MpuRequestContext(this);
        }
    }
}
