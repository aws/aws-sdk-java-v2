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

package software.amazon.awssdk.core.interceptor;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.interceptor.DefaultFailedExecutionContext;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A wrapper for a list of {@link ExecutionInterceptor}s that ensures the interceptors are executed in the correct order as it
 * is documented in the {@link ExecutionInterceptor} documentation.
 *
 * Interceptors are invoked in forward order up to {@link #beforeTransmission} and in reverse order after (and including)
 * {@link #afterTransmission}. This ensures the last interceptors to modify the request are the first interceptors to see the
 * response.
 */
@SdkProtectedApi
public class ExecutionInterceptorChain {
    private static final Logger LOG = Logger.loggerFor(ExecutionInterceptorChain.class);

    private final List<ExecutionInterceptor> interceptors;

    /**
     * Create a chain that will execute the provided interceptors in the order they are provided.
     */
    public ExecutionInterceptorChain(List<ExecutionInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(Validate.paramNotNull(interceptors, "interceptors"));
        LOG.debug(() -> "Creating an interceptor chain that will apply interceptors in the following order: " + interceptors);
    }

    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        interceptors.forEach(i -> i.beforeExecution(context, executionAttributes));
    }

    public InterceptorContext modifyRequest(InterceptorContext context, ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;
        for (ExecutionInterceptor interceptor : interceptors) {
            SdkRequest interceptorResult = interceptor.modifyRequest(result, executionAttributes);
            validateInterceptorResult(result.request(), interceptorResult, interceptor, "modifyRequest");

            result = result.copy(b -> b.request(interceptorResult));
        }
        return result;
    }

    public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
        interceptors.forEach(i -> i.beforeMarshalling(context, executionAttributes));
    }

    public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
        interceptors.forEach(i -> i.afterMarshalling(context, executionAttributes));
    }

    public InterceptorContext modifyHttpRequestAndHttpContent(InterceptorContext context,
                                                              ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;
        for (ExecutionInterceptor interceptor : interceptors) {
            AsyncRequestBody asyncRequestBody = interceptor.modifyAsyncHttpContent(result, executionAttributes).orElse(null);
            RequestBody requestBody = interceptor.modifyHttpContent(result, executionAttributes).orElse(null);
            SdkHttpRequest interceptorResult = interceptor.modifyHttpRequest(result, executionAttributes);
            validateInterceptorResult(result.httpRequest(), interceptorResult, interceptor, "modifyHttpRequest");

            result = applySdkHttpFullRequestHack(result);

            result = result.copy(b -> b.httpRequest(interceptorResult)
                                       .asyncRequestBody(asyncRequestBody)
                                       .requestBody(requestBody));
        }
        return result;
    }

    private InterceptorContext applySdkHttpFullRequestHack(InterceptorContext context) {
        // Someone thought it would be a great idea to allow interceptors to return SdkHttpFullRequest to modify the payload
        // instead of using the modifyPayload method. This is for backwards-compatibility with those interceptors.
        // TODO: Update interceptors to use the proper payload-modifying method so that this code path is only used for older
        // client versions. Maybe if we ever decide to break @SdkProtectedApis (if we stop using Jackson?!) we can even remove
        // this hack!
        SdkHttpFullRequest sdkHttpFullRequest = (SdkHttpFullRequest) context.httpRequest();

        if (context.requestBody().isPresent()) {
            return context;
        }

        Optional<ContentStreamProvider> contentStreamProvider = sdkHttpFullRequest.contentStreamProvider();

        if (!contentStreamProvider.isPresent()) {
            return context;
        }

        long contentLength = Long.parseLong(sdkHttpFullRequest.firstMatchingHeader("Content-Length").orElse("0"));
        String contentType = sdkHttpFullRequest.firstMatchingHeader("Content-Type").orElse("");
        RequestBody requestBody = RequestBody.fromContentProvider(contentStreamProvider.get(),
                                                                  contentLength,
                                                                  contentType);
        return context.toBuilder().requestBody(requestBody).build();
    }

    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        interceptors.forEach(i -> i.beforeTransmission(context, executionAttributes));
    }

    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        reverseForEach(i -> i.afterTransmission(context, executionAttributes));
    }

    public InterceptorContext modifyHttpResponse(InterceptorContext context,
                                                 ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;

        for (int i = interceptors.size() - 1; i >= 0; i--) {
            SdkHttpResponse interceptorResult =
                interceptors.get(i).modifyHttpResponse(result, executionAttributes);
            validateInterceptorResult(result.httpResponse(), interceptorResult, interceptors.get(i), "modifyHttpResponse");

            InputStream response = interceptors.get(i).modifyHttpResponseContent(result, executionAttributes).orElse(null);

            result = result.toBuilder().httpResponse(interceptorResult).responseBody(response).build();
        }

        return result;
    }

    public InterceptorContext modifyAsyncHttpResponse(InterceptorContext context,
                                                      ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;

        for (int i = interceptors.size() - 1; i >= 0; i--) {
            ExecutionInterceptor interceptor = interceptors.get(i);

            Publisher<ByteBuffer> newResponsePublisher =
                interceptor.modifyAsyncHttpResponseContent(result, executionAttributes).orElse(null);

            result = result.toBuilder()
                           .responsePublisher(newResponsePublisher)
                           .build();
        }

        return result;
    }

    public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {
        reverseForEach(i -> i.beforeUnmarshalling(context, executionAttributes));
    }

    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
        reverseForEach(i -> i.afterUnmarshalling(context, executionAttributes));
    }

    public InterceptorContext modifyResponse(InterceptorContext context, ExecutionAttributes executionAttributes) {
        InterceptorContext result = context;
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            SdkResponse interceptorResult = interceptors.get(i).modifyResponse(result, executionAttributes);
            validateInterceptorResult(result.response(), interceptorResult, interceptors.get(i), "modifyResponse");

            result = result.copy(b -> b.response(interceptorResult));
        }

        return result;
    }

    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        reverseForEach(i -> i.afterExecution(context, executionAttributes));
    }

    public DefaultFailedExecutionContext modifyException(DefaultFailedExecutionContext context,
                                                         ExecutionAttributes executionAttributes) {
        DefaultFailedExecutionContext result = context;
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            Throwable interceptorResult = interceptors.get(i).modifyException(result, executionAttributes);
            validateInterceptorResult(result.exception(), interceptorResult, interceptors.get(i), "modifyException");
            result = result.copy(b -> b.exception(interceptorResult));
        }

        return result;
    }

    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        interceptors.forEach(i -> i.onExecutionFailure(context, executionAttributes));
    }

    /**
     * Validate the result of calling an interceptor method that is attempting to modify the message to make sure its result is
     * valid.
     */
    private void validateInterceptorResult(Object originalMessage, Object newMessage,
                                           ExecutionInterceptor interceptor, String methodName) {
        if (!Objects.equals(originalMessage, newMessage)) {
            LOG.debug(() -> "Interceptor '" + interceptor + "' modified the message with its " + methodName + " method.");
            LOG.trace(() -> "Old: " + originalMessage + "\nNew: " + newMessage);
        }
        Validate.validState(newMessage != null,
                            "Request interceptor '%s' returned null from its %s interceptor.",
                            interceptor, methodName);
        Validate.isInstanceOf(originalMessage.getClass(), newMessage,
                              "Request interceptor '%s' returned '%s' from its %s method, but '%s' was expected.",
                              interceptor, newMessage.getClass(), methodName, originalMessage.getClass());
    }

    /**
     * Execute the provided action against the interceptors in this chain in the reverse order they are configured.
     */
    private void reverseForEach(Consumer<ExecutionInterceptor> action) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            action.accept(interceptors.get(i));
        }
    }
}
