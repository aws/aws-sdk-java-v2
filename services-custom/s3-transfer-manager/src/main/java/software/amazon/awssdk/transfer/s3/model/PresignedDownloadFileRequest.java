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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Download an object using a pre-signed URL to a local file. For non-file-based downloads, you may use {@link
 * PresignedDownloadRequest} instead.
 *
 * @see S3TransferManager#downloadFileWithPresignedUrl(PresignedDownloadFileRequest)
 */
@SdkPublicApi
public final class PresignedDownloadFileRequest
    implements TransferObjectRequest, ToCopyableBuilder<PresignedDownloadFileRequest.Builder, PresignedDownloadFileRequest> {

    private final Path destination;
    private final PresignedUrlDownloadRequest presignedUrlDownloadRequest;
    private final List<TransferListener> transferListeners;

    private PresignedDownloadFileRequest(DefaultBuilder builder) {
        this.destination = Validate.paramNotNull(builder.destination, "destination");
        this.presignedUrlDownloadRequest = Validate.paramNotNull(builder.presignedUrlDownloadRequest,
                                                                        "presignedUrlDownloadRequest");
        this.transferListeners = builder.transferListeners;
    }

    /**
     * Creates a builder that can be used to create a {@link PresignedDownloadFileRequest}.
     *
     * @see S3TransferManager#downloadFileWithPresignedUrl(PresignedDownloadFileRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }
    
    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    /**
     * The {@link Path} to file that response contents will be written to. The file must not exist or this method
     * will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *
     * @return the destination path
     */
    public Path destination() {
        return destination;
    }

    /**
     * @return The {@link PresignedUrlDownloadRequest} request that should be used for the download
     */
    public PresignedUrlDownloadRequest presignedUrlDownloadRequest() {
        return presignedUrlDownloadRequest;
    }

    /**
     *
     * @return List of {@link TransferListener}s that will be notified as part of this request.
     */
    @Override
    public List<TransferListener> transferListeners() {
        return transferListeners;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PresignedDownloadFileRequest that = (PresignedDownloadFileRequest) o;

        if (!Objects.equals(destination, that.destination)) {
            return false;
        }
        if (!Objects.equals(presignedUrlDownloadRequest, that.presignedUrlDownloadRequest)) {
            return false;
        }
        return Objects.equals(transferListeners, that.transferListeners);
    }

    @Override
    public int hashCode() {
        int result = destination != null ? destination.hashCode() : 0;
        result = 31 * result + (presignedUrlDownloadRequest != null ? presignedUrlDownloadRequest.hashCode() : 0);
        result = 31 * result + (transferListeners != null ? transferListeners.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("PresignedDownloadFileRequest")
                       .add("destination", destination)
                       .add("presignedUrlDownloadRequest", presignedUrlDownloadRequest)
                       .add("transferListeners", transferListeners)
                       .build();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    /**
     * A builder for a {@link PresignedDownloadFileRequest}, created with {@link #builder()}
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, PresignedDownloadFileRequest> {

        /**
         * The {@link Path} to file that response contents will be written to. The file must not exist or this method
         * will throw an exception. If the file is not writable by the current user then an exception will be thrown.
         *
         * @param destination the destination path
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder destination(Path destination);

        /**
         * The file that response contents will be written to. The file must not exist or this method
         * will throw an exception. If the file is not writable by the current user then an exception will be thrown.
         *
         * @param destination the destination path
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        default Builder destination(File destination) {
            Validate.paramNotNull(destination, "destination");
            return destination(destination.toPath());
        }

        /**
         * The {@link PresignedUrlDownloadRequest} request that should be used for the download
         *
         * @param presignedUrlDownloadRequest the presigned URL download request
         * @return a reference to this object so that method calls can be chained together.
         * @see #presignedUrlDownloadRequest(Consumer)
         */
        Builder presignedUrlDownloadRequest(PresignedUrlDownloadRequest presignedUrlDownloadRequest);

        /**
         * The {@link PresignedUrlDownloadRequest} request that should be used for the download
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link PresignedUrlDownloadRequest} builder avoiding the
         * need to create one manually via {@link PresignedUrlDownloadRequest#builder()}.
         *
         * @param presignedUrlDownloadRequestBuilder the presigned URL download request
         * @return a reference to this object so that method calls can be chained together.
         * @see #presignedUrlDownloadRequest(PresignedUrlDownloadRequest)
         */
        default Builder presignedUrlDownloadRequest(
                Consumer<PresignedUrlDownloadRequest.Builder> presignedUrlDownloadRequestBuilder) {
            PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                             .applyMutation(presignedUrlDownloadRequestBuilder)
                                                                             .build();
            presignedUrlDownloadRequest(request);
            return this;
        }

        /**
         * The {@link TransferListener}s that will be notified as part of this request. This method overrides and replaces any
         * transferListeners that have already been set. Add an optional request override configuration.
         *
         * @param transferListeners     the collection of transferListeners
         * @return Returns a reference to this object so that method calls can be chained together.
         * @return This builder for method chaining.
         * @see TransferListener
         */
        Builder transferListeners(Collection<TransferListener> transferListeners);

        /**
         * Add a {@link TransferListener} that will be notified as part of this request.
         *
         * @param transferListener the transferListener to add
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TransferListener
         */
        Builder addTransferListener(TransferListener transferListener);

    }

    private static final class DefaultBuilder implements Builder {
        private Path destination;
        private PresignedUrlDownloadRequest presignedUrlDownloadRequest;
        private List<TransferListener> transferListeners;

        private DefaultBuilder() {
        }

        private DefaultBuilder(PresignedDownloadFileRequest presignedDownloadFileRequest) {
            this.destination = presignedDownloadFileRequest.destination;
            this.presignedUrlDownloadRequest = presignedDownloadFileRequest.presignedUrlDownloadRequest;
            this.transferListeners = presignedDownloadFileRequest.transferListeners;
        }

        @Override
        public Builder destination(Path destination) {
            this.destination = Validate.paramNotNull(destination, "destination");
            return this;
        }

        public Path getDestination() {
            return destination;
        }

        public void setDestination(Path destination) {
            destination(destination);
        }

        @Override
        public DefaultBuilder presignedUrlDownloadRequest(PresignedUrlDownloadRequest presignedUrlDownloadRequest) {
            this.presignedUrlDownloadRequest = presignedUrlDownloadRequest;
            return this;
        }

        public PresignedUrlDownloadRequest getPresignedUrlDownloadRequest() {
            return presignedUrlDownloadRequest;
        }

        public void setPresignedUrlDownloadRequest(PresignedUrlDownloadRequest presignedUrlDownloadRequest) {
            presignedUrlDownloadRequest(presignedUrlDownloadRequest);
        }

        @Override
        public DefaultBuilder transferListeners(Collection<TransferListener> transferListeners) {
            this.transferListeners = transferListeners != null ? new ArrayList<>(transferListeners) : null;
            return this;
        }

        @Override
        public Builder addTransferListener(TransferListener transferListener) {
            if (transferListeners == null) {
                transferListeners = new ArrayList<>();
            }
            transferListeners.add(transferListener);
            return this;
        }

        public List<TransferListener> getTransferListeners() {
            return transferListeners;
        }

        public void setTransferListeners(Collection<TransferListener> transferListeners) {
            transferListeners(transferListeners);
        }

        @Override
        public PresignedDownloadFileRequest build() {
            return new PresignedDownloadFileRequest(this);
        }
    }
}