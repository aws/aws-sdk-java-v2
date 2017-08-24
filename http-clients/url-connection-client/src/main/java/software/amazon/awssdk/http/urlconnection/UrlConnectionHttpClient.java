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

package software.amazon.awssdk.http.urlconnection;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.SOCKET_TIMEOUT;
import static software.amazon.awssdk.utils.CollectionUtils.toMap;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;
import static software.amazon.awssdk.utils.StringUtils.isNotBlank;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;

@SdkInternalApi
final class UrlConnectionHttpClient implements SdkHttpClient {

    private final AttributeMap options;

    UrlConnectionHttpClient(AttributeMap options) {
        this.options = options;
    }

    @Override
    public AbortableCallable<SdkHttpFullResponse> prepareRequest(SdkHttpFullRequest request, SdkRequestContext requestContext) {
        final HttpURLConnection connection = createAndConfigureConnection(request);
        return new RequestCallable(connection, request);
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        return Optional.ofNullable(options.get(key));
    }

    @Override
    public void close() {

    }

    private HttpURLConnection createAndConfigureConnection(SdkHttpFullRequest request) {
        HttpURLConnection connection = invokeSafely(() -> (HttpURLConnection) createRequest(request).toURL().openConnection());
        request.getHeaders().forEach((key, values) -> values.forEach(value -> connection.setRequestProperty(key, value)));
        invokeSafely(() -> connection.setRequestMethod(request.getHttpMethod().name()));
        if (request.getContent() != null) {
            connection.setDoOutput(true);
        }

        connection.setConnectTimeout(saturatedCast(options.get(CONNECTION_TIMEOUT).toMillis()));
        connection.setReadTimeout(saturatedCast(options.get(SOCKET_TIMEOUT).toMillis()));

        return connection;
    }

    private URI createRequest(SdkHttpFullRequest request) {
        StringBuilder uriBuilder = new StringBuilder(request.getEndpoint().toString());
        if (isNotBlank(request.getResourcePath())) {
            uriBuilder.append(request.getResourcePath());
        }

        String params = request.getParameters().entrySet().stream()
                               .flatMap(this::flattenParams)
                               .collect(Collectors.joining("&"));

        if (isNotBlank(params)) {
            uriBuilder.append("?").append(params);
        }

        return invokeSafely(() -> new URI(uriBuilder.toString()));
    }

    private Stream<String> flattenParams(Map.Entry<String, List<String>> e) {
        if (e.getValue() == null || e.getValue().size() == 0 || e.getValue().get(0) == null) {
            return Stream.of(encode(e.getKey()));
        }

        return e.getValue().stream().map(v -> encode(e.getKey()) + "=" + encode(v));
    }

    private static String encode(String string) {
        return invokeSafely(() -> URLEncoder.encode(string, StandardCharsets.UTF_8.name()));
    }

    private static class RequestCallable implements AbortableCallable<SdkHttpFullResponse> {

        private final HttpURLConnection connection;
        private final SdkHttpFullRequest request;

        private RequestCallable(HttpURLConnection connection, SdkHttpFullRequest request) {
            this.connection = connection;
            this.request = request;
        }

        @Override
        public SdkHttpFullResponse call() throws Exception {
            connection.connect();

            if (request.getContent() != null) {
                IoUtils.copy(request.getContent(), connection.getOutputStream());
            }

            int responseCode = connection.getResponseCode();
            InputStream content = responseCode < 400 ? connection.getInputStream() : connection.getErrorStream();
            return SdkHttpFullResponse.builder()
                                      .statusCode(responseCode)
                                      .statusText(connection.getResponseMessage())
                                      .content(content)
                                      .headers(extractHeaders(connection))
                                      .build();
        }

        private Map<String, List<String>> extractHeaders(HttpURLConnection response) {
            return response.getHeaderFields().entrySet().stream()
                           .filter(e -> e.getKey() != null)
                           .collect(toMap());
        }

        @Override
        public void abort() {
            connection.disconnect();
        }
    }
}
