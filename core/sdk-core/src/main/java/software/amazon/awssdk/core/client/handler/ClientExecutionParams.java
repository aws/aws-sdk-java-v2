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

package software.amazon.awssdk.core.client.handler;

import java.net.URI;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * Encapsulates parameters needed for a particular API call. Captures input and output pojo types.
 *
 * @param <InputT>  Input POJO type.
 * @param <OutputT> Output POJO type.
 */
@SdkProtectedApi
@NotThreadSafe
public final class ClientExecutionParams<InputT extends SdkRequest, OutputT> {

    private InputT input;
    private RequestBody requestBody;
    private AsyncRequestBody asyncRequestBody;
    private Marshaller<InputT> marshaller;
    private HttpResponseHandler<OutputT> responseHandler;
    private HttpResponseHandler<? extends SdkException> errorResponseHandler;
    private HttpResponseHandler<Response<OutputT>> combinedResponseHandler;
    private boolean fullDuplex;
    private String hostPrefixExpression;
    private String operationName;
    private URI discoveredEndpoint;

    public Marshaller<InputT> getMarshaller() {
        return marshaller;
    }

    public ClientExecutionParams<InputT, OutputT> withMarshaller(Marshaller<InputT> marshaller) {
        this.marshaller = marshaller;
        return this;
    }

    public InputT getInput() {
        return input;
    }

    public ClientExecutionParams<InputT, OutputT> withInput(InputT input) {
        this.input = input;
        return this;
    }

    public HttpResponseHandler<OutputT> getResponseHandler() {
        return responseHandler;
    }

    public ClientExecutionParams<InputT, OutputT> withResponseHandler(
            HttpResponseHandler<OutputT> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    public HttpResponseHandler<? extends SdkException> getErrorResponseHandler() {
        return errorResponseHandler;
    }

    public ClientExecutionParams<InputT, OutputT> withErrorResponseHandler(
            HttpResponseHandler<? extends SdkException> errorResponseHandler) {
        this.errorResponseHandler = errorResponseHandler;
        return this;
    }

    /**
     * Non-streaming requests can use handlers that handle both error and success as a single handler instead of
     * submitting individual success and error handlers. This allows the protocol to have more control over how to
     * determine success and failure from a given HTTP response. This handler is mutually exclusive to
     * {@link #getResponseHandler()} and {@link #getErrorResponseHandler()} and an exception will be thrown if this
     * constraint is violated.
     */
    public HttpResponseHandler<Response<OutputT>> getCombinedResponseHandler() {
        return combinedResponseHandler;
    }

    public ClientExecutionParams<InputT, OutputT> withCombinedResponseHandler(
            HttpResponseHandler<Response<OutputT>> combinedResponseHandler) {
        this.combinedResponseHandler = combinedResponseHandler;
        return this;
    }

    public RequestBody getRequestBody() {
        return requestBody;
    }

    public ClientExecutionParams<InputT, OutputT> withRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public AsyncRequestBody getAsyncRequestBody() {
        return asyncRequestBody;
    }

    public ClientExecutionParams<InputT, OutputT> withAsyncRequestBody(AsyncRequestBody asyncRequestBody) {
        this.asyncRequestBody = asyncRequestBody;
        return this;
    }

    public boolean isFullDuplex() {
        return fullDuplex;
    }

    /**
     * Sets whether the API is a full duplex ie, request and response are streamed in parallel.
     */
    public ClientExecutionParams<InputT, OutputT> withFullDuplex(boolean fullDuplex) {
        this.fullDuplex = fullDuplex;
        return this;
    }

    public String getOperationName() {
        return operationName;
    }

    /**
     * Sets the operation name of the API.
     */
    public ClientExecutionParams<InputT, OutputT> withOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    public String hostPrefixExpression() {
        return hostPrefixExpression;
    }

    /**
     * Sets the resolved host prefix expression that will be added as a prefix to the original endpoint.
     * This value is present only if the operation is tagged with endpoint trait.
     */
    public ClientExecutionParams<InputT, OutputT> hostPrefixExpression(String hostPrefixExpression) {
        this.hostPrefixExpression = hostPrefixExpression;
        return this;
    }

    public URI discoveredEndpoint() {
        return discoveredEndpoint;
    }

    public ClientExecutionParams<InputT, OutputT> discoveredEndpoint(URI discoveredEndpoint) {
        this.discoveredEndpoint = discoveredEndpoint;
        return this;
    }
}
