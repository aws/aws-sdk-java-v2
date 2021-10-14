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

package software.amazon.awssdk.transfer.s3;

import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Represents a failed single file upload from {@link S3TransferManager#uploadDirectory}. It
 * has detailed description of the result
 */
@SdkPublicApi
@SdkPreviewApi
public final class FailedFileUpload implements FailedFileTransfer<UploadRequest>,
                                               ToCopyableBuilder<FailedFileUpload.Builder,
                                                   FailedFileUpload> {
    private final Throwable exception;
    private final UploadRequest request;

    FailedFileUpload(DefaultBuilder builder) {
        this.exception = Validate.paramNotNull(builder.exception, "exception");
        this.request = Validate.paramNotNull(builder.request, "request");
    }

    @Override
    public Throwable exception() {
        return exception;
    }

    @Override
    public UploadRequest request() {
        return request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FailedFileUpload that = (FailedFileUpload) o;

        if (!exception.equals(that.exception)) {
            return false;
        }
        return request.equals(that.request);
    }

    @Override
    public int hashCode() {
        int result = exception.hashCode();
        result = 31 * result + request.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("FailedUpload")
                       .add("exception", exception)
                       .add("request", request)
                       .build();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<Builder, FailedFileUpload>,
                                     FailedFileTransfer.Builder<UploadRequest> {

        @Override
        Builder exception(Throwable exception);

        @Override
        Builder request(UploadRequest request);

        @Override
        FailedFileUpload build();
    }

    private static final class DefaultBuilder implements Builder {
        private UploadRequest request;
        private Throwable exception;

        private DefaultBuilder(FailedFileUpload failedSingleFileUpload) {
            this.request = failedSingleFileUpload.request;
            this.exception = failedSingleFileUpload.exception;
        }

        private DefaultBuilder() {
        }

        @Override
        public Builder exception(Throwable exception) {
            this.exception = exception;
            return this;
        }

        public void setException(Throwable exception) {
            exception(exception);
        }

        public Throwable getException() {
            return exception;
        }

        @Override
        public Builder request(UploadRequest request) {
            this.request = request;
            return this;
        }

        public void setRequest(UploadRequest request) {
            request(request);
        }

        public UploadRequest getRequest() {
            return request;
        }

        @Override
        public FailedFileUpload build() {
            return new FailedFileUpload(this);
        }
    }
}
