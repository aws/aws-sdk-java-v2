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
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;

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
        this.headers = builder.headers();
        this.rawQueryParameters = builder.rawQueryParameters();
        this.apiNames = builder.apiNames();
    }

    /**
     * Optional additional headers to be added to the HTTP request.
     *
     * @return The optional additional headers.
     */
    public Optional<Map<String, List<String>>> headers() {
        return Optional.ofNullable(headers);
    }

    /**
     * Optional additional query parameters to be added to the HTTP request.
     *
     * @return The optional additional query parameters.
     */
    public Optional<Map<String, List<String>>> rawQueryParameters() {
        return Optional.ofNullable(rawQueryParameters);
    }

    /**
     * The optional names of the higher level libraries that constructed the request.
     *
     * @return The names of the libraries.
     */
    public Optional<List<ApiName>> apiNames() {
        return Optional.ofNullable(apiNames);
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
         * Add an additional header to be set on the HTTP request.
         *
         * @param name The name of the header.
         * @param value The value of the header.
         *
         * @return This object for method chaining.
         */
        default B header(String name, String value) {
            header(name, Collections.singletonList(value));
            return (B) this;
        }

        /**
         * Add additional headers to be set on the HTTP request.
         *
         * @param name The name of the header.
         * @param values The values of the header.
         *
         * @return This object for method chaining.
         */
        B header(String name, List<String> values);

        /**
         * Add additional headers to be set on the HTTP request.
         *
         * @param headers The set of additional headers.
         *
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
         * Add an additional query parameter to be set on the HTTP request.
         *
         * @param name The query parameter name.
         * @param value The query parameter value.
         *
         * @return This object for method chaining.
         */
        default B rawQueryParameter(String name, String value) {
            rawQueryParameter(name, Collections.singletonList(value));
            return (B) this;
        }

        /**
         * Add an additional query parameter to be set on the HTTP request.
         *
         * @param name The query parameter name.
         * @param values The query parameter values.
         *
         * @return This object for method chaining.
         */
        B rawQueryParameter(String name, List<String> values);

        /**
         * Add additional query parameters to be set on the HTTP request.
         *
         * @param rawQueryParameters The set of additional query parameters.
         *
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
        private Map<String, List<String>> headers;

        private Map<String, List<String>> rawQueryParameters;

        private List<ApiName> apiNames;

        protected BuilderImpl() {
        }

        protected BuilderImpl(RequestOverrideConfiguration sdkRequestOverrideConfig) {
            sdkRequestOverrideConfig.headers().ifPresent(this::headers);
            sdkRequestOverrideConfig.rawQueryParameters().ifPresent(this::rawQueryParameters);
            sdkRequestOverrideConfig.apiNames().ifPresent(apiNames -> apiNames.forEach(this::addApiName));
        }

        @Override
        public Map<String, List<String>> headers() {
            return headers;
        }

        @Override
        public B header(String name, List<String> values) {
            if (headers == null) {
                headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            }
            headers.put(name, new ArrayList<>(values));
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B headers(Map<String, List<String>> headers) {
            if (headers == null) {
                this.headers = null;
            } else {
                headers.forEach(this::header);
            }
            return (B) this;
        }

        @Override
        public Map<String, List<String>> rawQueryParameters() {
            return rawQueryParameters;
        }

        @Override
        public B rawQueryParameter(String name, List<String> values) {
            if (rawQueryParameters == null) {
                rawQueryParameters = new HashMap<>();
            }
            rawQueryParameters.put(name, new ArrayList<>(values));
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B rawQueryParameters(Map<String, List<String>> rawQueryParameters) {
            if (rawQueryParameters == null) {
                this.rawQueryParameters = null;
            } else {
                rawQueryParameters.forEach(this::rawQueryParameter);
            }
            return (B) this;
        }

        @Override
        public List<ApiName> apiNames() {
            return apiNames;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B addApiName(ApiName apiName) {
            if (apiNames == null) {
                this.apiNames = new ArrayList<>();
            }
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
