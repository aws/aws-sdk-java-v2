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

package software.amazon.awssdk.http.urlconnection;

import static software.amazon.awssdk.http.HttpStatusFamily.CLIENT_ERROR;
import static software.amazon.awssdk.http.HttpStatusFamily.SERVER_ERROR;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.READ_TIMEOUT;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;

/**
 * An implementation of {@link SdkHttpClient} that uses {@link HttpURLConnection} to communicate with the service. This is the
 * leanest synchronous client that optimizes for minimum dependencies and startup latency in exchange for having less
 * functionality than other implementations.
 *
 * <p>See software.amazon.awssdk.http.apache.ApacheHttpClient for an alternative implementation.</p>
 *
 * <p>This can be created via {@link #builder()}</p>
 */
@SdkPublicApi
public final class UrlConnectionHttpClient implements SdkHttpClient {

    private final AttributeMap options;
    private final UrlConnectionFactory connectionFactory;

    private UrlConnectionHttpClient(AttributeMap options, UrlConnectionFactory connectionFactory) {
        this.options = options;
        if (connectionFactory != null) {
            this.connectionFactory = connectionFactory;
        } else {
            this.connectionFactory = this::createDefaultConnection;
        }

    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Create a {@link HttpURLConnection} client with the default properties
     *
     * @return an {@link UrlConnectionHttpClient}
     */
    public static SdkHttpClient create() {
        return new DefaultBuilder().build();
    }

    /**
     * Use this method if you want to control the way a {@link HttpURLConnection} is created.
     * This will ignore SDK defaults like {@link SdkHttpConfigurationOption#CONNECTION_TIMEOUT}
     * and {@link SdkHttpConfigurationOption#READ_TIMEOUT}
     * @param connectionFactory a function that, given a {@link URI} will create an {@link HttpURLConnection}
     * @return an {@link UrlConnectionHttpClient}
     */
    public static SdkHttpClient create(UrlConnectionFactory connectionFactory) {
        return new UrlConnectionHttpClient(AttributeMap.empty(), connectionFactory);
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        HttpURLConnection connection = createAndConfigureConnection(request);
        return new RequestCallable(connection, request);
    }

    @Override
    public void close() {

    }

    private HttpURLConnection createAndConfigureConnection(HttpExecuteRequest request) {
        HttpURLConnection connection = connectionFactory.createConnection(request.httpRequest().getUri());
        request.httpRequest()
               .headers()
               .forEach((key, values) -> values.forEach(value -> connection.setRequestProperty(key, value)));
        invokeSafely(() -> connection.setRequestMethod(request.httpRequest().method().name()));
        if (request.contentStreamProvider().isPresent()) {
            connection.setDoOutput(true);
        }

        return connection;
    }

    private HttpURLConnection createDefaultConnection(URI uri) {
        HttpURLConnection connection = invokeSafely(() -> (HttpURLConnection) uri.toURL().openConnection());
        connection.setConnectTimeout(saturatedCast(options.get(CONNECTION_TIMEOUT).toMillis()));
        connection.setReadTimeout(saturatedCast(options.get(READ_TIMEOUT).toMillis()));
        return connection;
    }

    private static class RequestCallable implements ExecutableHttpRequest {

        private final HttpURLConnection connection;
        private final HttpExecuteRequest request;

        private RequestCallable(HttpURLConnection connection, HttpExecuteRequest request) {
            this.connection = connection;
            this.request = request;
        }

        @Override
        public HttpExecuteResponse call() throws IOException {
            connection.connect();

            request.contentStreamProvider().ifPresent(provider ->
                    invokeSafely(() -> IoUtils.copy(provider.newStream(), connection.getOutputStream())));

            int responseCode = connection.getResponseCode();
            boolean isErrorResponse = HttpStatusFamily.of(responseCode).isOneOf(CLIENT_ERROR, SERVER_ERROR);
            InputStream content = !isErrorResponse ? connection.getInputStream() : connection.getErrorStream();

            return HttpExecuteResponse.builder()
                                      .response(SdkHttpResponse.builder()
                                                           .statusCode(responseCode)
                                                           .statusText(connection.getResponseMessage())
                                                           // TODO: Don't ignore abort?
                                                           .headers(extractHeaders(connection))
                                                           .build())
                                      .responseBody(AbortableInputStream.create(content))
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

    /**
     * A builder for an instance of {@link SdkHttpClient} that uses JDKs build-in {@link java.net.URLConnection} HTTP
     * implementation. A builder can be created via {@link #builder()}.
     *
     * <pre class="brush: java">
     * SdkHttpClient httpClient = UrlConnectionHttpClient.builder()
     * .socketTimeout(Duration.ofSeconds(10))
     * .connectionTimeout(Duration.ofSeconds(1))
     * .build();
     * </pre>
     */
    public interface Builder extends SdkHttpClient.Builder<UrlConnectionHttpClient.Builder> {

        /**
         * The amount of time to wait for data to be transferred over an established, open connection before the connection is
         * timed out. A duration of 0 means infinity, and is not recommended.
         */
        Builder socketTimeout(Duration socketTimeout);

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out. A duration of 0
         * means infinity, and is not recommended.
         */
        Builder connectionTimeout(Duration connectionTimeout);
    }

    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();

        private DefaultBuilder() {
        }

        /**
         * Sets the read timeout to a specified timeout. A timeout of zero is interpreted as an infinite timeout.
         *
         * @param socketTimeout the timeout as a {@link Duration}
         * @return this object for method chaining
         */
        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            standardOptions.put(READ_TIMEOUT, socketTimeout);
            return this;
        }

        public void setSocketTimeout(Duration socketTimeout) {
            socketTimeout(socketTimeout);
        }

        /**
         * Sets the connect timeout to a specified timeout. A timeout of zero is interpreted as an infinite timeout.
         *
         * @param connectionTimeout the timeout as a {@link Duration}
         * @return this object for method chaining
         */
        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptions.put(CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }

        /**
         * Used by the SDK to create a {@link SdkHttpClient} with service-default values if no other values have been configured
         *
         * @param serviceDefaults Service specific defaults. Keys will be one of the constants defined in
         * {@link SdkHttpConfigurationOption}.
         * @return an instance of {@link SdkHttpClient}
         */
        @Override
        public SdkHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return new UrlConnectionHttpClient(standardOptions.build()
                                                              .merge(serviceDefaults)
                                                              .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS),
                                               null);
        }
    }
}
