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

import static software.amazon.awssdk.http.HttpStatusFamily.CLIENT_ERROR;
import static software.amazon.awssdk.http.HttpStatusFamily.SERVER_ERROR;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.SOCKET_TIMEOUT;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpStatusFamily;
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
        HttpURLConnection connection = invokeSafely(() -> (HttpURLConnection) request.getUri().toURL().openConnection());
        request.headers().forEach((key, values) -> values.forEach(value -> connection.setRequestProperty(key, value)));
        invokeSafely(() -> connection.setRequestMethod(request.method().name()));
        if (request.content().isPresent()) {
            connection.setDoOutput(true);
        }

        connection.setConnectTimeout(saturatedCast(options.get(CONNECTION_TIMEOUT).toMillis()));
        connection.setReadTimeout(saturatedCast(options.get(SOCKET_TIMEOUT).toMillis()));

        return connection;
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

            request.content().ifPresent(content -> invokeSafely(() -> IoUtils.copy(content, connection.getOutputStream())));

            int responseCode = connection.getResponseCode();
            boolean isErrorResponse = HttpStatusFamily.of(responseCode).isOneOf(CLIENT_ERROR, SERVER_ERROR);
            InputStream content = !isErrorResponse ? connection.getInputStream() : connection.getErrorStream();

            return SdkHttpFullResponse.builder()
                                      .statusCode(responseCode)
                                      .statusText(connection.getResponseMessage())
                                      .content(new AbortableInputStream(content, () -> { /* TODO: Don't ignore abort? */ }))
                                      .headers(extractHeaders(connection))
                                      .build();
        }

        private Map<String, List<String>> extractHeaders(HttpURLConnection response) {
            return response.getHeaderFields().entrySet().stream()
                           .filter(e -> e.getKey() != null)
                           .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public void abort() {
            connection.disconnect();
        }
    }
}
