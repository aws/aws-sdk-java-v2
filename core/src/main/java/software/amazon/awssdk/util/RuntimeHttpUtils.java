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

package software.amazon.awssdk.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.config.AdvancedClientOption;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpFullRequest;

public class RuntimeHttpUtils {
    private static final String COMMA = ", ";
    private static final String SPACE = " ";

    private static final String AWS_EXECUTION_ENV_PREFIX = "exec-env/";

    public static String getUserAgent(ClientConfiguration config, final String userAgentMarker) {
        ClientOverrideConfiguration overrideConfig = config.overrideConfiguration();
        String userDefinedPrefix = overrideConfig.advancedOption(AdvancedClientOption.USER_AGENT_PREFIX);
        String userDefinedSuffix = overrideConfig.advancedOption(AdvancedClientOption.USER_AGENT_SUFFIX);
        String awsExecutionEnvironment = AwsSystemSetting.AWS_EXECUTION_ENV.getStringValue().orElse(null);

        StringBuilder userAgent = new StringBuilder(software.amazon.awssdk.utils.StringUtils.trimToEmpty(userDefinedPrefix));

        if (!VersionInfoUtils.getUserAgent().equals(userDefinedPrefix)) {
            userAgent.append(COMMA).append(VersionInfoUtils.getUserAgent());
        }

        if (StringUtils.hasValue(userDefinedSuffix)) {
            userAgent.append(COMMA).append(userDefinedSuffix.trim());
        }

        if (StringUtils.hasValue(awsExecutionEnvironment)) {
            userAgent.append(SPACE).append(AWS_EXECUTION_ENV_PREFIX).append(awsExecutionEnvironment.trim());
        }

        if (StringUtils.hasValue(userAgentMarker)) {
            userAgent.append(SPACE).append(userAgentMarker.trim());
        }

        return userAgent.toString();
    }

    /**
     * Returns an URI for the given endpoint.
     * Prefixes the protocol if the endpoint given does not have it.
     *
     * @throws IllegalArgumentException if the inputs are null.
     */
    public static URI toUri(String endpoint, Protocol protocol) {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint cannot be null");
        }

        /*
         * If the endpoint doesn't explicitly specify a protocol to use, then
         * we'll defer to the default protocol specified in the client
         * configuration.
         */
        if (!endpoint.contains("://")) {
            endpoint = protocol.toString() + "://" + endpoint;
        }

        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Converts the specified request object into a URL, containing all the specified parameters, the specified request endpoint,
     * etc.
     *
     * @param request                          The request to convert into a URL.
     * @param removeLeadingSlashInResourcePath Whether the leading slash in resource-path should be removed before appending to
     *                                         the endpoint.
     * @param urlEncode                        True if request resource path should be URL encoded
     * @return A new URL representing the specified request.
     * @throws SdkClientException If the request cannot be converted to a well formed URL.
     */
    @SdkProtectedApi
    @Deprecated
    public static URL convertRequestToUrl(Request<?> request,
                                          boolean removeLeadingSlashInResourcePath,
                                          boolean urlEncode) {
        String resourcePath = urlEncode ?
                SdkHttpUtils.urlEncode(request.getResourcePath(), true)
                : request.getResourcePath();

        // Removed the padding "/" that was already added into the request's resource path.
        if (removeLeadingSlashInResourcePath
            && resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        // Some http client libraries (e.g. Apache HttpClient) cannot handle
        // consecutive "/"s between URL authority and path components.
        // So we escape "////..." into "/%2F%2F%2F...", in the same way as how
        // we treat consecutive "/"s in AmazonS3Client#presign(...)

        String urlPath = "/" + resourcePath;
        urlPath = urlPath.replaceAll("(?<=/)/", "%2F");
        StringBuilder url = new StringBuilder(request.getEndpoint().toString());
        url.append(urlPath);

        StringBuilder queryParams = new StringBuilder();
        Map<String, List<String>> requestParams = request.getParameters();
        for (Map.Entry<String, List<String>> entry : requestParams.entrySet()) {
            for (String value : entry.getValue()) {
                queryParams = queryParams.length() > 0 ? queryParams
                        .append("&") : queryParams.append("?");
                queryParams.append(SdkHttpUtils.urlEncode(entry.getKey(), false))
                           .append("=")
                           .append(SdkHttpUtils.urlEncode(value, false));
            }
        }
        url.append(queryParams.toString());

        try {
            return new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new SdkClientException(
                    "Unable to convert request to well formed URL: " + e.getMessage(), e);
        }
    }

    /**
     * Converts the specified request object into a URL, containing all the specified parameters, the specified request endpoint,
     * etc.
     *
     * @param request                          The request to convert into a URL.
     * @param removeLeadingSlashInResourcePath Whether the leading slash in resource-path should be removed before appending to
     *                                         the endpoint.
     * @param urlEncode                        True if request resource path should be URL encoded
     * @return A new URL representing the specified request.
     * @throws SdkClientException If the request cannot be converted to a well formed URL.
     */
    @SdkProtectedApi
    public static URL convertRequestToUrl(SdkHttpFullRequest request,
                                          boolean removeLeadingSlashInResourcePath,
                                          boolean urlEncode) {
        String resourcePath = urlEncode ?
                SdkHttpUtils.urlEncode(request.getResourcePath(), true)
                : request.getResourcePath();

        // Removed the padding "/" that was already added into the request's resource path.
        if (removeLeadingSlashInResourcePath
            && resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        // Some http client libraries (e.g. Apache HttpClient) cannot handle
        // consecutive "/"s between URL authority and path components.
        // So we escape "////..." into "/%2F%2F%2F...", in the same way as how
        // we treat consecutive "/"s in AmazonS3Client#presign(...)

        String urlPath = "/" + resourcePath;
        urlPath = urlPath.replaceAll("(?<=/)/", "%2F");
        StringBuilder url = new StringBuilder(request.getEndpoint().toString());
        url.append(urlPath);

        StringBuilder queryParams = new StringBuilder();
        Map<String, List<String>> requestParams = request.getParameters();
        for (Map.Entry<String, List<String>> entry : requestParams.entrySet()) {
            for (String value : entry.getValue()) {
                queryParams = queryParams.length() > 0 ? queryParams
                        .append("&") : queryParams.append("?");
                queryParams.append(SdkHttpUtils.urlEncode(entry.getKey(), false))
                           .append("=")
                           .append(SdkHttpUtils.urlEncode(value, false));
            }
        }
        url.append(queryParams.toString());

        try {
            return new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new SdkClientException(
                    "Unable to convert request to well formed URL: " + e.getMessage(), e);
        }
    }
}
