/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.client;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.ServiceAdvancedConfiguration;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.async.AsyncRequestProvider;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.runtime.transform.Marshaller;

/**
 * Encapsulates parameters needed for a particular API call. Captures input and output pojo types.
 *
 * @param <InputT>  Input POJO type.
 * @param <OutputT> Output POJO type.
 */
@SdkProtectedApi
@NotThreadSafe
@ReviewBeforeRelease("Using old style withers/getters")
public class ClientExecutionParams<InputT extends SdkRequest, OutputT> {

    private InputT input;
    private AsyncRequestProvider asyncRequestProvider;
    private Marshaller<Request<InputT>, InputT> marshaller;
    private SdkHttpResponseHandler<OutputT> asyncResponseHandler;
    private HttpResponseHandler<OutputT> responseHandler;
    private HttpResponseHandler<? extends SdkBaseException> errorResponseHandler;
    private RequestConfig requestConfig;
    private ServiceAdvancedConfiguration serviceAdvancedConfiguration;

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

    public SdkHttpResponseHandler<OutputT> getAsyncResponseHandler() {
        return asyncResponseHandler;
    }

    public ClientExecutionParams<InputT, OutputT> withAsyncResponseHandler(
            SdkHttpResponseHandler<OutputT> asyncResponseHandler) {
        this.asyncResponseHandler = asyncResponseHandler;
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

    public HttpResponseHandler<? extends SdkBaseException> getErrorResponseHandler() {
        return errorResponseHandler;
    }

    public ClientExecutionParams<InputT, OutputT> withErrorResponseHandler(
            HttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {
        this.errorResponseHandler = errorResponseHandler;
        return this;
    }

    public RequestConfig getRequestConfig() {
        return requestConfig;
    }

    public ClientExecutionParams<InputT, OutputT> withRequestConfig(RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
        return this;
    }

    public AsyncRequestProvider getAsyncRequestProvider() {
        return asyncRequestProvider;
    }

    public ClientExecutionParams<InputT, OutputT> withAsyncRequestProvider(AsyncRequestProvider asyncRequestProvider) {
        this.asyncRequestProvider = asyncRequestProvider;
        return this;
    }
}
