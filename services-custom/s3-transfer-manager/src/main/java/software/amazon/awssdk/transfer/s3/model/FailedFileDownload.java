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
 * Represents a failed single file download from {@link S3TransferManager#downloadDirectory(DownloadDirectoryRequest)}. It
 * has a detailed description of the result.
 */
@SdkPublicApi
public final class FailedFileDownload
    implements FailedObjectTransfer,
               ToCopyableBuilder<FailedFileDownload.Builder, FailedFileDownload> {

    private final DownloadFileRequest request;
    private final Throwable exception;

    private FailedFileDownload(DefaultBuilder builder) {
        this.exception = Validate.paramNotNull(builder.exception, "exception");
        this.request = Validate.paramNotNull(builder.request, "request");
    }

    @Override
    public Throwable exception() {
        return exception;
    }

    /**
     * The request for the failed download.
     *
     * <p>
     * If the object's key can't be mapped to a path inside the destination directory (for example, its relative path
     * resolves outside the destination directory, or it isn't a valid path on the destination's file system), nothing is
     * downloaded for it, and {@link #exception()} names the key and the reason. In that case,
     * {@code request().destination()} is a placeholder path under the destination directory that does not exist and is
     * not written by the SDK. This request isn't meant to be retried as is, and the destination directory must not be deleted to
     * clean up after it. The object's key is available from {@code request().getObjectRequest().key()}.
     */
    @Override
    public DownloadFileRequest request() {
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

        FailedFileDownload that = (FailedFileDownload) o;

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
        return ToString.builder("FailedFileDownload")
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

    public interface Builder extends CopyableBuilder<Builder, FailedFileDownload> {

        Builder exception(Throwable exception);

        Builder request(DownloadFileRequest request);
    }

    private static final class DefaultBuilder implements Builder {
        private DownloadFileRequest request;
        private Throwable exception;

        private DefaultBuilder(FailedFileDownload failedFileDownload) {
            this.request = failedFileDownload.request;
            this.exception = failedFileDownload.exception;
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
        public Builder request(DownloadFileRequest request) {
            this.request = request;
            return this;
        }

        public void setRequest(DownloadFileRequest request) {
            request(request);
        }

        public DownloadFileRequest getRequest() {
            return request;
        }

        @Override
        public FailedFileDownload build() {
            return new FailedFileDownload(this);
        }
    }
}
