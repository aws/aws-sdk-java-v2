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

import static software.amazon.awssdk.utils.CollectionUtils.deepUnmodifiableLinkedMap;
import static software.amazon.awssdk.utils.CollectionUtils.deepUnmodifiableMap;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Internal implementation of {@link SdkHttpFullRequest}. Provided to HTTP implement to execute a request.
 */
@SdkInternalApi
public class DefaultSdkHttpFullRequest implements SdkHttpFullRequest {

    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final String resourcePath;
    private final Map<String, List<String>> queryParameters = new LinkedHashMap<>();
    private final URI endpoint;
    private final SdkHttpMethod httpMethod;
    private final InputStream content;
    private final AttributeMap handlerContextKeys;

    private DefaultSdkHttpFullRequest(Builder builder) {
        this.headers.putAll(builder.headers);
        this.queryParameters.putAll(builder.queryParameters);
        this.resourcePath = builder.resourcePath;
        this.endpoint = builder.endpoint;
        this.httpMethod = builder.httpMethod;
        this.content = builder.content;
        this.handlerContextKeys = builder.handlerContextKeys.build();
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return deepUnmodifiableMap(headers);
    }

    @Override
    public Collection<String> getValuesForHeader(String header) {
        List<String> values = headers.get(header);
        return values != null ? Collections.unmodifiableList(values) : Collections.emptyList();
    }

    @Override
    public String getResourcePath() {
        return resourcePath;
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return deepUnmodifiableLinkedMap(queryParameters);
    }

    @Override
    public URI getEndpoint() {
        return endpoint;
    }

    @Override
    public SdkHttpMethod getHttpMethod() {
        return httpMethod;
    }

    @Override
    public InputStream getContent() {
        return content;
    }

    @Override
    public <T> T handlerContext(HandlerContextKey<T> key) {
        return handlerContextKeys.get(key);
    }

    @Override
    public Builder toBuilder() {
        return new Builder(handlerContextKeys.toBuilder())
                .headers(headers)
                .resourcePath(resourcePath)
                .httpMethod(httpMethod)
                .endpoint(endpoint)
                .queryParameters(queryParameters)
                .content(content);
    }

    /**
     * @return Builder instance to construct a {@link DefaultSdkHttpFullRequest}.
     */
    public static Builder builder() {
        return new Builder(AttributeMap.builder());
    }

    /**
     * Builder for a {@link DefaultSdkHttpFullRequest}.
     */
    public static final class Builder implements SdkHttpFullRequest.Builder {

        private final Map<String, List<String>> headers = new HashMap<>();
        private String resourcePath;
        @ReviewBeforeRelease("Do we need linked hash map here?")
        private Map<String, List<String>> queryParameters = new LinkedHashMap<>();
        private URI endpoint;
        private SdkHttpMethod httpMethod;
        private InputStream content;
        private final AttributeMap.Builder handlerContextKeys;

        private Builder(AttributeMap.Builder handlerContextKeys) {
            this.handlerContextKeys = handlerContextKeys;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder header(String key, List<String> values) {
            this.headers.put(key, new ArrayList<>(values));
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder headers(Map<String, List<String>> headers) {
            headers.forEach((k, v) -> this.headers.put(k, new ArrayList<>(v)));
            return this;
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return deepUnmodifiableMap(headers);
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder resourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }

        @Override
        public String getResourcePath() {
            return resourcePath;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder queryParameter(String paramName, List<String> paramValues) {
            this.queryParameters.put(paramName, new ArrayList<>(paramValues));
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder queryParameters(Map<String, List<String>> queryParameters) {
            queryParameters.forEach((k, v) -> this.queryParameters.put(k, new ArrayList<>(v)));
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
        public Map<String, List<String>> getParameters() {
            return deepUnmodifiableLinkedMap(queryParameters);
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public URI getEndpoint() {
            return endpoint;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder httpMethod(SdkHttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        @Override
        public SdkHttpMethod getHttpMethod() {
            return httpMethod;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder content(InputStream content) {
            this.content = content;
            return this;
        }

        @Override
        public InputStream getContent() {
            return content;
        }

        @Override
        public <T> DefaultSdkHttpFullRequest.Builder handlerContext(HandlerContextKey<T> key, T value) {
            handlerContextKeys.put(key, value);
            return this;
        }

        @Override
        public <T> T handlerContext(HandlerContextKey<T> key) {
            return handlerContextKeys.get(key);
        }

        /**
         * @return An immutable {@link DefaultSdkHttpFullRequest} object.
         */
        @Override
        public DefaultSdkHttpFullRequest build() {
            return new DefaultSdkHttpFullRequest(this);
        }
    }

}
