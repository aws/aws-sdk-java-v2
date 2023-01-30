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
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Upload the given {@link AsyncRequestBody} to an object in S3. For file-based uploads, you may use {@link UploadFileRequest}
 * instead.
 *
 * @see S3TransferManager#upload(UploadRequest)
 */
@SdkPublicApi
public final class UploadRequest
    implements TransferObjectRequest,
               ToCopyableBuilder<UploadRequest.Builder, UploadRequest> {

    private final PutObjectRequest putObjectRequest;
    private final AsyncRequestBody requestBody;
    private final List<TransferListener> listeners;

    private UploadRequest(DefaultBuilder builder) {
        this.putObjectRequest = paramNotNull(builder.putObjectRequest, "putObjectRequest");
        this.requestBody = paramNotNull(builder.requestBody, "requestBody");
        this.listeners = builder.listeners;
    }

    /**
     * @return The {@link PutObjectRequest} request that should be used for the upload
     */
    public PutObjectRequest putObjectRequest() {
        return putObjectRequest;
    }

    /**
     * The {@link AsyncRequestBody} containing data to send to the service.
     *
     * @return the request body
     */
    public AsyncRequestBody requestBody() {
        return requestBody;
    }

    /**
     * @return the List of {@link TransferListener}s that will be notified as part of this request.
     */
    @Override
    public List<TransferListener> transferListeners() {
        return listeners;
    }

    /**
     * Creates a builder that can be used to create a {@link UploadRequest}.
     *
     * @see S3TransferManager#upload(UploadRequest)
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

        UploadRequest that = (UploadRequest) o;

        if (!Objects.equals(putObjectRequest, that.putObjectRequest)) {
            return false;
        }
        if (!Objects.equals(requestBody, that.requestBody)) {
            return false;
        }
        return Objects.equals(listeners, that.listeners);
    }

    @Override
    public int hashCode() {
        int result = putObjectRequest != null ? putObjectRequest.hashCode() : 0;
        result = 31 * result + (requestBody != null ? requestBody.hashCode() : 0);
        result = 31 * result + (listeners != null ? listeners.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("UploadRequest")
                       .add("putObjectRequest", putObjectRequest)
                       .add("requestBody", requestBody)
                       .add("configuration", listeners)
                       .build();
    }

    /**
     * A builder for a {@link UploadRequest}, created with {@link #builder()}
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, UploadRequest> {

        /**
         * The {@link AsyncRequestBody} containing the data to send to the service. Request bodies may be declared using one of
         * the static factory methods in the {@link AsyncRequestBody} class.
         *
         * @param requestBody the request body
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see AsyncRequestBody
         */
        Builder requestBody(AsyncRequestBody requestBody);

        /**
         * Configure the {@link PutObjectRequest} that should be used for the upload
         *
         * @param putObjectRequest the putObjectRequest
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #putObjectRequest(Consumer)
         */
        Builder putObjectRequest(PutObjectRequest putObjectRequest);

        /**
         * Configure the {@link PutObjectRequest} that should be used for the upload
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link PutObjectRequest} builder avoiding the
         * need to create one manually via {@link PutObjectRequest#builder()}.
         *
         * @param putObjectRequestBuilder the putObjectRequest consumer builder
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #putObjectRequest(PutObjectRequest)
         */
        default Builder putObjectRequest(Consumer<PutObjectRequest.Builder> putObjectRequestBuilder) {
            return putObjectRequest(PutObjectRequest.builder()
                                                    .applyMutation(putObjectRequestBuilder)
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
         * Add a {@link TransferListener} that will be notified as part of this request.
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
        UploadRequest build();
    }

    private static class DefaultBuilder implements Builder {
        private PutObjectRequest putObjectRequest;
        private AsyncRequestBody requestBody;
        private List<TransferListener> listeners;

        private DefaultBuilder() {
        }

        private DefaultBuilder(UploadRequest uploadRequest) {
            this.putObjectRequest = uploadRequest.putObjectRequest;
            this.requestBody = uploadRequest.requestBody;
            this.listeners = uploadRequest.listeners;
        }

        @Override
        public Builder requestBody(AsyncRequestBody requestBody) {
            this.requestBody = Validate.paramNotNull(requestBody, "requestBody");
            return this;
        }

        public AsyncRequestBody getRequestBody() {
            return requestBody;
        }

        public void setRequestBody(AsyncRequestBody requestBody) {
            requestBody(requestBody);
        }

        @Override
        public Builder putObjectRequest(PutObjectRequest putObjectRequest) {
            this.putObjectRequest = putObjectRequest;
            return this;
        }

        public PutObjectRequest getPutObjectRequest() {
            return putObjectRequest;
        }

        public void setPutObjectRequest(PutObjectRequest putObjectRequest) {
            putObjectRequest(putObjectRequest);
        }

        @Override
        public Builder transferListeners(Collection<TransferListener> transferListeners) {
            this.listeners = transferListeners != null ? new ArrayList<>(transferListeners) : null;
            return this;
        }

        @Override
        public Builder addTransferListener(TransferListener transferListener) {
            if (listeners == null) {
                listeners = new ArrayList<>();
            }
            listeners.add(transferListener);
            return this;
        }

        public List<TransferListener> getListeners() {
            return listeners;
        }

        public void setListeners(Collection<TransferListener> listeners) {
            transferListeners(listeners);
        }

        @Override
        public UploadRequest build() {
            return new UploadRequest(this);
        }
    }
}
