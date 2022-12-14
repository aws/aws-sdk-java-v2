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

import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.model.TransferObjectRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An SDK-internal implementation of {@link TransferListener.Context.TransferFailed}.
 *
 * @see TransferListenerContext
 */
@SdkInternalApi
@Immutable
public class TransferListenerFailedContext
    implements TransferListener.Context.TransferFailed,
               ToCopyableBuilder<TransferListenerFailedContext.Builder, TransferListenerFailedContext> {

    private final TransferListenerContext transferContext;
    private final Throwable exception;

    private TransferListenerFailedContext(Builder builder) {
        this.exception = unwrap(Validate.paramNotNull(builder.exception, "exception"));
        this.transferContext = Validate.paramNotNull(builder.transferContext, "transferContext");
    }

    private Throwable unwrap(Throwable exception) {
        while (exception instanceof CompletionException) {
            exception = exception.getCause();
        }
        return exception;
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
        return transferContext.request();
    }

    @Override
    public TransferProgressSnapshot progressSnapshot() {
        return transferContext.progressSnapshot();
    }

    @Override
    public Throwable exception() {
        return exception;
    }

    @Override
    public String toString() {
        return ToString.builder("TransferListenerFailedContext")
                       .add("transferContext", transferContext)
                       .add("exception", exception)
                       .build();
    }

    public static final class Builder implements CopyableBuilder<Builder, TransferListenerFailedContext> {
        private TransferListenerContext transferContext;
        private Throwable exception;

        private Builder() {
        }

        private Builder(TransferListenerFailedContext failedContext) {
            this.exception = failedContext.exception;
            this.transferContext = failedContext.transferContext;
        }

        public Builder exception(Throwable exception) {
            this.exception = exception;
            return this;
        }

        public Builder transferContext(TransferListenerContext transferContext) {
            this.transferContext = transferContext;
            return this;
        }

        @Override
        public TransferListenerFailedContext build() {
            return new TransferListenerFailedContext(this);
        }
    }
}
