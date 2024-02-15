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

package software.amazon.awssdk.services.s3.multipart;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class S3ResumeToken {

    private final String uploadId;
    private final Long partSize;
    private final Long totalNumParts;
    private final Long numPartsCompleted;

    public S3ResumeToken(Builder builder) {
        this.uploadId = builder.uploadId;
        this.partSize = builder.partSize;
        this.totalNumParts = builder.totalNumParts;
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
        S3ResumeToken that = (S3ResumeToken) o;

        return partSize == that.partSize && totalNumParts == that.totalNumParts && numPartsCompleted == that.numPartsCompleted
               && Objects.equals(uploadId, that.uploadId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uploadId);
    }

    public String uploadId() {
        return uploadId;
    }

    public Long partSize() {
        return partSize;
    }

    public Long totalNumParts() {
        return totalNumParts;
    }

    public Long numPartsCompleted() {
        return numPartsCompleted;
    }

    public static final class Builder {
        private String uploadId;
        private Long partSize;
        private Long totalNumParts;
        private Long numPartsCompleted;

        private Builder() {
        }

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder partSize(Long partSize) {
            this.partSize = partSize;
            return this;
        }

        public Builder totalNumParts(Long totalNumParts) {
            this.totalNumParts = totalNumParts;
            return this;
        }

        public Builder numPartsCompleted(Long numPartsCompleted) {
            this.numPartsCompleted = numPartsCompleted;
            return this;
        }

        public S3ResumeToken build() {
            return new S3ResumeToken(this);
        }
    }
}
