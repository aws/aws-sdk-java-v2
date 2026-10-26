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
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DirectBufferPoolDownloadRequest.TypedBuilder;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Represents the request to download an object identified by the bucket and key from S3 through the direct buffer pool of a
 * CRT-based S3 client, using a direct buffer pool response transformer such as
 * {@link software.amazon.awssdk.services.s3.crt.S3CrtResponseTransformerFactory#toBlockingInputStreamFromDirectBufferPool()}.
 *
 * @see S3TransferManager#downloadWithDirectBufferPool(DirectBufferPoolDownloadRequest)
 */
@SdkPublicApi
public final class DirectBufferPoolDownloadRequest<ReturnT>
    implements TransferObjectRequest,
               ToCopyableBuilder<TypedBuilder<ReturnT>, DirectBufferPoolDownloadRequest<ReturnT>> {

    private final AsyncResponseTransformer<GetObjectResponse, ReturnT> responseTransformer;
    private final GetObjectRequest getObjectRequest;
    private final List<TransferListener> transferListeners;

    private DirectBufferPoolDownloadRequest(DefaultTypedBuilder<ReturnT> builder) {
        this.responseTransformer = Validate.paramNotNull(builder.responseTransformer, "responseTransformer");
        this.getObjectRequest = Validate.paramNotNull(builder.getObjectRequest, "getObjectRequest");
        this.transferListeners = builder.transferListeners;
    }

    /**
     * Creates a builder that can be used to create a {@link DirectBufferPoolDownloadRequest}.
     *
     * @see UntypedBuilder
     */
    public static UntypedBuilder builder() {
        return new DefaultUntypedBuilder();
    }

    @Override
    public TypedBuilder<ReturnT> toBuilder() {
        return new DefaultTypedBuilder<>(this);
    }

    /**
     * @return The direct buffer pool {@link AsyncResponseTransformer} that should be used for the download
     */
    public AsyncResponseTransformer<GetObjectResponse, ReturnT> responseTransformer() {
        return responseTransformer;
    }

    /**
     * @return The {@link GetObjectRequest} request that should be used for the download
     */
    public GetObjectRequest getObjectRequest() {
        return getObjectRequest;
    }

    /**
     * @return the List of transferListeners.
     * @see TypedBuilder#transferListeners(Collection)
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

        DirectBufferPoolDownloadRequest<?> that = (DirectBufferPoolDownloadRequest<?>) o;

        if (!Objects.equals(responseTransformer, that.responseTransformer)) {
            return false;
        }
        if (!Objects.equals(getObjectRequest, that.getObjectRequest)) {
            return false;
        }
        return Objects.equals(transferListeners, that.transferListeners);
    }

    @Override
    public int hashCode() {
        int result = responseTransformer != null ? responseTransformer.hashCode() : 0;
        result = 31 * result + (getObjectRequest != null ? getObjectRequest.hashCode() : 0);
        result = 31 * result + (transferListeners != null ? transferListeners.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DirectBufferPoolDownloadRequest")
                       .add("responseTransformer", responseTransformer)
                       .add("getObjectRequest", getObjectRequest)
                       .add("transferListeners", transferListeners)
                       .build();
    }

    /**
     * Initial calls to {@link DirectBufferPoolDownloadRequest#builder()} return an {@link UntypedBuilder}, where the builder
     * is not yet parameterized with the generic type associated with {@link DirectBufferPoolDownloadRequest}. The type is
     * inferred as part of specifying the {@link #responseTransformer(AsyncResponseTransformer)} parameter, at which point the
     * builder chain will return a new {@link TypedBuilder}.
     */
    @NotThreadSafe
    public interface UntypedBuilder {

        /**
         * The {@link GetObjectRequest} request that should be used for the download
         *
         * @param getObjectRequest the getObject request
         * @return a reference to this object so that method calls can be chained together.
         * @see #getObjectRequest(Consumer)
         */
        UntypedBuilder getObjectRequest(GetObjectRequest getObjectRequest);

        /**
         * The {@link GetObjectRequest} request that should be used for the download
         * <p>
         * This is a convenience method that creates an instance of the {@link GetObjectRequest} builder, avoiding the need to
         * create one manually via {@link GetObjectRequest#builder()}.
         *
         * @param getObjectRequestBuilder the getObject request
         * @return a reference to this object so that method calls can be chained together.
         * @see #getObjectRequest(GetObjectRequest)
         */
        default UntypedBuilder getObjectRequest(Consumer<GetObjectRequest.Builder> getObjectRequestBuilder) {
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
        UntypedBuilder transferListeners(Collection<TransferListener> transferListeners);

        /**
         * Adds a {@link TransferListener} that will be notified as part of this request.
         *
         * @param transferListener the transferListener to add
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TransferListener
         */
        UntypedBuilder addTransferListener(TransferListener transferListener);

        /**
         * Specifies the direct buffer pool {@link AsyncResponseTransformer} that should be used for the download. This method
         * also infers the generic type of {@link DirectBufferPoolDownloadRequest} to create from the second type parameter of
         * the provided {@link AsyncResponseTransformer}.
         *
         * @param responseTransformer the AsyncResponseTransformer
         * @param <T>                 the type of {@link DirectBufferPoolDownloadRequest} to create
         * @return a reference to this object so that method calls can be chained together.
         */
        <T> TypedBuilder<T> responseTransformer(AsyncResponseTransformer<GetObjectResponse, T> responseTransformer);
    }

    private static final class DefaultUntypedBuilder implements UntypedBuilder {
        private GetObjectRequest getObjectRequest;
        private List<TransferListener> transferListeners;

        private DefaultUntypedBuilder() {
        }

        @Override
        public UntypedBuilder getObjectRequest(GetObjectRequest getObjectRequest) {
            this.getObjectRequest = getObjectRequest;
            return this;
        }

        @Override
        public UntypedBuilder transferListeners(Collection<TransferListener> transferListeners) {
            this.transferListeners = transferListeners != null ? new ArrayList<>(transferListeners) : null;
            return this;
        }

        @Override
        public UntypedBuilder addTransferListener(TransferListener transferListener) {
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
        public <T> TypedBuilder<T> responseTransformer(AsyncResponseTransformer<GetObjectResponse, T> responseTransformer) {
            return new DefaultTypedBuilder<T>()
                .getObjectRequest(getObjectRequest)
                .transferListeners(transferListeners)
                .responseTransformer(responseTransformer);
        }
    }

    /**
     * The type-parameterized version of {@link UntypedBuilder}. This builder's type is inferred as part of specifying
     * {@link UntypedBuilder#responseTransformer(AsyncResponseTransformer)}, after which this builder can be used to construct a
     * {@link DirectBufferPoolDownloadRequest} with the same generic type.
     */
    @NotThreadSafe
    public interface TypedBuilder<T> extends CopyableBuilder<TypedBuilder<T>, DirectBufferPoolDownloadRequest<T>> {

        /**
         * The {@link GetObjectRequest} request that should be used for the download
         *
         * @param getObjectRequest the getObject request
         * @return a reference to this object so that method calls can be chained together.
         * @see #getObjectRequest(Consumer)
         */
        TypedBuilder<T> getObjectRequest(GetObjectRequest getObjectRequest);

        /**
         * The {@link GetObjectRequest} request that should be used for the download
         * <p>
         * This is a convenience method that creates an instance of the {@link GetObjectRequest} builder, avoiding the need to
         * create one manually via {@link GetObjectRequest#builder()}.
         *
         * @param getObjectRequestBuilder the getObject request
         * @return a reference to this object so that method calls can be chained together.
         * @see #getObjectRequest(GetObjectRequest)
         */
        default TypedBuilder<T> getObjectRequest(Consumer<GetObjectRequest.Builder> getObjectRequestBuilder) {
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
        TypedBuilder<T> transferListeners(Collection<TransferListener> transferListeners);

        /**
         * Add a {@link TransferListener} that will be notified as part of this request.
         *
         * @param transferListener the transferListener to add
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TransferListener
         */
        TypedBuilder<T> addTransferListener(TransferListener transferListener);

        /**
         * Specifies the direct buffer pool {@link AsyncResponseTransformer} that should be used for the download. The generic
         * type used is constrained by the {@link UntypedBuilder#responseTransformer(AsyncResponseTransformer)} that was
         * previously used to create this {@link TypedBuilder}.
         *
         * @param responseTransformer the AsyncResponseTransformer
         * @return a reference to this object so that method calls can be chained together.
         */
        TypedBuilder<T> responseTransformer(AsyncResponseTransformer<GetObjectResponse, T> responseTransformer);
    }

    private static final class DefaultTypedBuilder<T> implements TypedBuilder<T> {
        private GetObjectRequest getObjectRequest;
        private List<TransferListener> transferListeners;
        private AsyncResponseTransformer<GetObjectResponse, T> responseTransformer;

        private DefaultTypedBuilder() {
        }

        private DefaultTypedBuilder(DirectBufferPoolDownloadRequest<T> request) {
            this.getObjectRequest = request.getObjectRequest;
            this.responseTransformer = request.responseTransformer;
            this.transferListeners = request.transferListeners;
        }

        @Override
        public TypedBuilder<T> getObjectRequest(GetObjectRequest getObjectRequest) {
            this.getObjectRequest = getObjectRequest;
            return this;
        }

        @Override
        public TypedBuilder<T> responseTransformer(AsyncResponseTransformer<GetObjectResponse, T> responseTransformer) {
            this.responseTransformer = responseTransformer;
            return this;
        }

        @Override
        public TypedBuilder<T> transferListeners(Collection<TransferListener> transferListeners) {
            this.transferListeners = transferListeners != null ? new ArrayList<>(transferListeners) : null;
            return this;
        }

        @Override
        public TypedBuilder<T> addTransferListener(TransferListener transferListener) {
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
        public DirectBufferPoolDownloadRequest<T> build() {
            return new DirectBufferPoolDownloadRequest<>(this);
        }
    }
}
