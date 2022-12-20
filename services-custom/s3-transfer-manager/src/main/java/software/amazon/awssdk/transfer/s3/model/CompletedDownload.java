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
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload.TypedBuilder;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Represents a completed download transfer from Amazon S3. It can be used to track the underlying result
 * that was transformed via an {@link AsyncResponseTransformer}.
 *
 * @see S3TransferManager#download(DownloadRequest)
 */
@SdkPublicApi
public final class CompletedDownload<ResultT>
    implements CompletedObjectTransfer,
               ToCopyableBuilder<TypedBuilder<ResultT>, CompletedDownload<ResultT>> {

    private final ResultT result;

    private CompletedDownload(DefaultTypedBuilder<ResultT> builder) {
        this.result = Validate.paramNotNull(builder.result, "result");
    }

    /**
     * Creates a builder that can be used to create a {@link CompletedDownload}.
     *
     * @see UntypedBuilder
     */
    public static UntypedBuilder builder() {
        return new DefaultUntypedBuilder();
    }


    @Override
    public TypedBuilder<ResultT> toBuilder() {
        return new DefaultTypedBuilder<>(this);
    }

    /**
     * Returns the result.
     */
    public ResultT result() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompletedDownload<?> that = (CompletedDownload<?>) o;

        return Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return result != null ? result.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("CompletedDownload")
                       .add("result", result)
                       .build();
    }

    /**
     * Initial calls to {@link CompletedDownload#builder()} return an {@link UntypedBuilder}, where the builder is not yet
     * parameterized with the generic type associated with {@link CompletedDownload}. This prevents the otherwise awkward syntax
     * of having to explicitly cast the builder type, e.g.,
     * {@snippet :
     *  CompletedDownload.<ResponseBytes<GetObjectResponse>>builder()
     * }
     * Instead, the type may be inferred as part of specifying the {@link #result(Object)} parameter, at which point the builder
     * chain will return a new {@link TypedBuilder}.
     */
    public interface UntypedBuilder {

        /**
         * Specifies the result of the completed download. This method also infers the generic type of {@link CompletedDownload}
         * to create.
         *
         * @param result the result of the completed download, as transformed by an {@link AsyncResponseTransformer}
         * @param <T>    the type of {@link CompletedDownload} to create
         * @return a reference to this object so that method calls can be chained together.
         */
        <T> TypedBuilder<T> result(T result);
    }

    private static class DefaultUntypedBuilder implements UntypedBuilder {
        private DefaultUntypedBuilder() {
        }

        @Override
        public <T> TypedBuilder<T> result(T result) {
            return new DefaultTypedBuilder<T>()
                .result(result);
        }
    }

    /**
     * The type-parameterized version of {@link UntypedBuilder}. This builder's type is inferred as part of specifying {@link
     * #result(Object)}, after which this builder can be used to construct a {@link CompletedDownload} with the same generic
     * type.
     */
    public interface TypedBuilder<T> extends CopyableBuilder<TypedBuilder<T>, CompletedDownload<T>> {

        /**
         * Specifies the result of the completed download. The generic type used is constrained by the {@link
         * UntypedBuilder#result(Object)} that  was previously used to create this {@link TypedBuilder}.
         *
         * @param result the result of the completed download, as transformed by an {@link AsyncResponseTransformer}
         * @return a reference to this object so that method calls can be chained together.
         */
        TypedBuilder<T> result(T result);
    }


    private static class DefaultTypedBuilder<T> implements TypedBuilder<T> {
        private T result;

        private DefaultTypedBuilder() {
        }

        private DefaultTypedBuilder(CompletedDownload<T> request) {
            this.result = request.result;
        }


        @Override
        public TypedBuilder<T> result(T result) {
            this.result = result;
            return this;
        }


        @Override
        public CompletedDownload<T> build() {
            return new CompletedDownload<>(this);
        }
    }
}
