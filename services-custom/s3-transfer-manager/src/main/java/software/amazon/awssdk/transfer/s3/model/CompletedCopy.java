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
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a completed copy transfer to Amazon S3. It can be used to track the underlying {@link CopyObjectResponse}
 *
 * @see S3TransferManager#copy(CopyRequest)
 */
@SdkPublicApi
public final class CompletedCopy implements CompletedObjectTransfer {
    private final CopyObjectResponse response;

    private CompletedCopy(DefaultBuilder builder) {
        this.response = Validate.paramNotNull(builder.response, "response");
    }

    /**
     * Returns the {@link CopyObjectResponse}.
     */
    @Override
    public CopyObjectResponse response() {
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

        CompletedCopy that = (CompletedCopy) o;

        return Objects.equals(response, that.response);
    }

    @Override
    public int hashCode() {
        return response.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("CompletedCopy")
                       .add("response", response)
                       .build();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    /**
     * Creates a default builder for {@link CompletedCopy}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    public interface Builder {
        /**
         * Specifies the {@link CopyObjectResponse} from {@link S3AsyncClient#putObject}
         *
         * @param response the response
         * @return This builder for method chaining.
         */
        Builder response(CopyObjectResponse response);

        /**
         * Builds a {@link CompletedCopy} based on the properties supplied to this builder
         *
         * @return An initialized {@link CompletedCopy}
         */
        CompletedCopy build();
    }

    private static class DefaultBuilder implements Builder {
        private CopyObjectResponse response;

        private DefaultBuilder() {
        }

        @Override
        public Builder response(CopyObjectResponse response) {
            this.response = response;
            return this;
        }

        @Override
        public CompletedCopy build() {
            return new CompletedCopy(this);
        }
    }
}
