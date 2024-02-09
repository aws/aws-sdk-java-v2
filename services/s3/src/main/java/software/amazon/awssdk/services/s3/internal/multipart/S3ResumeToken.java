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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class S3ResumeToken {

    private final String uploadId;
    private final long partSize;
    private final long totalNumParts;
    private final long numPartsCompleted;

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

    public long partSize() {
        return partSize;
    }

    public long totalNumParts() {
        return totalNumParts;
    }

    public long numPartsCompleted() {
        return numPartsCompleted;
    }

    public static final class Builder {
        private String uploadId;
        private long partSize;
        private long totalNumParts;
        private long numPartsCompleted;

        private Builder() {
        }

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder partSize(long partSize) {
            this.partSize = partSize;
            return this;
        }

        public Builder totalNumParts(long totalNumParts) {
            this.totalNumParts = totalNumParts;
            return this;
        }

        public Builder numPartsCompleted(long numPartsCompleted) {
            this.numPartsCompleted = numPartsCompleted;
            return this;
        }

        public S3ResumeToken build() {
            return new S3ResumeToken(this);
        }
    }
}
