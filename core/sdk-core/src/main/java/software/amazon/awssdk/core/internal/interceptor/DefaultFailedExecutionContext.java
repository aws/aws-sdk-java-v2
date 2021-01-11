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

package software.amazon.awssdk.core.internal.interceptor;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An SDK-internal implementation of {@link Context.FailedExecution}.
 */
@SdkInternalApi
public class DefaultFailedExecutionContext implements Context.FailedExecution,
                                                      ToCopyableBuilder<DefaultFailedExecutionContext.Builder,
                                                          DefaultFailedExecutionContext> {
    private final InterceptorContext interceptorContext;
    private final Throwable exception;

    private DefaultFailedExecutionContext(Builder builder) {
        this.exception = unwrap(Validate.paramNotNull(builder.exception, "exception"));
        this.interceptorContext = Validate.paramNotNull(builder.interceptorContext, "interceptorContext");
    }

    private Throwable unwrap(Throwable exception) {
        while (exception instanceof CompletionException) {
            exception = exception.getCause();
        }
        return exception;
    }

    @Override
    public SdkRequest request() {
        return interceptorContext.request();
    }

    @Override
    public Optional<SdkHttpRequest> httpRequest() {
        return Optional.ofNullable(interceptorContext.httpRequest());
    }

    @Override
    public Optional<SdkHttpResponse> httpResponse() {
        return Optional.ofNullable(interceptorContext.httpResponse());
    }

    @Override
    public Optional<SdkResponse> response() {
        return Optional.ofNullable(interceptorContext.response());
    }

    @Override
    public Throwable exception() {
        return exception;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements CopyableBuilder<Builder, DefaultFailedExecutionContext> {
        private InterceptorContext interceptorContext;
        private Throwable exception;

        private Builder() {
        }

        public Builder(DefaultFailedExecutionContext defaultFailedExecutionContext) {
            this.exception = defaultFailedExecutionContext.exception;
            this.interceptorContext = defaultFailedExecutionContext.interceptorContext;
        }

        public Builder exception(Throwable exception) {
            this.exception = exception;
            return this;
        }

        public Builder interceptorContext(InterceptorContext interceptorContext) {
            this.interceptorContext = interceptorContext;
            return this;
        }

        @Override
        public DefaultFailedExecutionContext build() {
            return new DefaultFailedExecutionContext(this);
        }
    }
}
