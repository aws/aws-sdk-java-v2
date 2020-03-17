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

package software.amazon.awssdk.protocols.core;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Utilities common to all protocols.
 */
@SdkProtectedApi
public final class ProtocolUtils {

    private ProtocolUtils() {
    }

    /**
     * Creates the basic {@link SdkHttpFullRequest} with information from the {@link OperationInfo} and the endpoint.
     *
     * @param operationInfo Metadata about operation, contains HTTP method and request URI.
     * @param endpoint Endpoint of request.
     * @return Mutable {@link SdkHttpFullRequest.Builder} with HTTP method, URI, and static query parameters set.
     */
    public static SdkHttpFullRequest.Builder createSdkHttpRequest(OperationInfo operationInfo, URI endpoint) {
        SdkHttpFullRequest.Builder request = SdkHttpFullRequest
            .builder()
            .method(operationInfo.httpMethod())
            .uri(endpoint);

        return request.encodedPath(SdkHttpUtils.appendUri(request.encodedPath(),
                                                addStaticQueryParametersToRequest(request, operationInfo.requestUri())));
    }

    /**
     * Identifies the static query parameters in Uri resource path for and adds it to
     * request.
     *
     * Returns the updated uriResourcePath.
     */
    @SdkTestInternalApi
    static String addStaticQueryParametersToRequest(SdkHttpFullRequest.Builder request,
                                                    String uriResourcePath) {
        if (request == null || uriResourcePath == null) {
            return null;
        }

        String resourcePath = uriResourcePath;

        int index = resourcePath.indexOf("?");
        if (index != -1) {
            String queryString = resourcePath.substring(index + 1);
            resourcePath = resourcePath.substring(0, index);

            for (String s : queryString.split("[;&]")) {
                index = s.indexOf("=");
                if (index != -1) {
                    request.putRawQueryParameter(s.substring(0, index), s.substring(index + 1));
                } else {
                    request.putRawQueryParameter(s, (String) null);
                }
            }
        }
        return resourcePath;
    }

}
