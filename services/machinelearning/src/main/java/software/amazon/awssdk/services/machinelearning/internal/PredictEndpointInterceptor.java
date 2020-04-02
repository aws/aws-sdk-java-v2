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

package software.amazon.awssdk.services.machinelearning.internal;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.machinelearning.model.PredictRequest;

/**
 * Predict calls are sent to a predictor-specific endpoint. This handler
 * extracts the PredictRequest's PredictEndpoint "parameter" and swaps it in as
 * the endpoint to send the request to.
 */
@SdkInternalApi
public final class PredictEndpointInterceptor implements ExecutionInterceptor {

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpRequest request = context.httpRequest();
        Object originalRequest = context.request();
        if (originalRequest instanceof PredictRequest) {
            PredictRequest pr = (PredictRequest) originalRequest;
            if (pr.predictEndpoint() == null) {
                throw SdkClientException.builder().message("PredictRequest.PredictEndpoint is required!").build();
            }

            try {
                URI endpoint = new URI(pr.predictEndpoint());
                return request.toBuilder().uri(endpoint).build();
            } catch (URISyntaxException e) {
                throw SdkClientException.builder()
                                        .message("Unable to parse PredictRequest.PredictEndpoint")
                                        .cause(e)
                                        .build();
            }
        }
        return request;
    }

}
