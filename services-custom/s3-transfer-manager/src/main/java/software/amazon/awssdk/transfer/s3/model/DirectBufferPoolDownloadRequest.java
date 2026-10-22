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
 * Download an object identified by the bucket and key from S3 as a stream delivered through the direct buffer pool of a
 * CRT-based S3 client.
 *
 * @see S3TransferManager#downloadWithDirectBufferPool(DirectBufferPoolDownloadRequest)
 */
@SdkPublicApi
public final class DirectBufferPoolDownloadRequest
    implements TransferObjectRequest,
               ToCopyableBuilder<DirectBufferPoolDownloadRequest.Builder, DirectBufferPoolDownloadRequest> {

    private final GetObjectRequest getObjectRequest;
    private final List<TransferListener> transferListeners;

    private DirectBufferPoolDownloadRequest(DefaultBuilder builder) {
        this.getObjectRequest = Validate.paramNotNull(builder.getObjectRequest, "getObjectRequest");
        this.transferListeners = builder.transferListeners;
    }

    /**
     * Creates a builder that can be used to create a {@link DirectBufferPoolDownloadRequest}.
     *
     * @see S3TransferManager#downloadWithDirectBufferPool(DirectBufferPoolDownloadRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    /**
     * @return The {@link GetObjectRequest} request that should be used for the download
     */
    public GetObjectRequest getObjectRequest() {
        return getObjectRequest;
    }

    /**
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

        DirectBufferPoolDownloadRequest that = (DirectBufferPoolDownloadRequest) o;

        if (!Objects.equals(getObjectRequest, that.getObjectRequest)) {
            return false;
        }
        return Objects.equals(transferListeners, that.transferListeners);
    }

    @Override
    public int hashCode() {
        int result = getObjectRequest != null ? getObjectRequest.hashCode() : 0;
        result = 31 * result + (transferListeners != null ? transferListeners.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DirectBufferPoolDownloadRequest")
                       .add("getObjectRequest", getObjectRequest)
                       .add("transferListeners", transferListeners)
                       .build();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    /**
     * A builder for a {@link DirectBufferPoolDownloadRequest}, created with {@link #builder()}
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, DirectBufferPoolDownloadRequest> {

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
         * transferListeners that have already been set.
         *
         * @param transferListeners the collection of transferListeners
         * @return Returns a reference to this object so that method calls can be chained together.
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
        private GetObjectRequest getObjectRequest;
        private List<TransferListener> transferListeners;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DirectBufferPoolDownloadRequest request) {
            this.getObjectRequest = request.getObjectRequest;
            this.transferListeners = request.transferListeners;
        }

        @Override
        public Builder getObjectRequest(GetObjectRequest getObjectRequest) {
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
        public Builder transferListeners(Collection<TransferListener> transferListeners) {
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
        public DirectBufferPoolDownloadRequest build() {
            return new DirectBufferPoolDownloadRequest(this);
        }
    }
}
