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

package software.amazon.awssdk.core.client.handler;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.runtime.transform.Marshaller;

/**
 * Encapsulates parameters needed for a particular API call. Captures input and output pojo types.
 *
 * @param <InputT>  Input POJO type.
 * @param <OutputT> Output POJO type.
 */
@SdkProtectedApi
@NotThreadSafe
@ReviewBeforeRelease("Using old style withers/getters")
public final class ClientExecutionParams<InputT extends SdkRequest, OutputT> {

    private InputT input;
    private AsyncRequestBody asyncRequestBody;
    private Marshaller<Request<InputT>, InputT> marshaller;
    private HttpResponseHandler<OutputT> responseHandler;
    private HttpResponseHandler<? extends SdkException> errorResponseHandler;

    public Marshaller<Request<InputT>, InputT> getMarshaller() {
        return marshaller;
    }

    public ClientExecutionParams<InputT, OutputT> withMarshaller(
            Marshaller<Request<InputT>, InputT> marshaller) {
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

    public AsyncRequestBody getAsyncRequestBody() {
        return asyncRequestBody;
    }

    public ClientExecutionParams<InputT, OutputT> withAsyncRequestBody(AsyncRequestBody asyncRequestBody) {
        this.asyncRequestBody = asyncRequestBody;
        return this;
    }
}
