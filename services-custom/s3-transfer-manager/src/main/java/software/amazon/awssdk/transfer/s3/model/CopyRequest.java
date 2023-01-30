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

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Creates a copy of an object that is already stored in S3.
 *
 * @see S3TransferManager#copy(CopyRequest)
 */
@SdkPublicApi
public final class CopyRequest
    implements TransferObjectRequest,
               ToCopyableBuilder<CopyRequest.Builder, CopyRequest> {

    private final CopyObjectRequest copyObjectRequest;
    private final List<TransferListener> transferListeners;

    private CopyRequest(DefaultBuilder builder) {
        this.copyObjectRequest = paramNotNull(builder.copyObjectRequest, "copyObjectRequest");
        this.transferListeners = builder.transferListeners;
    }

    /**
     * @return The {@link CopyObjectRequest} request that should be used for the copy
     */
    public CopyObjectRequest copyObjectRequest() {
        return copyObjectRequest;
    }

    /**
     * @return the List of transferListener.
     * @see Builder#transferListeners(Collection)
     */
    @Override
    public List<TransferListener> transferListeners() {
        return transferListeners;
    }

    /**
     * Creates a builder that can be used to create a {@link CopyRequest}.
     *
     * @see S3TransferManager#copy(CopyRequest)
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CopyRequest that = (CopyRequest) o;

        if (!Objects.equals(copyObjectRequest, that.copyObjectRequest)) {
            return false;
        }
        return Objects.equals(transferListeners, that.transferListeners);
    }

    @Override
    public int hashCode() {
        int result = copyObjectRequest != null ? copyObjectRequest.hashCode() : 0;
        result = 31 * result + (transferListeners != null ? transferListeners.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("CopyRequest")
                       .add("copyRequest", copyObjectRequest)
                       .add("transferListeners", transferListeners)
                       .build();
    }

    /**
     * A builder for a {@link CopyRequest}, created with {@link #builder()}
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, CopyRequest> {

        /**
         * Configures the {@link CopyRequest} that should be used for the copy
         *
         * @param copyRequest the copyRequest
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #copyObjectRequest(Consumer)
         */
        Builder copyObjectRequest(CopyObjectRequest copyRequest);

        /**
         * Configures the {@link CopyRequest} that should be used for the copy
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link CopyRequest} builder avoiding the need to create
         * one manually via {@link CopyRequest#builder()}.
         *
         * @param copyRequestBuilder the copyRequest consumer builder
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #copyObjectRequest(CopyObjectRequest) 
         */
        default Builder copyObjectRequest(Consumer<CopyObjectRequest.Builder> copyRequestBuilder) {
            return copyObjectRequest(CopyObjectRequest.builder()
                                                      .applyMutation(copyRequestBuilder)
                                                      .build());
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
         * Adds a {@link TransferListener} that will be notified as part of this request.
         *
         * @param transferListener the transferListener to add
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TransferListener
         */
        Builder addTransferListener(TransferListener transferListener);

        /**
         * @return The built request.
         */
        @Override
        CopyRequest build();
    }

    private static class DefaultBuilder implements Builder {
        private CopyObjectRequest copyObjectRequest;
        private List<TransferListener> transferListeners;

        private DefaultBuilder() {
        }

        private DefaultBuilder(CopyRequest copyRequest) {
            this.copyObjectRequest = copyRequest.copyObjectRequest;
            this.transferListeners = copyRequest.transferListeners;
        }

        @Override
        public Builder copyObjectRequest(CopyObjectRequest copyRequest) {
            this.copyObjectRequest = copyRequest;
            return this;
        }

        public CopyObjectRequest getCopyObjectRequest() {
            return copyObjectRequest;
        }

        public void setCopyObjectRequest(CopyObjectRequest copyObjectRequest) {
            copyObjectRequest(copyObjectRequest);
        }

        @Override
        public DefaultBuilder transferListeners(Collection<TransferListener> transferListeners) {
            this.transferListeners = transferListeners != null ? new ArrayList<>(transferListeners) : null;
            return this;
        }

        @Override
        public DefaultBuilder addTransferListener(TransferListener transferListener) {
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
        public CopyRequest build() {
            return new CopyRequest(this);
        }
    }
}
