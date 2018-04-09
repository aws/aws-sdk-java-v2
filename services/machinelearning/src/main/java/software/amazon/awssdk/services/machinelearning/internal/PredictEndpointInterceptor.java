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

package software.amazon.awssdk.services.machinelearning.internal;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.machinelearning.model.PredictRequest;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Predict calls are sent to a predictor-specific endpoint. This handler
 * extracts the PredictRequest's PredictEndpoint "parameter" and swaps it in as
 * the endpoint to send the request to.
 */
public class PredictEndpointInterceptor implements ExecutionInterceptor {

    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();
        Object originalRequest = context.request();
        if (originalRequest instanceof PredictRequest) {
            PredictRequest pr = (PredictRequest) originalRequest;
            if (pr.predictEndpoint() == null) {
                throw new SdkClientException("PredictRequest.PredictEndpoint is required!");
            }

            try {
                URI endpoint = new URI(pr.predictEndpoint());
                return request.toBuilder()
                              .protocol(endpoint.getScheme())
                              .host(endpoint.getHost())
                              .port(endpoint.getPort())
                              .encodedPath(SdkHttpUtils.appendUri(endpoint.getPath(), request.encodedPath()))
                              .build();
            } catch (URISyntaxException e) {
                throw new SdkClientException("Unable to parse PredictRequest.PredictEndpoint", e);
            }
        }
        return request;
    }

}
