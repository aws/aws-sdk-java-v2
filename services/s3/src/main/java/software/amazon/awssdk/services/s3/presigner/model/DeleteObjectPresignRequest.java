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

package software.amazon.awssdk.services.s3.presigner.model;

import java.time.Duration;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.presigner.PresignRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link DeleteObjectRequest} so that it can be executed at a later time without requiring additional
 * signing or authentication.
 *
 * @see S3Presigner#presignDeleteObject(DeleteObjectPresignRequest
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class DeleteObjectPresignRequest extends PresignRequest
    implements ToCopyableBuilder<DeleteObjectPresignRequest.Builder, DeleteObjectPresignRequest> {
    private final DeleteObjectRequest deleteObjectRequest;

    protected DeleteObjectPresignRequest(DefaultBuilder builder) {
        super(builder);
        this.deleteObjectRequest = Validate.notNull(builder.deleteObjectRequest, "deleteObjectRequest");
    }

    /**
     * Retrieve the {@link DeleteObjectRequest} that should be presigned.
     */
    public DeleteObjectRequest deleteObjectRequest()  {
        return deleteObjectRequest;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    /**
     * Create a builder that can be used to create a {@link DeleteObjectPresignRequest}.
     *
     * @see S3Presigner#presignDeleteObject(DeleteObjectPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        DeleteObjectPresignRequest that = (DeleteObjectPresignRequest) o;

        return deleteObjectRequest.equals(that.deleteObjectRequest);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + deleteObjectRequest.hashCode();
        return result;
    }

    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends PresignRequest.Builder,
                                     CopyableBuilder<DeleteObjectPresignRequest.Builder, DeleteObjectPresignRequest> {
        Builder deleteObjectRequest(DeleteObjectRequest deleteObjectRequest);

        default Builder deleteObjectRequest(Consumer<DeleteObjectRequest.Builder> deleteObjectRequest) {
            DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
            deleteObjectRequest.accept(builder);
            return deleteObjectRequest(builder.build());
        }

        @Override
        Builder signatureDuration(Duration signatureDuration);

        @Override
        DeleteObjectPresignRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder extends PresignRequest.DefaultBuilder<DefaultBuilder> implements Builder {
        private DeleteObjectRequest deleteObjectRequest;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DeleteObjectPresignRequest deleteObjectPresignRequest) {
            super(deleteObjectPresignRequest);
            this.deleteObjectRequest = deleteObjectPresignRequest.deleteObjectRequest;
        }

        @Override
        public Builder deleteObjectRequest(DeleteObjectRequest deleteObjectRequest) {
            this.deleteObjectRequest = deleteObjectRequest;
            return this;
        }

        @Override
        public DeleteObjectPresignRequest build() {
            return new DeleteObjectPresignRequest(this);
        }
    }
}
