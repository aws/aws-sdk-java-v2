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

package software.amazon.awssdk.http;

import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.handlers.AwsHandlerKeys;

/**
 * Adapts a {@link Request} to the new {@link SdkHttpFullRequest} interface.
 *
 * TODO this should eventually be removed and SdkHttpFullRequest should completely replace Request
 */
public class SdkHttpFullRequestAdapter {

    public static SdkHttpFullRequest toHttpFullRequest(Request<?> request) {
        return toMutableHttpFullRequest(request).build();
    }

    public static SdkHttpFullRequest.Builder toMutableHttpFullRequest(Request<?> request) {
        return DefaultSdkHttpFullRequest
                .builder()
                .content(request.getContent())
                .httpMethod(SdkHttpMethod.fromValue(request.getHttpMethod().name()))
                .headers(adaptHeaders(request.getHeaders()))
                .queryParameters(request.getParameters())
                .endpoint(request.getEndpoint())
                .resourcePath(request.getResourcePath())
                // TODO find a better place to set this
                .handlerContext(AwsHandlerKeys.SERVICE_NAME, request.getServiceName());
    }

    private static Map<String, List<String>> adaptHeaders(Map<String, String> headers) {
        Map<String, List<String>> adapated = new HashMap<>(headers.size());
        headers.forEach((k, v) -> adapated.put(k, singletonList(v)));
        return adapated;
    }

}
