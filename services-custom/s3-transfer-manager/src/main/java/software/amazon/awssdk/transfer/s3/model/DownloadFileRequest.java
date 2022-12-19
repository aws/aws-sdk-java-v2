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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Download an object identified by the bucket and key from S3 to a local file. For non-file-based downloads, you may use {@link
 * DownloadRequest} instead.
 *
 * @see S3TransferManager#downloadFile(DownloadFileRequest)
 */
@SdkPublicApi
public final class DownloadFileRequest
    implements TransferObjectRequest, ToCopyableBuilder<DownloadFileRequest.Builder, DownloadFileRequest> {

    private final Path destination;
    private final GetObjectRequest getObjectRequest;
    private final List<TransferListener> transferListeners;

    private DownloadFileRequest(DefaultBuilder builder) {
        this.destination = Validate.paramNotNull(builder.destination, "destination");
        this.getObjectRequest = Validate.paramNotNull(builder.getObjectRequest, "getObjectRequest");
        this.transferListeners = builder.transferListeners;
    }

    /**
     * Creates a builder that can be used to create a {@link DownloadFileRequest}.
     *
     * @see S3TransferManager#downloadFile(DownloadFileRequest)
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
     * @return The {@link GetObjectRequest} request that should be used for the download
     */
    public GetObjectRequest getObjectRequest() {
        return getObjectRequest;
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

        DownloadFileRequest that = (DownloadFileRequest) o;

        if (!Objects.equals(destination, that.destination)) {
            return false;
        }
        if (!Objects.equals(getObjectRequest, that.getObjectRequest)) {
            return false;
        }
        return Objects.equals(transferListeners, that.transferListeners);
    }

    @Override
    public int hashCode() {
        int result = destination != null ? destination.hashCode() : 0;
        result = 31 * result + (getObjectRequest != null ? getObjectRequest.hashCode() : 0);
        result = 31 * result + (transferListeners != null ? transferListeners.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DownloadFileRequest")
                       .add("destination", destination)
                       .add("getObjectRequest", getObjectRequest)
                       .add("transferListeners", transferListeners)
                       .build();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    /**
     * A builder for a {@link DownloadFileRequest}, created with {@link #builder()}
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, DownloadFileRequest> {

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
         * The {@link GetObjectRequest} request that should be used for the download
         *
         * @param getObjectRequest the getObject request
         * @return a reference to this object so that method calls can be chained together.
         * @see #getObjectRequest(Consumer)
         */
        Builder getObjectRequest(GetObjectRequest getObjectRequest);

        /**
         * The {@link GetObjectRequest} request that should be used for the download
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link GetObjectRequest} builder avoiding the
         * need to create one manually via {@link GetObjectRequest#builder()}.
         *
         * @param getObjectRequestBuilder the getObject request
         * @return a reference to this object so that method calls can be chained together.
         * @see #getObjectRequest(GetObjectRequest)
         */
        default Builder getObjectRequest(Consumer<GetObjectRequest.Builder> getObjectRequestBuilder) {
            GetObjectRequest request = GetObjectRequest.builder()
                                                       .applyMutation(getObjectRequestBuilder)
                                                       .build();
            getObjectRequest(request);
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
        private GetObjectRequest getObjectRequest;
        private List<TransferListener> transferListeners;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DownloadFileRequest downloadFileRequest) {
            this.destination = downloadFileRequest.destination;
            this.getObjectRequest = downloadFileRequest.getObjectRequest;
            this.transferListeners = downloadFileRequest.transferListeners;
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
        public DefaultBuilder getObjectRequest(GetObjectRequest getObjectRequest) {
            this.getObjectRequest = getObjectRequest;
            return this;
        }

        public GetObjectRequest getGetObjectRequest() {
            return getObjectRequest;
        }

        public void setGetObjectRequest(GetObjectRequest getObjectRequest) {
            getObjectRequest(getObjectRequest);
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
        public DownloadFileRequest build() {
            return new DownloadFileRequest(this);
        }
    }
}
