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

package software.amazon.awssdk.core.internal.progress;

import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.progress.listener.ProgressListener;
import software.amazon.awssdk.core.progress.snapshot.ProgressSnapshot;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An SDK-internal implementation of {@link ProgressListener.Context.ExecutionFailure}.
 * An instance of this class can be used by ProgressListener methods to capture and store failed request
 *
 * @see ProgressListenerContext for a successful request progress state capturing
 */
@SdkInternalApi
@Immutable
public class ProgressListenerFailedContext
    implements ProgressListener.Context.ExecutionFailure,
               ToCopyableBuilder<ProgressListenerFailedContext.Builder, ProgressListenerFailedContext> {

    private final ProgressListenerContext progressListenerContext;
    private final Throwable exception;

    private ProgressListenerFailedContext(Builder builder) {
        this.exception = unwrap(Validate.paramNotNull(builder.exception, "exception"));
        this.progressListenerContext = Validate.paramNotNull(builder.progressListenerContext, "progressListenerContext");
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
    public SdkRequest request() {
        return progressListenerContext.request();
    }

    @Override
    public SdkHttpRequest httpRequest() {
        return progressListenerContext.httpRequest();
    }

    @Override
    public ProgressSnapshot uploadProgressSnapshot() {
        return progressListenerContext.uploadProgressSnapshot();
    }

    @Override
    public Throwable exception() {
        return exception;
    }

    @Override
    public String toString() {
        return ToString.builder("ProgressListenerFailedContext")
                       .add("progressListenerContext", progressListenerContext)
                       .add("exception", exception)
                       .build();
    }

    public static final class Builder implements CopyableBuilder<Builder, ProgressListenerFailedContext> {
        private ProgressListenerContext progressListenerContext;
        private Throwable exception;

        private Builder() {
        }

        private Builder(ProgressListenerFailedContext failedContext) {
            this.exception = failedContext.exception;
            this.progressListenerContext = failedContext.progressListenerContext;
        }

        public Builder exception(Throwable exception) {
            this.exception = exception;
            return this;
        }

        public Builder progressListenerContext(ProgressListenerContext progressListenerContext) {
            this.progressListenerContext = progressListenerContext;
            return this;
        }

        @Override
        public ProgressListenerFailedContext build() {
            return new ProgressListenerFailedContext(this);
        }
    }
}

