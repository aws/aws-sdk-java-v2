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

package software.amazon.awssdk.services.machinelearning.internal;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.interceptor.Context;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.interceptor.Priority;
import software.amazon.awssdk.services.machinelearning.model.PredictRequest;

/**
 * Predict calls are sent to a predictor-specific endpoint. This handler
 * extracts the PredictRequest's PredictEndpoint "parameter" and swaps it in as
 * the endpoint to send the request to.
 */
public class PredictEndpointInterceptor implements ExecutionInterceptor {
    @Override
    public Priority priority() {
        return Priority.SERVICE;
    }

    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();
        Object originalRequest = context.request();
        if (originalRequest instanceof PredictRequest) {
            PredictRequest pr = (PredictRequest) originalRequest;
            if (pr.predictEndpoint() == null) {
                throw new AmazonClientException("PredictRequest.PredictEndpoint is required!");
            }

            try {
                return request.toBuilder()
                              .endpoint(new URI(pr.predictEndpoint()))
                              .build();
            } catch (URISyntaxException e) {
                throw new AmazonClientException("Unable to parse PredictRequest.PredictEndpoint", e);
            }
        }
        return request;
    }

}
