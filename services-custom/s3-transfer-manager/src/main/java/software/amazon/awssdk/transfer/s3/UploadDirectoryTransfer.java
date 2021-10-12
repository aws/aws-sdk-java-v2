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

package software.amazon.awssdk.transfer.s3;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;

@SdkPublicApi
@SdkPreviewApi
public final class UploadDirectoryTransfer implements Transfer {
    private final CompletableFuture<CompletedUploadDirectory> completionFuture;

    private UploadDirectoryTransfer(DefaultBuilder builder) {
        this.completionFuture = builder.completionFuture;
    }

    @Override
    public CompletableFuture<CompletedUploadDirectory> completionFuture() {
        return completionFuture;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    public interface Builder {

        /**
         * Specifies the future that will be completed when this transfer is complete.
         *
         * @param completionFuture the future that will be completed when this transfer is complete.
         * @return This builder for method chaining.
         */
        Builder completionFuture(CompletableFuture<CompletedUploadDirectory> completionFuture);

        /**
         * Builds a {@link UploadDirectoryTransfer} based on the properties supplied to this builder
         *
         * @return An initialized {@link UploadDirectoryTransfer}
         */
        UploadDirectoryTransfer build();
    }

    private static final class DefaultBuilder implements Builder {
        private CompletableFuture<CompletedUploadDirectory> completionFuture;

        private DefaultBuilder() {
        }

        @Override
        public DefaultBuilder completionFuture(CompletableFuture<CompletedUploadDirectory> completionFuture) {
            this.completionFuture = completionFuture;
            return this;
        }

        public void setCompletionFuture(CompletableFuture<CompletedUploadDirectory> completionFuture) {
            completionFuture(completionFuture);
        }

        @Override
        public UploadDirectoryTransfer build() {
            return new UploadDirectoryTransfer(this);
        }
    }
}
