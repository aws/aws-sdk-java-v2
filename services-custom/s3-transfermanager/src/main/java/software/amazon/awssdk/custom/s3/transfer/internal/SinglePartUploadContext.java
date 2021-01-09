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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.custom.s3.transfer.UploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SdkInternalApi
public final class SinglePartUploadContext {

    private final UploadRequest uploadRequest;
    private final PutObjectRequest putObjectRequest;

    private SinglePartUploadContext(Builder builder) {
        this.uploadRequest = builder.uploadRequest;
        this.putObjectRequest = builder.putObjectRequest;
    }

    public UploadRequest uploadRequest() {
        return uploadRequest;
    }

    public PutObjectRequest putObjectRequest() {
        return putObjectRequest;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UploadRequest uploadRequest;

        private PutObjectRequest putObjectRequest;

        private Builder() {
        }

        public Builder uploadRequest(UploadRequest uploadRequest) {
            this.uploadRequest = uploadRequest;
            return this;
        }

        public Builder putObjectRequest(PutObjectRequest request) {
            this.putObjectRequest = request;
            return this;
        }

        public SinglePartUploadContext build() {
            return new SinglePartUploadContext(this);
        }
    }
}
