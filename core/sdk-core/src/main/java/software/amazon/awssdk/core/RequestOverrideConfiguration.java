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

package software.amazon.awssdk.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Base per-request override configuration for all SDK requests.
 */
@Immutable
@SdkPublicApi
public abstract class RequestOverrideConfiguration {

    private final Map<String, List<String>> headers;

    private final Map<String, List<String>> rawQueryParameters;

    private final List<ApiName> apiNames;

    protected RequestOverrideConfiguration(Builder<?> builder) {
        this.headers = CollectionUtils.deepUnmodifiableMap(builder.headers(), () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        this.rawQueryParameters = CollectionUtils.deepUnmodifiableMap(builder.rawQueryParameters());
        this.apiNames = Collections.unmodifiableList(new ArrayList<>(builder.apiNames()));
    }

    /**
     * Optional additional headers to be added to the HTTP request.
     *
     * @return The optional additional headers.
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Optional additional query parameters to be added to the HTTP request.
     *
     * @return The optional additional query parameters.
     */
    public Map<String, List<String>> rawQueryParameters() {
        return rawQueryParameters;
    }

    /**
     * The optional names of the higher level libraries that constructed the request.
     *
     * @return The names of the libraries.
     */
    public List<ApiName> apiNames() {
        return apiNames;
    }

    /**
     * Create a {@link Builder} initialized with the properties of this {@code SdkRequestOverrideConfiguration}.
     *
     * @return A new builder intialized with this config's properties.
     */
    public abstract Builder<? extends Builder> toBuilder();

    public interface Builder<B extends Builder> {
        /**
         * Optional additional headers to be added to the HTTP request.
         *
         * @return The optional additional headers.
         */
        Map<String, List<String>> headers();

        /**
         * Add a single header to be set on the HTTP request.
         *
         * <p>
         * This overrides any values already configured with this header name in the builder.
         *
         * @param name The name of the header.
         * @param value The value of the header.
         * @return This object for method chaining.
         */
        default B putHeader(String name, String value) {
            putHeader(name, Collections.singletonList(value));
            return (B) this;
        }

        /**
         * Add a single header with multiple values to be set on the HTTP request.
         *
         * <p>
         * This overrides any values already configured with this header name in the builder.
         *
         * @param name The name of the header.
         * @param values The values of the header.
         * @return This object for method chaining.
         */
        B putHeader(String name, List<String> values);

        /**
         * Add additional headers to be set on the HTTP request.
         *
         * <p>
         * This completely overrides any values currently configured in the builder.
         *
         * @param headers The set of additional headers.
         * @return This object for method chaining.
         */
        B headers(Map<String, List<String>> headers);

        /**
         * Optional additional query parameters to be added to the HTTP request.
         *
         * @return The optional additional query parameters.
         */
        Map<String, List<String>> rawQueryParameters();

        /**
         * Add a single query parameter to be set on the HTTP request.
         *
         * <p>
         * This overrides any values already configured with this query name in the builder.
         *
         * @param name The query parameter name.
         * @param value The query parameter value.
         * @return This object for method chaining.
         */
        default B putRawQueryParameter(String name, String value) {
            putRawQueryParameter(name, Collections.singletonList(value));
            return (B) this;
        }

        /**
         * Add a single query parameter with multiple values to be set on the HTTP request.
         *
         * <p>
         * This overrides any values already configured with this query name in the builder.
         *
         * @param name The query parameter name.
         * @param values The query parameter values.
         * @return This object for method chaining.
         */
        B putRawQueryParameter(String name, List<String> values);

        /**
         * Configure query parameters to be set on the HTTP request.
         *
         * <p>
         * This completely overrides any query parameters currently configured in the builder.
         *
         * @param rawQueryParameters The set of additional query parameters.
         * @return This object for method chaining.
         */
        B rawQueryParameters(Map<String, List<String>> rawQueryParameters);

        /**
         * The optional names of the higher level libraries that constructed the request.
         *
         * @return The names of the libraries.
         */
        List<ApiName> apiNames();

        /**
         * Set the optional name of the higher level library that constructed the request.
         *
         * @param apiName The name of the library.
         *
         * @return This object for method chaining.
         */
        B addApiName(ApiName apiName);

        /**
         * Set the optional name of the higher level library that constructed the request.
         *
         * @param apiNameConsumer A {@link Consumer} that accepts a {@link ApiName.Builder}.
         *
         * @return This object for method chaining.
         */
        B addApiName(Consumer<ApiName.Builder> apiNameConsumer);

        /**
         * Create a new {@code SdkRequestOverrideConfiguration} with the properties set on this builder.
         *
         * @return The new {@code SdkRequestOverrideConfiguration}.
         */
        RequestOverrideConfiguration build();
    }

    protected abstract static class BuilderImpl<B extends Builder> implements Builder<B> {
        private Map<String, List<String>> headers = new HashMap<>();

        private Map<String, List<String>> rawQueryParameters = new HashMap<>();

        private List<ApiName> apiNames = new ArrayList<>();

        protected BuilderImpl() {
        }

        protected BuilderImpl(RequestOverrideConfiguration sdkRequestOverrideConfig) {
            headers(sdkRequestOverrideConfig.headers);
            rawQueryParameters(sdkRequestOverrideConfig.rawQueryParameters);
            sdkRequestOverrideConfig.apiNames.forEach(this::addApiName);
        }

        @Override
        public Map<String, List<String>> headers() {
            return CollectionUtils.deepUnmodifiableMap(headers, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        }

        @Override
        public B putHeader(String name, List<String> values) {
            Validate.paramNotNull(values, "values");
            headers.put(name, new ArrayList<>(values));
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B headers(Map<String, List<String>> headers) {
            Validate.paramNotNull(headers, "headers");
            this.headers = CollectionUtils.deepCopyMap(headers);
            return (B) this;
        }

        @Override
        public Map<String, List<String>> rawQueryParameters() {
            return CollectionUtils.deepUnmodifiableMap(rawQueryParameters);
        }

        @Override
        public B putRawQueryParameter(String name, List<String> values) {
            Validate.paramNotNull(name, "name");
            Validate.paramNotNull(values, "values");
            rawQueryParameters.put(name, new ArrayList<>(values));
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B rawQueryParameters(Map<String, List<String>> rawQueryParameters) {
            Validate.paramNotNull(rawQueryParameters, "rawQueryParameters");
            this.rawQueryParameters = CollectionUtils.deepCopyMap(rawQueryParameters);
            return (B) this;
        }

        @Override
        public List<ApiName> apiNames() {
            return Collections.unmodifiableList(apiNames);
        }

        @Override
        @SuppressWarnings("unchecked")
        public B addApiName(ApiName apiName) {
            this.apiNames.add(apiName);
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B addApiName(Consumer<ApiName.Builder> apiNameConsumer) {
            ApiName.Builder b = ApiName.builder();
            apiNameConsumer.accept(b);
            addApiName(b.build());
            return (B) this;
        }
    }
}
