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
import java.util.Optional;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * A wrapper for the immutable context objects that are visible to the {@link ExecutionInterceptor}s.
 */
@SdkProtectedApi
public final class Context {
    private Context() {
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#beforeExecution} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface BeforeExecution {
        /**
         * The {@link SdkRequest} to be executed.
         */
        SdkRequest request();
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#modifyRequest} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface ModifyRequest extends BeforeExecution {
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#beforeMarshalling} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface BeforeMarshalling extends ModifyRequest {
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#afterMarshalling} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface AfterMarshalling extends BeforeMarshalling {
        /**
         * The {@link SdkHttpRequest} that was created as a result of marshalling the {@link #request()}. This is the HTTP
         * request that will be sent to the downstream service.
         */
        SdkHttpRequest httpRequest();

        /**
         * The {@link RequestBody} that represents the body of an HTTP request.
         */
        Optional<RequestBody> requestBody();

        /**
         * The {@link AsyncRequestBody} that allows non-blocking streaming of request content.
         */
        Optional<AsyncRequestBody> asyncRequestBody();
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#modifyHttpRequest} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface ModifyHttpRequest extends AfterMarshalling {
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#beforeTransmission} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface BeforeTransmission extends ModifyHttpRequest {
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#afterTransmission} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface AfterTransmission extends BeforeTransmission {
        /**
         * The HTTP response returned by the service with which the SDK is communicating.
         */
        SdkHttpResponse httpResponse();

        /**
         * The {@link Publisher} that provides {@link ByteBuffer} events upon request.
         */
        Optional<Publisher<ByteBuffer>> responsePublisher();

        /**
         * The {@link InputStream} that provides streaming content returned from the service.
         */
        Optional<InputStream> responseBody();
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#modifyHttpResponse} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface ModifyHttpResponse extends AfterTransmission {
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#beforeUnmarshalling} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface BeforeUnmarshalling extends ModifyHttpResponse {
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#afterUnmarshalling} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface AfterUnmarshalling extends BeforeUnmarshalling {
        /**
         * The {@link SdkResponse} that was generated by unmarshalling the {@link #httpResponse()}.
         */
        SdkResponse response();
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#modifyResponse} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface ModifyResponse extends AfterUnmarshalling {
    }

    /**
     * The state of the execution when the {@link ExecutionInterceptor#afterExecution} method is invoked.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface AfterExecution extends ModifyResponse {
    }

    /**
     * All information that is known about a particular execution that has failed. This is given to
     * {@link ExecutionInterceptor#onExecutionFailure} if an entire execution fails for any reason. This includes all information
     * that is known about the request, like the {@link #request()} and the {@link #exception()} that caused the failure.
     */
    @ThreadSafe
    @SdkPublicApi
    public interface FailedExecution {
        /**
         * The exception associated with the failed execution. This is the reason the execution has failed, and is the exception
         * that will be returned or thrown from the client method call. This will never return null.
         */
        Throwable exception();

        /**
         * The latest version of the {@link SdkRequest} available when the execution failed. This will never return null.
         */
        SdkRequest request();

        /**
         * The latest version of the {@link SdkHttpFullRequest} available when the execution failed. If the execution failed
         * before or during request marshalling, this will return {@link Optional#empty()}.
         */
        Optional<SdkHttpRequest> httpRequest();

        /**
         * The latest version of the {@link SdkHttpFullResponse} available when the execution failed. If the execution failed
         * before or during transmission, this will return {@link Optional#empty()}.
         */
        Optional<SdkHttpResponse> httpResponse();

        /**
         * The latest version of the {@link SdkResponse} available when the execution failed. If the execution failed before or
         * during response unmarshalling, this will return {@link Optional#empty()}.
         */
        Optional<SdkResponse> response();
    }
}
