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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.PresignedDownloadRequest.TypedBuilder;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Represents the request to download an object using a pre-signed URL through the given
 * {@link AsyncResponseTransformer}. For downloading to a file,
 * you may use {@link PresignedDownloadFileRequest} instead.
 *
 * @see S3TransferManager#downloadWithPresignedUrl(PresignedDownloadRequest)
 */
@SdkPublicApi
public final class PresignedDownloadRequest<ReturnT>
    implements TransferObjectRequest,
               ToCopyableBuilder<TypedBuilder<ReturnT>, PresignedDownloadRequest<ReturnT>> {
    
    private final AsyncResponseTransformer<GetObjectResponse, ReturnT> responseTransformer;
    private final PresignedUrlDownloadRequest presignedUrlDownloadRequest;
    private final List<TransferListener> transferListeners;

    private PresignedDownloadRequest(DefaultTypedBuilder<ReturnT> builder) {
        this.responseTransformer = Validate.paramNotNull(builder.responseTransformer, "responseTransformer");
        this.presignedUrlDownloadRequest = Validate.paramNotNull(builder.presignedUrlDownloadRequest,
                                                                        "presignedUrlDownloadRequest");
        this.transferListeners = builder.transferListeners;
    }

    /**
     * Creates a builder that can be used to create a {@link PresignedDownloadRequest}.
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
     * The {@link AsyncResponseTransformer} that response contents will be written to.
     *
     * @return the response transformer
     */
    public AsyncResponseTransformer<GetObjectResponse, ReturnT> responseTransformer() {
        return responseTransformer;
    }

    /**
     * @return The {@link PresignedUrlDownloadRequest} request that should be used for the download
     */
    public PresignedUrlDownloadRequest presignedUrlDownloadRequest() {
        return presignedUrlDownloadRequest;
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

        PresignedDownloadRequest<?> that = (PresignedDownloadRequest<?>) o;

        if (!Objects.equals(responseTransformer, that.responseTransformer)) {
            return false;
        }
        if (!Objects.equals(presignedUrlDownloadRequest, that.presignedUrlDownloadRequest)) {
            return false;
        }
        return Objects.equals(transferListeners, that.transferListeners);
    }

    @Override
    public int hashCode() {
        int result = responseTransformer != null ? responseTransformer.hashCode() : 0;
        result = 31 * result + (presignedUrlDownloadRequest != null ? presignedUrlDownloadRequest.hashCode() : 0);
        result = 31 * result + (transferListeners != null ? transferListeners.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("PresignedDownloadRequest")
                       .add("responseTransformer", responseTransformer)
                       .add("presignedUrlDownloadRequest", presignedUrlDownloadRequest)
                       .add("transferListeners", transferListeners)
                       .build();
    }

    /**
     * Initial calls to {@link PresignedDownloadRequest#builder()} return an {@link UntypedBuilder}, where the builder is not yet
     * parameterized with the generic type associated with {@link PresignedDownloadRequest}.
     * This prevents the otherwise awkward syntax of having to explicitly cast the builder type, e.g.,
     * <pre>
     * {@code PresignedDownloadRequest.<ResponseBytes<GetObjectResponse>>builder()}
     * </pre>
     * Instead, the type may be inferred as part of specifying the {@link #responseTransformer(AsyncResponseTransformer)}
     * parameter, at which point the builder chain will return a new {@link TypedBuilder}.
     */
    public interface UntypedBuilder {

        /**
         * The {@link PresignedUrlDownloadRequest} request that should be used for the download
         *
         * @param presignedUrlDownloadRequest the presigned URL download request
         * @return a reference to this object so that method calls can be chained together.
         * @see #presignedUrlDownloadRequest(Consumer)
         */
        UntypedBuilder presignedUrlDownloadRequest(PresignedUrlDownloadRequest presignedUrlDownloadRequest);

        /**
         * The {@link PresignedUrlDownloadRequest} request that should be used for the download
         * <p>
         * This is a convenience method that creates an instance of the {@link PresignedUrlDownloadRequest}
         * builder, avoiding the need to create one manually via {@link PresignedUrlDownloadRequest#builder()}.
         *
         * @param presignedUrlDownloadRequestBuilder the presigned URL download request
         * @return a reference to this object so that method calls can be chained together.
         * @see #presignedUrlDownloadRequest(PresignedUrlDownloadRequest)
         */
        default UntypedBuilder presignedUrlDownloadRequest(
                Consumer<PresignedUrlDownloadRequest.Builder> presignedUrlDownloadRequestBuilder) {
            PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                             .applyMutation(presignedUrlDownloadRequestBuilder)
                                                                             .build();
            presignedUrlDownloadRequest(request);
            return this;
        }

        /**
         * The {@link TransferListener}s that will be notified as part of this request. This method
         * overrides and replaces any transferListeners that have already been set. Add an optional
         * request override configuration.
         *
         * @param transferListeners     the collection of transferListeners
         * @return Returns a reference to this object so that method calls can be chained together.
         * @return This builder for method chaining.
         * @see TransferListener
         */
        UntypedBuilder transferListeners(
                Collection<TransferListener> transferListeners);

        /**
         * Adds a {@link TransferListener} that will be notified as part of this request.
         *
         * @param transferListener the transferListener to add
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TransferListener
         */
        UntypedBuilder addTransferListener(TransferListener transferListener);

        /**
         * Specifies the {@link AsyncResponseTransformer} that should be used for the download. This
         * method also infers the generic type of {@link PresignedDownloadRequest} to create,
         * inferred from the second type parameter of the provided {@link AsyncResponseTransformer}.
         * E.g, specifying {@link AsyncResponseTransformer#toBytes()} would result in inferring the
         * type of the {@link PresignedDownloadRequest} to be of {@code ResponseBytes<GetObjectResponse>}.
         * See the static factory methods available in {@link AsyncResponseTransformer}.
         *
         * @param responseTransformer the AsyncResponseTransformer
         * @param <T>                 the type of {@link PresignedDownloadRequest} to create
         * @return a reference to this object so that method calls can be chained together.
         * @see AsyncResponseTransformer
         */
        <T> TypedBuilder<T> responseTransformer(
                AsyncResponseTransformer<GetObjectResponse, T> responseTransformer);
    }

    private static final class DefaultUntypedBuilder implements UntypedBuilder {
        private PresignedUrlDownloadRequest presignedUrlDownloadRequest;
        private List<TransferListener> transferListeners;

        private DefaultUntypedBuilder() {
        }

        @Override
        public UntypedBuilder presignedUrlDownloadRequest(PresignedUrlDownloadRequest presignedUrlDownloadRequest) {
            this.presignedUrlDownloadRequest = presignedUrlDownloadRequest;
            return this;
        }

        @Override
        public UntypedBuilder transferListeners(
                Collection<TransferListener> transferListeners) {
            this.transferListeners = transferListeners != null ?
                    new ArrayList<>(transferListeners) : null;
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

        public void setTransferListeners(
                Collection<TransferListener> transferListeners) {
            transferListeners(transferListeners);
        }

        @Override
        public <T> TypedBuilder<T> responseTransformer(
                AsyncResponseTransformer<GetObjectResponse, T> responseTransformer) {
            return new DefaultTypedBuilder<T>()
                .presignedUrlDownloadRequest(presignedUrlDownloadRequest)
                .transferListeners(transferListeners)
                .responseTransformer(responseTransformer);
        }
    }

    /**
     * The type-parameterized version of {@link UntypedBuilder}. This builder's type is inferred as part of specifying {@link
     * UntypedBuilder#responseTransformer(AsyncResponseTransformer)}, after which this builder can be used to construct a {@link
     * PresignedDownloadRequest} with the same generic type.
     */
    public interface TypedBuilder<T> extends CopyableBuilder<TypedBuilder<T>, PresignedDownloadRequest<T>> {

        /**
         * The {@link PresignedUrlDownloadRequest} request that should be used for the download
         *
         * @param presignedUrlDownloadRequest the presigned URL download request
         * @return a reference to this object so that method calls can be chained together.
         * @see #presignedUrlDownloadRequest(Consumer)
         */
        TypedBuilder<T> presignedUrlDownloadRequest(PresignedUrlDownloadRequest presignedUrlDownloadRequest);

        /**
         * The {@link PresignedUrlDownloadRequest} request that should be used for the download
         * <p>
         * This is a convenience method that creates an instance of the {@link PresignedUrlDownloadRequest} builder,
         * avoiding the need to create one manually via {@link PresignedUrlDownloadRequest#builder()}.
         *
         * @param presignedUrlDownloadRequestBuilder the presigned URL download request
         * @return a reference to this object so that method calls can be chained together.
         * @see #presignedUrlDownloadRequest(PresignedUrlDownloadRequest)
         */
        default TypedBuilder<T> presignedUrlDownloadRequest(
                Consumer<PresignedUrlDownloadRequest.Builder> presignedUrlDownloadRequestBuilder) {
            PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                             .applyMutation(presignedUrlDownloadRequestBuilder)
                                                                             .build();
            presignedUrlDownloadRequest(request);
            return this;
        }

        /**
         * The {@link TransferListener}s that will be notified as part of this request. This method overrides and
         * replaces any transferListeners that have already been set. Add an optional request override
         * configuration.
         *
         * @param transferListeners     the collection of transferListeners
         * @return Returns a reference to this object so that method calls can be chained together.
         * @return This builder for method chaining.
         * @see TransferListener
         */
        TypedBuilder<T> transferListeners(
                Collection<TransferListener> transferListeners);

        /**
         * Add a {@link TransferListener} that will be notified as part of this request.
         *
         * @param transferListener the transferListener to add
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TransferListener
         */
        TypedBuilder<T> addTransferListener(TransferListener transferListener);

        /**
         * Specifies the {@link AsyncResponseTransformer} that should be used for the download. The generic type used is
         * constrained by the {@link UntypedBuilder#responseTransformer(AsyncResponseTransformer)} that was previously used to
         * create this {@link TypedBuilder}.
         *
         * @param responseTransformer the AsyncResponseTransformer
         * @return a reference to this object so that method calls can be chained together.
         * @see AsyncResponseTransformer
         */
        TypedBuilder<T> responseTransformer(
                AsyncResponseTransformer<GetObjectResponse, T> responseTransformer);
    }
    
    private static class DefaultTypedBuilder<T> implements TypedBuilder<T> {
        private PresignedUrlDownloadRequest presignedUrlDownloadRequest;
        private List<TransferListener> transferListeners;
        private AsyncResponseTransformer<GetObjectResponse, T> responseTransformer;

        private DefaultTypedBuilder() {
        }

        private DefaultTypedBuilder(PresignedDownloadRequest<T> request) {
            this.presignedUrlDownloadRequest = request.presignedUrlDownloadRequest;
            this.responseTransformer = request.responseTransformer;
            this.transferListeners = request.transferListeners;
        }

        @Override
        public TypedBuilder<T> presignedUrlDownloadRequest(PresignedUrlDownloadRequest presignedUrlDownloadRequest) {
            this.presignedUrlDownloadRequest = presignedUrlDownloadRequest;
            return this;
        }

        @Override
        public TypedBuilder<T> responseTransformer(
                AsyncResponseTransformer<GetObjectResponse, T> responseTransformer) {
            this.responseTransformer = responseTransformer;
            return this;
        }

        @Override
        public TypedBuilder<T> transferListeners(
                Collection<TransferListener> transferListeners) {
            this.transferListeners = transferListeners != null ?
                    new ArrayList<>(transferListeners) : null;
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

        public void setTransferListeners(
                Collection<TransferListener> transferListeners) {
            transferListeners(transferListeners);
        }

        @Override
        public PresignedDownloadRequest<T> build() {
            return new PresignedDownloadRequest<>(this);
        }
    }
}