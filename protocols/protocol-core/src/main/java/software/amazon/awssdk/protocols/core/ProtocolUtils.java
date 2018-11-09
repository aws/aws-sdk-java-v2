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

package software.amazon.awssdk.protocols.core;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.StringUtils;

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

        return request.encodedPath(concatPaths(request.encodedPath(),
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

    /**
     * Concats two paths together.
     *
     * @param pathOne First part of path.
     * @param pathTwo Second part of path.
     * @return Concatenated paths.
     */
    private static String concatPaths(String pathOne, String pathTwo) {
        if (pathTwo == null) {
            return pathOne;
        } else if (pathOne == null) {
            return pathTwo;
        } else {
            return stripTrailingSlash(pathOne) + "/" + stripLeadingSlash(pathTwo);
        }
    }

    /**
     * Strips any trailing slash from a path.
     *
     * @param path Path to strip trailing slash from.
     * @return Path without trailing slash or original path if there is no trailing slash.
     */
    private static String stripTrailingSlash(String path) {
        if (StringUtils.isEmpty(path)) {
            return path;
        }
        if (path.charAt(path.length() - 1) == '/') {
            return path.substring(0, path.length() - 1);
        } else {
            return path;
        }
    }

    /**
     * Strips any leading slash from a path.
     *
     * @param path Path to strip leading slash from.
     * @return Path without leading slash or original path if there is no leading slash.
     */
    private static String stripLeadingSlash(String path) {
        if (StringUtils.isEmpty(path)) {
            return path;
        }
        if (path.charAt(0) == '/') {
            return path.substring(1, path.length());
        } else {
            return path;
        }
    }
}
