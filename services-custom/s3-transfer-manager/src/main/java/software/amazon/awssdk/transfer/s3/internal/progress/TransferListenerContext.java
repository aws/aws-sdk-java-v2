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

package software.amazon.awssdk.transfer.s3.internal.progress;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.transfer.s3.model.CompletedObjectTransfer;
import software.amazon.awssdk.transfer.s3.model.TransferObjectRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An SDK-internal implementation of {@link TransferListener.Context.TransferComplete} and its parent interfaces.
 *
 * @see TransferListenerFailedContext
 */
@SdkProtectedApi
@Immutable
public final class TransferListenerContext
    implements TransferListener.Context.TransferComplete,
               ToCopyableBuilder<TransferListenerContext.Builder, TransferListenerContext> {

    private final TransferObjectRequest request;
    private final TransferProgressSnapshot progressSnapshot;
    private final CompletedObjectTransfer completedTransfer;

    private TransferListenerContext(Builder builder) {
        this.request = builder.request;
        this.progressSnapshot = builder.progressSnapshot;
        this.completedTransfer = builder.completedTransfer;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public TransferObjectRequest request() {
        return request;
    }

    @Override
    public TransferProgressSnapshot progressSnapshot() {
        return progressSnapshot;
    }

    @Override
    public CompletedObjectTransfer completedTransfer() {
        return completedTransfer;
    }

    @Override
    public String toString() {
        return ToString.builder("TransferListenerContext")
                       .add("request", request)
                       .add("progressSnapshot", progressSnapshot)
                       .add("completedTransfer", completedTransfer)
                       .build();
    }

    public static final class Builder implements CopyableBuilder<Builder, TransferListenerContext> {
        private TransferObjectRequest request;
        private TransferProgressSnapshot progressSnapshot;
        private CompletedObjectTransfer completedTransfer;

        private Builder() {
        }

        private Builder(TransferListenerContext context) {
            this.request = context.request;
            this.progressSnapshot = context.progressSnapshot;
            this.completedTransfer = context.completedTransfer;
        }

        public Builder request(TransferObjectRequest request) {
            this.request = request;
            return this;
        }

        public Builder progressSnapshot(TransferProgressSnapshot progressSnapshot) {
            this.progressSnapshot = progressSnapshot;
            return this;
        }

        public Builder completedTransfer(CompletedObjectTransfer completedTransfer) {
            this.completedTransfer = completedTransfer;
            return this;
        }

        @Override
        public TransferListenerContext build() {
            return new TransferListenerContext(this);
        }
    }
}
