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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.utils.CollectionUtils.deepUnmodifiableMap;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Internal implementation of {@link SdkHttpFullRequest}, buildable via {@link SdkHttpFullRequest#builder()}. Provided to HTTP
 * implementation to execute a request.
 */
@SdkInternalApi
@Immutable
final class DefaultSdkHttpFullRequest implements SdkHttpFullRequest {
    private final String protocol;
    private final String host;
    private final Integer port;
    private final String path;
    private final Map<String, List<String>> queryParameters;
    private final SdkHttpMethod httpMethod;
    private final Map<String, List<String>> headers;
    private final InputStream content;

    private DefaultSdkHttpFullRequest(Builder builder) {
        this.protocol = standardizeProtocol(builder.protocol);
        this.host = Validate.paramNotNull(builder.host, "host");
        this.port = standardizePort(builder.port);
        this.path = standardizePath(builder.path);
        this.queryParameters = deepUnmodifiableMap(builder.queryParameters, () -> new LinkedHashMap<>());
        this.httpMethod = Validate.paramNotNull(builder.httpMethod, "method");
        this.headers = deepUnmodifiableMap(builder.headers, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        this.content = builder.content;
    }

    private String standardizeProtocol(String protocol) {
        Validate.paramNotNull(protocol, "protocol");

        String standardizedProtocol = StringUtils.lowerCase(protocol);
        Validate.isTrue(standardizedProtocol.equals("http") || standardizedProtocol.equals("https"),
                        "Protocol must be 'http' or 'https', but was %s", protocol);

        return standardizedProtocol;
    }

    private String standardizePath(String path) {
        if (StringUtils.isEmpty(path)) {
            return "";
        }

        StringBuilder standardizedPath = new StringBuilder();

        // Path must always start with '/'
        if (!path.startsWith("/")) {
            standardizedPath.append('/');
        }

        standardizedPath.append(path);

        return standardizedPath.toString();
    }

    private Integer standardizePort(Integer port) {
        Validate.isTrue(port == null || port >= -1,
                        "Port must be positive (or null/-1 to indicate no port), but was '%s'", port);

        if (port != null && port == -1) {
            return null;
        }

        return port;
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int port() {
        return Optional.ofNullable(port).orElseGet(() -> SdkHttpUtils.standardPort(protocol()));
    }

    @Override
    public Map<String, List<String>> headers() {
        return headers;
    }

    @Override
    public String encodedPath() {
        return path;
    }

    @Override
    public Map<String, List<String>> rawQueryParameters() {
        return queryParameters;
    }

    @Override
    public SdkHttpMethod method() {
        return httpMethod;
    }

    @Override
    public Optional<InputStream> content() {
        return Optional.ofNullable(content);
    }

    @Override
    public SdkHttpFullRequest.Builder toBuilder() {
        return new Builder()
                .protocol(protocol)
                .host(host)
                .port(port)
                .encodedPath(path)
                .rawQueryParameters(queryParameters)
                .method(httpMethod)
                .headers(headers)
                .content(content);
    }

    /**
     * Builder for a {@link DefaultSdkHttpFullRequest}.
     */
    static final class Builder implements SdkHttpFullRequest.Builder {
        private String protocol;
        private String host;
        private Integer port;
        private String path;
        private Map<String, List<String>> queryParameters = new LinkedHashMap<>();
        private SdkHttpMethod httpMethod;
        private Map<String, List<String>> headers = new LinkedHashMap<>();
        private InputStream content;

        Builder() {
        }

        @Override
        public String protocol() {
            return protocol;
        }

        @Override
        public SdkHttpFullRequest.Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        @Override
        public String host() {
            return host;
        }

        @Override
        public SdkHttpFullRequest.Builder host(String host) {
            this.host = host;
            return this;
        }

        @Override
        public Integer port() {
            return port;
        }

        @Override
        public SdkHttpFullRequest.Builder port(Integer port) {
            this.port = port;
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder encodedPath(String path) {
            this.path = path;
            return this;
        }

        @Override
        public String encodedPath() {
            return path;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder rawQueryParameter(String paramName, List<String> paramValues) {
            this.queryParameters.put(paramName, new ArrayList<>(paramValues));
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder rawQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters = CollectionUtils.deepCopyMap(queryParameters, () -> new LinkedHashMap<>());
            return this;
        }

        @Override
        public Builder removeQueryParameter(String paramName) {
            this.queryParameters.remove(paramName);
            return this;
        }

        @Override
        public Builder clearQueryParameters() {
            this.queryParameters.clear();
            return this;
        }

        @Override
        public Map<String, List<String>> rawQueryParameters() {
            return CollectionUtils.deepUnmodifiableMap(this.queryParameters, () -> new LinkedHashMap<>());
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder method(SdkHttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        @Override
        public SdkHttpMethod method() {
            return httpMethod;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder header(String headerName, List<String> headerValues) {
            this.headers.put(headerName, new ArrayList<>(headerValues));
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder headers(Map<String, List<String>> headers) {
            this.headers = CollectionUtils.deepCopyMap(headers);
            return this;
        }

        @Override
        public SdkHttpFullRequest.Builder removeHeader(String headerName) {
            this.headers.remove(headerName);
            return this;
        }

        @Override
        public SdkHttpFullRequest.Builder clearHeaders() {
            this.headers.clear();
            return this;
        }

        @Override
        public Map<String, List<String>> headers() {
            return CollectionUtils.deepUnmodifiableMap(this.headers);
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder content(InputStream content) {
            this.content = content;
            return this;
        }

        @Override
        public InputStream content() {
            return content;
        }

        @Override
        public DefaultSdkHttpFullRequest build() {
            return new DefaultSdkHttpFullRequest(this);
        }
    }

}
