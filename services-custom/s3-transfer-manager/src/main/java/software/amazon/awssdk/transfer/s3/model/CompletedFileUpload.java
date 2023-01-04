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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a completed upload transfer to Amazon S3. It can be used to track
 * the underlying {@link PutObjectResponse}
 *
 * @see S3TransferManager#uploadFile(UploadFileRequest)
 */
@SdkPublicApi
public final class CompletedFileUpload implements CompletedObjectTransfer {
    private final PutObjectResponse response;

    private CompletedFileUpload(DefaultBuilder builder) {
        this.response = Validate.paramNotNull(builder.response, "response");
    }
    
    @Override
    public PutObjectResponse response() {
        return response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompletedFileUpload that = (CompletedFileUpload) o;

        return Objects.equals(response, that.response);
    }

    @Override
    public int hashCode() {
        return response.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("CompletedFileUpload")
                       .add("response", response)
                       .build();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    /**
     * Creates a default builder for {@link CompletedFileUpload}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    public interface Builder {
        /**
         * Specifies the {@link PutObjectResponse} from {@link S3AsyncClient#putObject}
         *
         * @param response the response
         * @return This builder for method chaining.
         */
        Builder response(PutObjectResponse response);

        /**
         * Builds a {@link CompletedFileUpload} based on the properties supplied to this builder
         * @return An initialized {@link CompletedFileUpload}
         */
        CompletedFileUpload build();
    }

    private static class DefaultBuilder implements Builder {
        private PutObjectResponse response;

        private DefaultBuilder() {
        }

        @Override
        public Builder response(PutObjectResponse response) {
            this.response = response;
            return this;
        }

        @Override
        public CompletedFileUpload build() {
            return new CompletedFileUpload(this);
        }
    }
}
