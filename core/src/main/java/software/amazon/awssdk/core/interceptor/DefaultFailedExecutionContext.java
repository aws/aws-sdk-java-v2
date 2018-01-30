/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.interceptor;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * An SDK-internal implementation of {@link Context.FailedExecution}.
 */
@SdkInternalApi
public class DefaultFailedExecutionContext implements Context.FailedExecution {
    private final InterceptorContext interceptorContext;
    private final Throwable exception;

    public DefaultFailedExecutionContext(InterceptorContext interceptorContext, Throwable exception) {
        this.interceptorContext = Validate.paramNotNull(interceptorContext, "interceptorContext");
        this.exception = unwrap(Validate.paramNotNull(exception, "exception"));
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
    public Optional<SdkHttpFullRequest> httpRequest() {
        return Optional.ofNullable(interceptorContext.httpRequest());
    }

    @Override
    public Optional<SdkHttpFullResponse> httpResponse() {
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
}
