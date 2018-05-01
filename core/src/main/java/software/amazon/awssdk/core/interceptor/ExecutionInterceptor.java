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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * An interceptor that is invoked during the execution lifecycle of a request/response (execution). This can be used to publish
 * metrics, modify a request in-flight, debug request processing, view exceptions, etc. This interface exposes different methods
 * for hooking into different parts of the lifecycle of an execution.
 *
 * <p>
 * <b>Interceptor Hooks</b>
 * Methods for a given interceptor are executed in a predictable order, each receiving the information that is known about the
 * message so far as well as a {@link ExecutionAttributes} object for storing data that is specific to a particular execution.
 * <ol>
 *     <li>{@link #beforeExecution} - Read the request before it is modified by other interceptors.</li>
 *     <li>{@link #modifyRequest} - Modify the request object before it is marshalled into an HTTP request.</li>
 *     <li>{@link #beforeMarshalling} - Read the request that has potentially been modified by other request interceptors before
 *     it is marshalled into an HTTP request.</li>
 *     <li>{@link #afterMarshalling} - Read the HTTP request after it is created and before it can be modified by other
 *     interceptors.</li>
 *     <li>{@link #modifyHttpRequest} - Modify the HTTP request object before it is transmitted.</li>
 *     <li>{@link #beforeTransmission} - Read the HTTP request that has potentially been modified by other request interceptors
 *     before it is sent to the service.</li>
 *     <li>{@link #afterTransmission} - Read the HTTP response after it is received and before it can be modified by other
 *     interceptors.</li>
 *     <li>{@link #modifyHttpResponse} - Modify the HTTP response object before it is unmarshalled.</li>
 *     <li>{@link #beforeUnmarshalling} - Read the HTTP response that has potentially been modified by other request interceptors
 *     before it is unmarshalled.</li>
 *     <li>{@link #afterUnmarshalling} - Read the response after it is created and before it can be modified by other
 *     interceptors.</li>
 *     <li>{@link #modifyResponse} - Modify the response object before before it is returned to the client.</li>
 *     <li>{@link #afterExecution} - Read the response that has potentially been modified by other request interceptors.</li>
 * </ol>
 * An additional {@link #onExecutionFailure} method is provided that is invoked if an execution fails at any point during the
 * lifecycle of a request, including exceptions being thrown from this or other interceptors.
 * </p>
 *
 * <p>
 * <b>Interceptor Registration</b>
 * Interceptors can be registered in one of many ways.
 * <ol>
 *     <li><i>Override Configuration Interceptors</i> are the most common method for SDK users to register an interceptor. These
 *     interceptors are explicitly added to the client builder's override configuration when a client is created using the {@link
 *     software.amazon.awssdk.core.config.ClientOverrideConfiguration.Builder#addExecutionInterceptor(ExecutionInterceptor)}
 *     method.</li>
 *
 *     <li><i>Global Interceptors</i> are interceptors loaded from the classpath for all clients. When any service client is
 *     created by a client builder, all jars on the classpath (from the perspective of the current thread's classloader) are
 *     checked for a file named '/software/amazon/awssdk/global/handlers/execution.interceptors'. Any interceptors listed in these
 *     files (new line separated) are instantiated using their default constructor and loaded into the client.</li>
 *
 *     <li><i>Service Interceptors</i> are interceptors loaded from the classpath for a particular service's clients. When a
 *     service client is created by a client builder, all jars on the classpath (from the perspective of the current thread's
 *     classloader) are checked for a file named '/software/amazon/awssdk/services/{service}/execution.interceptors', where
 *     {service} is the package name of the service client. Any interceptors listed in these files (new line separated) are
 *     instantiated using their default constructor and loaded into the client.</li>
 * </ol>
 * </p>
 *
 * <p>
 * <b>Interceptor Order</b>
 * The order in which interceptors are executed is sometimes relevant to the accuracy of the interceptor itself. For example, an
 * interceptor that adds a field to a message should be executed before an interceptor that reads and modifies that field.
 * Interceptor's order is determined by their method of registration. The following order is used:
 * <ol>
 *     <li><i>Global Interceptors</i>. Interceptors earlier in the classpath will be placed earlier in the interceptor order than
 *     interceptors later in the classpath. Interceptors earlier within a specific file on the classpath will be placed earlier in
 *     the order than interceptors later in the file.</li>
 *
 *     <li><i>Service Interceptors</i>. Interceptors earlier in the classpath will be placed earlier in the interceptor order than
 *     interceptors later in the classpath. Interceptors earlier within a specific file on the classpath will be placed earlier in
 *     the order than interceptors later in the file.</li>
 *
 *     <li><i>Override Configuration Interceptors</i>. Any interceptors registered using {@link
 *     software.amazon.awssdk.core.config.ClientOverrideConfiguration.Builder#addExecutionInterceptor(ExecutionInterceptor)}
 *     in the order they were added.</li>
 * </ol>
 * When a request is being processed (up to and including {@link #beforeTransmission}, interceptors are applied in forward-order,
 * according to the order described above. When a response is being processed (after and including {@link #afterTransmission},
 * interceptors are applied in reverse-order from the order described above. This means that the last interceptors to touch the
 * request are the first interceptors to touch the response.
 * </p>
 *
 * <p>
 * <b>Execution Attributes</b>
 * {@link ExecutionAttributes} are unique to an execution (the process of an SDK processing a {@link SdkRequest}). This mutable
 * collection of attributes is created when a call to a service client is made and can be mutated throughout the course of the
 * client call. These attributes are made available to every interceptor hook and is available for storing data between method
 * calls. The SDK provides some attributes automatically, available via {@link SdkExecutionAttributes}.
 * </p>
 *
 * <p>
 * <b><i>Note: This interface will change between SDK versions and should not be implemented by SDK users.</i></b>
 * </p>
 */
@SdkInternalApi
public interface ExecutionInterceptor {
    /**
     * Read a request that has been given to a service client before it is modified by other interceptors.
     * {@link #beforeMarshalling} should be used in most circumstances for reading the request because it includes modifications
     * made by other interceptors.
     *
     * <p>This method is guaranteed to be executed on the thread that is making the client call. This is true even if a non-
     * blocking I/O client is used. This is useful for transferring data that may be stored thread-locally into the execution's
     * {@link ExecutionAttributes}.</p>
     *
     * @param context The current state of the execution, including the unmodified SDK request from the service client call.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     */
    default void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {

    }

    /**
     * Modify an {@link SdkRequest} given to a service client before it is marshalled into an {@link SdkHttpFullRequest}.
     *
     * @param context The current state of the execution, including the current SDK request from the service client call.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     *                            give data to future lifecycle methods.
     * @return The potentially-modified request that should be used for the rest of the execution. Must not be null.
     */
    default SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        return context.request();
    }

    /**
     * Read the finalized request as it will be given to the marshaller to be converted into an {@link SdkHttpFullRequest}.
     *
     * @param context The current state of the execution, including the SDK request (potentially modified by other interceptors)
     *                from the service client call.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     */
    default void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {

    }

    /**
     * Read the marshalled HTTP request, before it is modified by other interceptors. {@link #beforeTransmission} should be used
     * in most circumstances for reading the HTTP request because it includes modifications made by other interceptors.
     *
     * @param context The current state of the execution, including the SDK and unmodified HTTP request.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     */
    default void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {

    }

    /**
     * Modify the {@link SdkHttpFullRequest} before it is sent to the service.
     *
     * @param context The current state of the execution, including the SDK and current HTTP request.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     *                            give data to future lifecycle methods.
     * @return The potentially-modified HTTP request that should be sent to the service. Must not be null.
     */
    default SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        return context.httpRequest();
    }

    /**
     * Read the finalized HTTP request as it will be sent to the HTTP client. This includes modifications made by other
     * interceptors and the message signature. It is possible that the HTTP client could further modify the request, so debug-
     * level wire logging should be trusted over the parameters to this method.
     *
     * <p>Note: Unlike many other lifecycle methods, this one may be invoked multiple times. If the {@link RetryPolicy} determines
     * a request failure is retriable, this will be invoked for each retry attempt.</p>
     *
     * @param context The current state of the execution, including the SDK and HTTP request (potentially modified by other
     *                interceptors) to be sent to the downstream service.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     */
    default void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {

    }

    /**
     * Read the HTTP response as it was returned by the HTTP client, before it is modified by other interceptors.
     * {@link #beforeTransmission} should be used in most circumstances for reading the HTTP response because it includes
     * modifications made by other interceptors.
     *
     * <p>It is possible that the HTTP client could have already modified this response, so debug-level wire logging should be
     * trusted over the parameters to this method.</p>
     *
     * <p>Note: Unlike many other lifecycle methods, this one may be invoked multiple times. If the {@link RetryPolicy} determines
     * the error code returned by the service is retriable, this will be invoked for each response returned by the service.</p>
     *
     * @param context The current state of the execution, including the SDK and HTTP requests and the unmodified HTTP response.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     */
    default void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {

    }

    /**
     * Modify the {@link SdkHttpFullRequest} before it is unmarshalled into an {@link SdkResponse}.
     *
     * <p>Note: Unlike many other lifecycle methods, this one may be invoked multiple times. If the {@link RetryPolicy} determines
     * the error code returned by the service is retriable, this will be invoked for each response returned by the service.</p>
     *
     * @param context The current state of the execution, including the SDK and HTTP requests and the current HTTP response.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     *                            give data to future lifecycle methods.
     * @return The potentially-modified HTTP response that should be given to the unmarshaller. Must not be null.
     */
    default SdkHttpFullResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                   ExecutionAttributes executionAttributes) {
        return context.httpResponse();
    }

    /**
     * Read the finalized HTTP response as it will be given to the unmarshaller to be converted into an {@link SdkResponse}.
     *
     * <p>Note: Unlike many other lifecycle methods, this one may be invoked multiple times. If the {@link RetryPolicy} determines
     * the error code returned by the service is retriable, this will be invoked for each response returned by the service.</p>
     *
     * @param context The current state of the execution, including the SDK and HTTP requests as well as the (potentially
     *                modified by other interceptors) HTTP response.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     *                            give data to future lifecycle methods.
     */
    default void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {

    }

    /**
     * Read the {@link SdkResponse} as it was returned by the unmarshaller, before it is modified by other interceptors.
     * {@link #afterExecution} should be used in most circumstances for reading the SDK response because it includes
     * modifications made by other interceptors.
     *
     * @param context The current state of the execution, including the SDK and HTTP requests and the HTTP response.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     *                            give data to future lifecycle methods.
     */
    default void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {

    }

    /**
     * Modify the {@link SdkResponse} before it is returned by the client.
     *
     * @param context The current state of the execution, including the SDK and HTTP requests as well as the SDK and HTTP
     *                response.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     *                            give data to future lifecycle methods.
     * @return The potentially-modified SDK response that should be returned by the client. Must not be null.
     */
    default SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        return context.response();
    }

    /**
     * Read the finalized {@link SdkResponse} as it will be returned by the client invocation.
     *
     * @param context The current state of the execution, including the SDK and HTTP requests as well as the SDK and HTTP
     *                response.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     *                            give data to future lifecycle methods.
     */
    default void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {

    }

    /**
     * Invoked when any error happens during an execution that prevents the request from succeeding. This could be due to an
     * error returned by a service call, a request timeout or even another interceptor raising an exception. The provided
     * exception will be thrown by the service client.
     *
     * <p>This will only be invoked if the entire execution fails. If a retriable error happens (according to the
     * {@link RetryPolicy}) and a subsequent retry succeeds, this method will not be invoked.</p>
     *
     * @param context The context associated with the execution that failed. An SDK request will always be available, but
     *                depending on the time at which the failure happened, the HTTP request, HTTP response and SDK response may
     *                not be available. This also includes the exception that triggered the failure.
     * @param executionAttributes A mutable set of attributes scoped to one specific request/response cycle that can be used to
     *                            give data to future lifecycle methods.
     */
    default void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {

    }
}
