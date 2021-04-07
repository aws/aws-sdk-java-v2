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

package software.amazon.awssdk.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricPublisher;
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
    private final Duration apiCallTimeout;
    private final Duration apiCallAttemptTimeout;
    private final Signer signer;
    private final List<MetricPublisher> metricPublishers;

    protected RequestOverrideConfiguration(Builder<?> builder) {
        this.headers = CollectionUtils.deepUnmodifiableMap(builder.headers(), () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        this.rawQueryParameters = CollectionUtils.deepUnmodifiableMap(builder.rawQueryParameters());
        this.apiNames = Collections.unmodifiableList(new ArrayList<>(builder.apiNames()));
        this.apiCallTimeout = Validate.isPositiveOrNull(builder.apiCallTimeout(), "apiCallTimeout");
        this.apiCallAttemptTimeout = Validate.isPositiveOrNull(builder.apiCallAttemptTimeout(), "apiCallAttemptTimeout");
        this.signer = builder.signer();
        this.metricPublishers = Collections.unmodifiableList(new ArrayList<>(builder.metricPublishers()));
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
     * The amount of time to allow the client to complete the execution of an API call. This timeout covers the entire client
     * execution except for marshalling. This includes request handler execution, all HTTP requests including retries,
     * unmarshalling, etc. This value should always be positive, if present.
     *
     * <p>The api call timeout feature doesn't have strict guarantees on how quickly a request is aborted when the
     * timeout is breached. The typical case aborts the request within a few milliseconds but there may occasionally be
     * requests that don't get aborted until several seconds after the timer has been breached. Because of this, the client
     * execution timeout feature should not be used when absolute precision is needed.
     *
     * <p>This may be used together with {@link #apiCallAttemptTimeout()} to enforce both a timeout on each individual HTTP
     * request (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'api call' time).
     *
     * @see Builder#apiCallTimeout(Duration)
     */
    public Optional<Duration> apiCallTimeout() {
        return Optional.ofNullable(apiCallTimeout);
    }

    /**
     * The amount of time to wait for the http request to complete before giving up and timing out. This value should always be
     * positive, if present.
     *
     * <p>The request timeout feature doesn't have strict guarantees on how quickly a request is aborted when the timeout is
     * breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that
     * don't get aborted until several seconds after the timer has been breached. Because of this, the request timeout
     * feature should not be used when absolute precision is needed.
     *
     * <p>This may be used together with {@link #apiCallTimeout()} to enforce both a timeout on each individual HTTP
     * request
     * (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'api call' time).
     *
     * @see Builder#apiCallAttemptTimeout(Duration)
     */
    public Optional<Duration> apiCallAttemptTimeout() {
        return Optional.ofNullable(apiCallAttemptTimeout);
    }

    /**
     * @return the signer for signing the request. This signer get priority over the signer set on the client while
     * signing the requests. If this value is not set, then the client level signer is used for signing the request.
     */
    public Optional<Signer> signer() {
        return Optional.ofNullable(signer);
    }

    /**
     * Return the metric publishers for publishing the metrics collected for this request. This list supersedes the
     * metric publishers set on the client.
     */
    public List<MetricPublisher> metricPublishers() {
        return metricPublishers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RequestOverrideConfiguration that = (RequestOverrideConfiguration) o;
        return Objects.equals(headers, that.headers) &&
               Objects.equals(rawQueryParameters, that.rawQueryParameters) &&
               Objects.equals(apiNames, that.apiNames) &&
               Objects.equals(apiCallTimeout, that.apiCallTimeout) &&
               Objects.equals(apiCallAttemptTimeout, that.apiCallAttemptTimeout) &&
               Objects.equals(signer, that.signer) &&
               Objects.equals(metricPublishers, that.metricPublishers);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(headers);
        hashCode = 31 * hashCode + Objects.hashCode(rawQueryParameters);
        hashCode = 31 * hashCode + Objects.hashCode(apiNames);
        hashCode = 31 * hashCode + Objects.hashCode(apiCallTimeout);
        hashCode = 31 * hashCode + Objects.hashCode(apiCallAttemptTimeout);
        hashCode = 31 * hashCode + Objects.hashCode(signer);
        hashCode = 31 * hashCode + Objects.hashCode(metricPublishers);
        return hashCode;
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
         * <p>
         * This overrides any values for the given header set on the request by default by the SDK, as well as header
         * overrides set at the client level using
         * {@link software.amazon.awssdk.core.client.config.ClientOverrideConfiguration}.
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
         * <p>
         * This overrides any values for the given header set on the request by default by the SDK, as well as header
         * overrides set at the client level using
         * {@link software.amazon.awssdk.core.client.config.ClientOverrideConfiguration}.
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
         * <p>
         * This overrides any values for the given headers set on the request by default by the SDK, as well as header
         * overrides set at the client level using
         * {@link software.amazon.awssdk.core.client.config.ClientOverrideConfiguration}.
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
         * Configure the amount of time to allow the client to complete the execution of an API call. This timeout covers the
         * entire client execution except for marshalling. This includes request handler execution, all HTTP requests including
         * retries, unmarshalling, etc. This value should always be positive, if present.
         *
         * <p>The api call timeout feature doesn't have strict guarantees on how quickly a request is aborted when the
         * timeout is breached. The typical case aborts the request within a few milliseconds but there may occasionally be
         * requests that don't get aborted until several seconds after the timer has been breached. Because of this, the client
         * execution timeout feature should not be used when absolute precision is needed.
         *
         * <p>This may be used together with {@link #apiCallAttemptTimeout()} to enforce both a timeout on each individual HTTP
         * request (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'api call' time).
         *
         * @see RequestOverrideConfiguration#apiCallTimeout()
         */
        B apiCallTimeout(Duration apiCallTimeout);

        Duration apiCallTimeout();

        /**
         * Configure the amount of time to wait for the http request to complete before giving up and timing out. This value
         * should always be positive, if present.
         *
         * <p>The request timeout feature doesn't have strict guarantees on how quickly a request is aborted when the timeout is
         * breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that
         * don't get aborted until several seconds after the timer has been breached. Because of this, the request timeout
         * feature should not be used when absolute precision is needed.
         *
         * <p>This may be used together with {@link #apiCallTimeout()} to enforce both a timeout on each individual HTTP
         * request (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'api call' time).
         *
         * @see RequestOverrideConfiguration#apiCallAttemptTimeout()
         */
        B apiCallAttemptTimeout(Duration apiCallAttemptTimeout);

        Duration apiCallAttemptTimeout();

        /**
         * Sets the signer to use for signing the request. This signer get priority over the signer set on the client while
         * signing the requests. If this value is null, then the client level signer is used for signing the request.
         *
         * @param signer Signer for signing the request
         * @return This object for method chaining
         */
        B signer(Signer signer);

        Signer signer();

        /**
         * Sets the metric publishers for publishing the metrics collected for this request. This list supersedes
         * the metric publisher set on the client.
         *
         * @param metricPublisher The list metric publisher for this request.
         * @return This object for method chaining.
         */
        B metricPublishers(List<MetricPublisher> metricPublisher);

        /**
         * Add a metric publisher to the existing list of previously set publishers to be used for publishing metrics
         * for this request.
         *
         * @param metricPublisher The metric publisher to add.
         */
        B addMetricPublisher(MetricPublisher metricPublisher);

        List<MetricPublisher> metricPublishers();

        /**
         * Create a new {@code SdkRequestOverrideConfiguration} with the properties set on this builder.
         *
         * @return The new {@code SdkRequestOverrideConfiguration}.
         */
        RequestOverrideConfiguration build();
    }

    protected abstract static class BuilderImpl<B extends Builder> implements Builder<B> {
        private Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private Map<String, List<String>> rawQueryParameters = new HashMap<>();
        private List<ApiName> apiNames = new ArrayList<>();
        private Duration apiCallTimeout;
        private Duration apiCallAttemptTimeout;
        private Signer signer;
        private List<MetricPublisher> metricPublishers = new ArrayList<>();

        protected BuilderImpl() {
        }

        protected BuilderImpl(RequestOverrideConfiguration sdkRequestOverrideConfig) {
            headers(sdkRequestOverrideConfig.headers);
            rawQueryParameters(sdkRequestOverrideConfig.rawQueryParameters);
            sdkRequestOverrideConfig.apiNames.forEach(this::addApiName);
        }

        @Override
        public Map<String, List<String>> headers() {
            return CollectionUtils.unmodifiableMapOfLists(headers);
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
            return CollectionUtils.unmodifiableMapOfLists(rawQueryParameters);
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

        @Override
        public B apiCallTimeout(Duration apiCallTimeout) {
            this.apiCallTimeout = apiCallTimeout;
            return (B) this;
        }

        public void setApiCallTimeout(Duration apiCallTimeout) {
            apiCallTimeout(apiCallTimeout);
        }

        @Override
        public Duration apiCallTimeout() {
            return apiCallTimeout;
        }

        @Override
        public B apiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
            this.apiCallAttemptTimeout = apiCallAttemptTimeout;
            return (B) this;
        }

        public void setApiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
            apiCallAttemptTimeout(apiCallAttemptTimeout);
        }

        @Override
        public Duration apiCallAttemptTimeout() {
            return apiCallAttemptTimeout;
        }

        @Override
        public B signer(Signer signer) {
            this.signer = signer;
            return (B) this;
        }

        public void setSigner(Signer signer) {
            signer(signer);
        }

        @Override
        public Signer signer() {
            return signer;
        }

        @Override
        public B metricPublishers(List<MetricPublisher> metricPublishers) {
            Validate.paramNotNull(metricPublishers, "metricPublishers");
            this.metricPublishers = new ArrayList<>(metricPublishers);
            return (B) this;
        }

        @Override
        public B addMetricPublisher(MetricPublisher metricPublisher) {
            Validate.paramNotNull(metricPublisher, "metricPublisher");
            this.metricPublishers.add(metricPublisher);
            return (B) this;
        }

        public void setMetricPublishers(List<MetricPublisher> metricPublishers) {
            metricPublishers(metricPublishers);
        }

        @Override
        public List<MetricPublisher> metricPublishers() {
            return metricPublishers;
        }
    }
}
