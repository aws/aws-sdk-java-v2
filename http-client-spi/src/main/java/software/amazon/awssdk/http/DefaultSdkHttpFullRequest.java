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
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Internal implementation of {@link SdkHttpFullRequest}. Provided to HTTP implement to execute a request.
 */
@SdkInternalApi
class DefaultSdkHttpFullRequest implements SdkHttpFullRequest {

    private final Map<String, List<String>> headers;
    private final String resourcePath;
    private final Map<String, List<String>> queryParameters;
    private final URI endpoint;
    private final SdkHttpMethod httpMethod;
    private final InputStream content;

    private DefaultSdkHttpFullRequest(Builder builder) {
        this.headers = deepUnmodifiableMap(builder.headers, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        this.queryParameters = deepUnmodifiableMap(builder.queryParameters, () -> new LinkedHashMap<>());
        this.resourcePath = builder.resourcePath;
        this.endpoint = builder.endpoint;
        this.httpMethod = builder.httpMethod;
        this.content = builder.content;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public Collection<String> getValuesForHeader(String header) {
        return headers.getOrDefault(header, Collections.emptyList());
    }

    @Override
    public String getResourcePath() {
        return resourcePath;
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return queryParameters;
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
    public Builder toBuilder() {
        return new Builder()
                .headers(headers)
                .resourcePath(resourcePath)
                .httpMethod(httpMethod)
                .endpoint(endpoint)
                .queryParameters(queryParameters)
                .content(content);
    }

    /**
     * Builder for a {@link DefaultSdkHttpFullRequest}.
     */
    static final class Builder implements SdkHttpFullRequest.Builder {

        private Map<String, List<String>> headers = new HashMap<>();
        private String resourcePath;
        @ReviewBeforeRelease("Do we need linked hash map here?")
        private Map<String, List<String>> queryParameters = new LinkedHashMap<>();
        private URI endpoint;
        private SdkHttpMethod httpMethod;
        private InputStream content;

        Builder() {
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder header(String key, List<String> values) {
            this.headers.put(key, new ArrayList<>(values));
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder headers(Map<String, List<String>> headers) {
            this.headers = CollectionUtils.deepCopyMap(headers);
            return this;
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return CollectionUtils.deepUnmodifiableMap(this.headers);
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder resourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }

        @Override
        public String getResourcePath() {
            return this.resourcePath;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder queryParameter(String paramName, List<String> paramValues) {
            this.queryParameters.put(paramName, new ArrayList<>(paramValues));
            return this;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder queryParameters(Map<String, List<String>> queryParameters) {
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
        public Map<String, List<String>> getParameters() {
            return CollectionUtils.deepUnmodifiableMap(this.queryParameters);
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public URI getEndpoint() {
            return this.endpoint;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder httpMethod(SdkHttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        @Override
        public SdkHttpMethod getHttpMethod() {
            return this.httpMethod;
        }

        @Override
        public DefaultSdkHttpFullRequest.Builder content(InputStream content) {
            this.content = content;
            return this;
        }

        @Override
        public InputStream getContent() {
            return this.content;
        }

        /**
         * @return An immutable {@link DefaultSdkHttpFullRequest} object.
         */
        @Override
        public DefaultSdkHttpFullRequest build() {
            return new DefaultSdkHttpFullRequest(this);
        }

        @Override
        @ReviewBeforeRelease("This is why we shouldn't extend the request in the builder.")
        public Builder toBuilder() {
            throw new UnsupportedOperationException();
        }
    }

}
