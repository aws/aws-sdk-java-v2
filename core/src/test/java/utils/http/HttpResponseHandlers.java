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

package utils.http;

import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.utils.IoUtils;

public class HttpResponseHandlers {

    public static HttpResponseHandler<AmazonWebServiceResponse<String>> stringResponseHandler() {
        return responseHandler(in -> IoUtils.toString(in.getContent()));
    }

    public static <T> HttpResponseHandler<AmazonWebServiceResponse<T>> responseHandler(
            final FunctionWithException<HttpResponse, T> handle) {
        return (response, executionAttributes) -> {
            AmazonWebServiceResponse<T> resp = new AmazonWebServiceResponse<T>();
            resp.setResult(handle.apply(response));
            return resp;
        };
    }

    interface FunctionWithException<Input, Output> {
        Output apply(Input in) throws Exception;
    }
}
