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

package software.amazon.awssdk.http;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.internal.http.LowCopyListMap;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.ToString;
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
    private final LowCopyListMap.ForBuildable queryParameters;
    private final LowCopyListMap.ForBuildable headers;
    private final SdkHttpMethod httpMethod;
    private final ContentStreamProvider contentStreamProvider;

    private DefaultSdkHttpFullRequest(Builder builder) {
        this.protocol = standardizeProtocol(builder.protocol);
        this.host = Validate.paramNotNull(builder.host, "host");
        this.port = standardizePort(builder.port);
        this.path = standardizePath(builder.path);
        this.httpMethod = Validate.paramNotNull(builder.httpMethod, "method");
        this.contentStreamProvider = builder.contentStreamProvider;
        this.queryParameters = builder.queryParameters.forBuildable();
        this.headers = builder.headers.forBuildable();
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
        return headers.forExternalRead();
    }

    @Override
    public List<String> matchingHeaders(String header) {
        return unmodifiableList(headers.forInternalRead().getOrDefault(header, emptyList()));
    }

    @Override
    public Optional<String> firstMatchingHeader(String headerName) {
        List<String> headers = this.headers.forInternalRead().get(headerName);
        if (headers == null || headers.isEmpty()) {
            return Optional.empty();
        }

        String header = headers.get(0);
        if (StringUtils.isEmpty(header)) {
            return Optional.empty();
        }

        return Optional.of(header);
    }

    @Override
    public Optional<String> firstMatchingHeader(Collection<String> headersToFind) {
        for (String headerName : headersToFind) {
            Optional<String> header = firstMatchingHeader(headerName);
            if (header.isPresent()) {
                return header;
            }
        }

        return Optional.empty();
    }

    @Override
    public void forEachHeader(BiConsumer<? super String, ? super List<String>> consumer) {
        headers.forInternalRead().forEach((k, v) -> consumer.accept(k, Collections.unmodifiableList(v)));
    }

    @Override
    public void forEachRawQueryParameter(BiConsumer<? super String, ? super List<String>> consumer) {
        queryParameters.forInternalRead().forEach((k, v) -> consumer.accept(k, Collections.unmodifiableList(v)));
    }

    @Override
    public int numHeaders() {
        return headers.forInternalRead().size();
    }

    @Override
    public int numRawQueryParameters() {
        return queryParameters.forInternalRead().size();
    }

    @Override
    public Optional<String> encodedQueryParameters() {
        return SdkHttpUtils.encodeAndFlattenQueryParameters(queryParameters.forInternalRead());
    }

    @Override
    public Optional<String> encodedQueryParametersAsFormData() {
        return SdkHttpUtils.encodeAndFlattenFormData(queryParameters.forInternalRead());
    }

    @Override
    public String encodedPath() {
        return path;
    }

    @Override
    public Map<String, List<String>> rawQueryParameters() {
        return queryParameters.forExternalRead();
    }

    @Override
    public Optional<String> firstMatchingRawQueryParameter(String key) {
        List<String> values = queryParameters.forInternalRead().get(key);
        return values == null ? Optional.empty() : values.stream().findFirst();
    }

    @Override
    public Optional<String> firstMatchingRawQueryParameter(Collection<String> keys) {
        for (String key : keys) {
            Optional<String> result = firstMatchingRawQueryParameter(key);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public List<String> firstMatchingRawQueryParameters(String key) {
        List<String> values = queryParameters.forInternalRead().get(key);
        return values == null ? emptyList() : unmodifiableList(values);
    }

    @Override
    public SdkHttpMethod method() {
        return httpMethod;
    }

    @Override
    public Optional<ContentStreamProvider> contentStreamProvider() {
        return Optional.ofNullable(contentStreamProvider);
    }

    @Override
    public SdkHttpFullRequest.Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultSdkHttpFullRequest")
                       .add("httpMethod", httpMethod)
                       .add("protocol", protocol)
                       .add("host", host)
                       .add("port", port)
                       .add("encodedPath", path)
                       .add("headers", headers.forInternalRead().keySet())
                       .add("queryParameters", queryParameters.forInternalRead().keySet())
                       .build();
    }

    /**
     * Builder for a {@link DefaultSdkHttpFullRequest}.
     */
    static final class Builder implements SdkHttpFullRequest.Builder {
        private String protocol;
        private String host;
        private Integer port;
        private String path;
        private LowCopyListMap.ForBuilder queryParameters;
        private LowCopyListMap.ForBuilder headers;
        private SdkHttpMethod httpMethod;
        private ContentStreamProvider contentStreamProvider;

        Builder() {
            queryParameters = LowCopyListMap.emptyQueryParameters();
            headers = LowCopyListMap.emptyHeaders();
        }

        Builder(DefaultSdkHttpFullRequest request) {
            queryParameters = request.queryParameters.forBuilder();
            headers = request.headers.forBuilder();
            protocol = request.protocol;
            host = request.host;
            port = request.port;
            path = request.path;
            httpMethod = request.httpMethod;
            contentStreamProvider = request.contentStreamProvider;
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
        public DefaultSdkHttpFullRequest.Builder putRawQueryParameter(String paramName, List<String> paramValues) {
            this.queryParameters.forInternalWrite().put(paramName, new ArrayList<>(paramValues));
            return this;
        }

        @Override
        public SdkHttpFullRequest.Builder appendRawQueryParameter(String paramName, String paramValue) {
            this.queryParameters.forInternalWrite().computeIfAbsent(paramName, k -> new ArrayList<>()).add(paramValue);
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder rawQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters.setFromExternal(queryParameters);
            return this;
        }

        @Override
        public Builder removeQueryParameter(String paramName) {
            this.queryParameters.forInternalWrite().remove(paramName);
            return this;
        }

        @Override
        public Builder clearQueryParameters() {
            this.queryParameters.forInternalWrite().clear();
            return this;
        }

        @Override
        public Map<String, List<String>> rawQueryParameters() {
            return CollectionUtils.unmodifiableMapOfLists(queryParameters.forInternalRead());
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
        public DefaultSdkHttpFullRequest.Builder putHeader(String headerName, List<String> headerValues) {
            this.headers.forInternalWrite().put(headerName, new ArrayList<>(headerValues));
            return this;
        }

        @Override
        public SdkHttpFullRequest.Builder appendHeader(String headerName, String headerValue) {
            this.headers.forInternalWrite().computeIfAbsent(headerName, k -> new ArrayList<>()).add(headerValue);
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder headers(Map<String, List<String>> headers) {
            this.headers.setFromExternal(headers);
            return this;
        }

        @Override
        public SdkHttpFullRequest.Builder removeHeader(String headerName) {
            this.headers.forInternalWrite().remove(headerName);
            return this;
        }

        @Override
        public SdkHttpFullRequest.Builder clearHeaders() {
            this.headers.clear();
            return this;
        }

        @Override
        public Map<String, List<String>> headers() {
            return CollectionUtils.unmodifiableMapOfLists(this.headers.forInternalRead());
        }

        @Override
        public List<String> matchingHeaders(String header) {
            return unmodifiableList(headers.forInternalRead().getOrDefault(header, emptyList()));
        }

        @Override
        public Optional<String> firstMatchingHeader(String headerName) {
            List<String> headers = this.headers.forInternalRead().get(headerName);
            if (headers == null || headers.isEmpty()) {
                return Optional.empty();
            }

            String header = headers.get(0);
            if (StringUtils.isEmpty(header)) {
                return Optional.empty();
            }

            return Optional.of(header);
        }

        @Override
        public Optional<String> firstMatchingHeader(Collection<String> headersToFind) {
            for (String headerName : headersToFind) {
                Optional<String> header = firstMatchingHeader(headerName);
                if (header.isPresent()) {
                    return header;
                }
            }

            return Optional.empty();
        }

        @Override
        public void forEachHeader(BiConsumer<? super String, ? super List<String>> consumer) {
            headers.forInternalRead().forEach((k, v) -> consumer.accept(k, unmodifiableList(v)));
        }

        @Override
        public void forEachRawQueryParameter(BiConsumer<? super String, ? super List<String>> consumer) {
            queryParameters.forInternalRead().forEach((k, v) -> consumer.accept(k, unmodifiableList(v)));
        }

        @Override
        public int numHeaders() {
            return headers.forInternalRead().size();
        }

        @Override
        public int numRawQueryParameters() {
            return queryParameters.forInternalRead().size();
        }

        @Override
        public Optional<String> encodedQueryParameters() {
            return SdkHttpUtils.encodeAndFlattenQueryParameters(queryParameters.forInternalRead());
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder contentStreamProvider(ContentStreamProvider contentStreamProvider) {
            this.contentStreamProvider = contentStreamProvider;
            return this;
        }

        @Override
        public ContentStreamProvider contentStreamProvider() {
            return contentStreamProvider;
        }

        @Override
        public SdkHttpFullRequest.Builder copy() {
            return build().toBuilder();
        }

        @Override
        public SdkHttpFullRequest.Builder applyMutation(Consumer<SdkHttpRequest.Builder> mutator) {
            mutator.accept(this);
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest build() {
            return new DefaultSdkHttpFullRequest(this);
        }
    }

}
