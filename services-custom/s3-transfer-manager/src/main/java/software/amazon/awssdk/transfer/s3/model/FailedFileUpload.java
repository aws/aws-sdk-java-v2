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

package software.amazon.awssdk.transfer.s3.model;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Represents a failed single file upload from {@link S3TransferManager#uploadDirectory}. It
 * has a detailed description of the result.
 */
@SdkPublicApi
public final class FailedFileUpload
    implements FailedObjectTransfer,
               ToCopyableBuilder<FailedFileUpload.Builder, FailedFileUpload> {
    
    private final UploadFileRequest request;
    private final Throwable exception;

    private FailedFileUpload(DefaultBuilder builder) {
        this.exception = Validate.paramNotNull(builder.exception, "exception");
        this.request = Validate.paramNotNull(builder.request, "request");
    }

    @Override
    public Throwable exception() {
        return exception;
    }

    @Override
    public UploadFileRequest request() {
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

        if (!Objects.equals(request, that.request)) {
            return false;
        }
        return Objects.equals(exception, that.exception);
    }

    @Override
    public int hashCode() {
        int result = request != null ? request.hashCode() : 0;
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("FailedFileUpload")
                       .add("request", request)
                       .add("exception", exception)
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

    public interface Builder extends CopyableBuilder<Builder, FailedFileUpload> {

        Builder exception(Throwable exception);

        Builder request(UploadFileRequest request);
    }

    private static final class DefaultBuilder implements Builder {
        private UploadFileRequest request;
        private Throwable exception;

        private DefaultBuilder(FailedFileUpload failedFileUpload) {
            this.request = failedFileUpload.request;
            this.exception = failedFileUpload.exception;
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
        public Builder request(UploadFileRequest request) {
            this.request = request;
            return this;
        }

        public void setRequest(UploadFileRequest request) {
            request(request);
        }

        public UploadFileRequest getRequest() {
            return request;
        }

        @Override
        public FailedFileUpload build() {
            return new FailedFileUpload(this);
        }
    }
}
