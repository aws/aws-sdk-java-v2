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

package software.amazon.awssdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.retry.RetryPolicy;
import software.amazon.awssdk.util.ValidationUtils;
import software.amazon.awssdk.util.VersionInfoUtils;

/**
 * Client configuration options such as proxy settings, user agent string, max retry attempts, etc.
 *
 * @deprecated Replaced with {@link ClientConfiguration}.
 */
@NotThreadSafe
@Deprecated
public class LegacyClientConfiguration {

    /**
     * The default timeout for a request. This is disabled by default.
     */
    public static final int DEFAULT_CLIENT_EXECUTION_TIMEOUT = 0;

    /** The default max connection pool size. */
    public static final int DEFAULT_MAX_CONNECTIONS = 50;

    /** The default HTTP user agent header for AWS Java SDK clients. */
    public static final String DEFAULT_USER_AGENT = VersionInfoUtils.getUserAgent();

    /**
     * Default request retry policy, including the maximum retry count of 3, the default retry
     * condition and the default back-off strategy.
     *
     * @see PredefinedRetryPolicies#DEFAULT
     * @see PredefinedRetryPolicies#DYNAMODB_DEFAULT
     */
    public static final RetryPolicy DEFAULT_RETRY_POLICY = PredefinedRetryPolicies.DEFAULT;

    /**
     * The default on whether to use gzip compression.
     */
    public static final boolean DEFAULT_USE_GZIP = false;

    /**
     * The default on whether to throttle retries.
     */
    public static final boolean DEFAULT_THROTTLE_RETRIES = true;

    public static final int DEFAULT_MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING = 100;

    /** A prefix to the HTTP user agent header passed with all HTTP requests.  */
    private String userAgentPrefix = DEFAULT_USER_AGENT;
    /** A suffix to the HTTP user agent header. */
    private String userAgentSuffix;
    /**
     * The maximum number of times that a retryable failed request (ex: a 5xx response from a
     * service) will be retried. Or -1 if the user has not explicitly set this value, in which case
     * the configured RetryPolicy will be used to control the retry count.
     */
    private int maxErrorRetry = -1;
    /** The retry policy upon failed requests. **/
    private RetryPolicy retryPolicy = DEFAULT_RETRY_POLICY;

    /**
     * The protocol to use when connecting to Amazon Web Services.
     * <p>
     * The default configuration is to use HTTPS for all requests for increased security.
     */
    private Protocol protocol = Protocol.HTTPS;
    /** The maximum number of open HTTP connections. */
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private int clientExecutionTimeout = DEFAULT_CLIENT_EXECUTION_TIMEOUT;
    private boolean throttleRetries = DEFAULT_THROTTLE_RETRIES;
    /**
     * Optional whether to use gzip compression when making HTTP requests.
     */
    private boolean useGzip = DEFAULT_USE_GZIP;
    /**
     * Optional override to control which signature algorithm should be used to sign requests to the
     * service. If not explicitly set, the client will determine the algorithm to use by inspecting
     * a configuration file baked in to the SDK.
     */
    private String signerOverride;
    /**
     * Headers to be added to all requests
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * The maximum number of throttled retries if the initial request
     * fails.
     */
    private int maxConsecutiveRetriesBeforeThrottling = DEFAULT_MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING;

    /**
     * Create a legacy client configuration object with default options.
     */
    public LegacyClientConfiguration() {
    }

    /**
     * Create a copy of another legacy client configuration object.
     */
    public LegacyClientConfiguration(LegacyClientConfiguration other) {
        this.maxConnections = other.maxConnections;
        this.maxErrorRetry = other.maxErrorRetry;
        this.retryPolicy = other.retryPolicy;
        this.throttleRetries = other.throttleRetries;
        this.protocol = other.protocol;
        this.clientExecutionTimeout = other.clientExecutionTimeout;
        this.userAgentPrefix = other.userAgentPrefix;
        this.userAgentSuffix = other.userAgentSuffix;
        this.useGzip = other.useGzip;
        this.signerOverride = other.signerOverride;
        this.headers.clear();
        this.headers.putAll(other.headers);
        this.maxConsecutiveRetriesBeforeThrottling = other.maxConsecutiveRetriesBeforeThrottling;
    }

    /**
     * Returns the protocol (HTTP or HTTPS) to use when connecting to Amazon Web Services.
     * <p>
     * The default configuration is to use HTTPS for all requests for increased security.
     * <p>
     * Individual clients can also override this setting by explicitly including the protocol as
     * part of the endpoint URL when calling {@link AmazonWebServiceClient#setEndpoint(String)}.
     *
     * @return The protocol to use when connecting to Amazon Web Services.
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol (i.e. HTTP or HTTPS) to use when connecting to Amazon Web Services.
     * <p>
     * The default configuration is to use HTTPS for all requests for increased security.
     * <p>
     * Individual clients can also override this setting by explicitly including the protocol as
     * part of the endpoint URL when calling {@link AmazonWebServiceClient#setEndpoint(String)}.
     *
     * @param protocol
     *            The protocol to use when connecting to Amazon Web Services.
     */
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Sets the protocol (i.e. HTTP or HTTPS) to use when connecting to Amazon Web Services, and
     * returns the updated ClientConfiguration object so that additional calls may be chained
     * together.
     * <p>
     * The default configuration is to use HTTPS for all requests for increased security.
     * <p>
     * Individual clients can also override this setting by explicitly including the protocol as
     * part of the endpoint URL when calling {@link AmazonWebServiceClient#setEndpoint(String)}.
     *
     * @param protocol
     *            The protocol to use when connecting to Amazon Web Services.
     * @return The updated ClientConfiguration object with the new max HTTP connections setting.
     */
    public LegacyClientConfiguration withProtocol(Protocol protocol) {
        setProtocol(protocol);
        return this;
    }

    /**
     * @deprecated Replaced by {@link #getUserAgentPrefix()} and {@link #getUserAgentSuffix()}
     * @return The user agent string to use when sending requests.
     */
    @Deprecated
    public String getUserAgent() {
        return getUserAgentPrefix();
    }

    /**
     * @deprecated Replaced by {@link #setUserAgentPrefix(String)} and {@link #setUserAgentSuffix(String)}
     * @param userAgent
     *            The user agent string to use when sending requests.
     */
    @Deprecated
    public void setUserAgent(String userAgent) {
        setUserAgentPrefix(userAgent);
    }

    /**
     * @deprecated Replaced by {@link #withUserAgentPrefix(String)} and {@link #withUserAgentSuffix(String)}
     * @param userAgent
     *            The user agent string to use when sending requests.
     * @return The updated ClientConfiguration object.
     */
    @Deprecated
    public LegacyClientConfiguration withUserAgent(String userAgent) {
        return withUserAgentPrefix(userAgent);
    }

    /**
     * Returns the HTTP user agent header prefix to send with all requests.
     *
     * @return The user agent string prefix to use when sending requests.
     */
    public String getUserAgentPrefix() {
        return userAgentPrefix;
    }

    /**
     * Sets the HTTP user agent prefix to send with all requests.
     *
     * @param prefix
     *            The string to prefix to user agent to use when sending requests.
     */
    public void setUserAgentPrefix(String prefix) {
        this.userAgentPrefix = prefix;
    }

    /**
     * Sets the HTTP user agent prefix header used in requests and returns the updated ClientConfiguration
     * object.
     *
     * @param prefix
     *            The string to prefix to user agent to use when sending requests.
     * @return The updated ClientConfiguration object.
     */
    public LegacyClientConfiguration withUserAgentPrefix(String prefix) {
        setUserAgentPrefix(prefix);
        return this;
    }

    /**
     * Returns the HTTP user agent header suffix to add to the end of the user agent header on all requests.
     *
     * @return The user agent string suffix to use when sending requests.
     */
    public String getUserAgentSuffix() {
        return userAgentSuffix;
    }

    /**
     * Sets the HTTP user agent suffix to send with all requests.
     *
     * @param suffix
     *            The string to suffix to user agent to use when sending requests.
     */
    public void setUserAgentSuffix(String suffix) {
        this.userAgentSuffix = suffix;
    }

    /**
     * Sets the HTTP user agent suffix header used in requests and returns the updated ClientConfiguration
     * object.
     *
     * @param suffix
     *            The string to suffix to user agent to use when sending requests.
     * @return The updated ClientConfiguration object.
     */
    public LegacyClientConfiguration withUserAgentSuffix(String suffix) {
        setUserAgentSuffix(suffix);
        return this;
    }

    /**
     * Returns the retry policy upon failed requests.
     *
     * @return The retry policy upon failed requests.
     */
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    /**
     * Sets the retry policy upon failed requests. User could specify whether the RetryPolicy should
     * honor maxErrorRetry set by {@link #setMaxErrorRetry(int)}.
     *
     * @param retryPolicy
     *            The retry policy upon failed requests.
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    /**
     * Sets the retry policy upon failed requests, and returns the updated ClientConfiguration
     * object. User could specify whether the RetryPolicy should honor maxErrorRetry set by
     * {@link #setMaxErrorRetry(int)}
     *
     * @param retryPolicy
     *            The retry policy upon failed requests.
     */
    public LegacyClientConfiguration withRetryPolicy(RetryPolicy retryPolicy) {
        setRetryPolicy(retryPolicy);
        return this;
    }

    /**
     * Returns the maximum number of retry attempts for failed retryable requests (ex: 5xx error
     * responses from a service). This method returns -1 before a maxErrorRetry value is explicitly
     * set by {@link #setMaxErrorRetry(int)}, in which case the configured RetryPolicy will be used
     * to control the retry count.
     *
     * @return The maximum number of retry attempts for failed retryable requests, or -1 if
     *         maxErrorRetry has not been set by {@link #setMaxErrorRetry(int)}.
     */
    public int getMaxErrorRetry() {
        return maxErrorRetry;
    }

    /**
     * Sets the maximum number of retry attempts for failed retryable requests (ex: 5xx error
     * responses from services).
     *
     * @param maxErrorRetry
     *            The maximum number of retry attempts for failed retryable requests. This value
     *            should not be negative.
     */
    public void setMaxErrorRetry(int maxErrorRetry) {
        if (maxErrorRetry < 0) {
            throw new IllegalArgumentException("maxErrorRetry shoud be non-negative");
        }
        this.maxErrorRetry = maxErrorRetry;
    }

    /**
     * Sets the maximum number of retry attempts for failed retryable requests (ex: 5xx error
     * responses from services), and returns the updated ClientConfiguration object.
     *
     * @param maxErrorRetry
     *            The maximum number of retry attempts for failed retryable requests. This value
     *            should not be negative.
     * @return The updated ClientConfiguration object.
     */
    public LegacyClientConfiguration withMaxErrorRetry(int maxErrorRetry) {
        setMaxErrorRetry(maxErrorRetry);
        return this;
    }

    /**
     * Returns the amount of time (in milliseconds) to allow the client to complete the execution of
     * an API call. This timeout covers the entire client execution except for marshalling. This
     * includes request handler execution, all HTTP request including retries, unmarshalling, etc.
     * <p>
     * This feature requires buffering the entire response (for non-streaming APIs) into memory to
     * enforce a hard timeout when reading the response. For APIs that return large responses this
     * could be expensive.
     * <p>
     * <p>
     * The client execution timeout feature doesn't have strict guarantees on how quickly a request
     * is aborted when the timeout is breached. The typical case aborts the request within a few
     * milliseconds but there may occasionally be requests that don't get aborted until several
     * seconds after the timer has been breached. Because of this, the client execution timeout
     * feature should not be used when absolute precision is needed.
     * </p>
     *
     * @return The amount of time (in milliseconds) to allow the client to complete the execution of
     *         an API call.
     */
    public int getClientExecutionTimeout() {
        return this.clientExecutionTimeout;
    }

    /**
     * Sets the amount of time (in milliseconds) to allow the client to complete the execution of
     * an API call. This timeout covers the entire client execution except for marshalling. This
     * includes request handler execution, all HTTP request including retries, unmarshalling, etc.
     * <p>
     * This feature requires buffering the entire response (for non-streaming APIs) into memory to
     * enforce a hard timeout when reading the response. For APIs that return large responses this
     * could be expensive.
     * <p>
     * <p>
     * The client execution timeout feature doesn't have strict guarantees on how quickly a request
     * is aborted when the timeout is breached. The typical case aborts the request within a few
     * milliseconds but there may occasionally be requests that don't get aborted until several
     * seconds after the timer has been breached. Because of this, the client execution timeout
     * feature should not be used when absolute precision is needed.
     * </p>
     *
     * @param clientExecutionTimeout
     *            The amount of time (in milliseconds) to allow the client to complete the execution
     *            of an API call. A value of '0' disables this feature.
     */
    public void setClientExecutionTimeout(int clientExecutionTimeout) {
        this.clientExecutionTimeout = clientExecutionTimeout;
    }

    /**
     * Sets the amount of time (in milliseconds) to allow the client to complete the execution of
     * an API call. This timeout covers the entire client execution except for marshalling. This
     * includes request handler execution, all HTTP request including retries, unmarshalling, etc.
     * <p>
     * This feature requires buffering the entire response (for non-streaming APIs) into memory to
     * enforce a hard timeout when reading the response. For APIs that return large responses this
     * could be expensive.
     * <p>
     * <p>
     * The client execution timeout feature doesn't have strict guarantees on how quickly a request
     * is aborted when the timeout is breached. The typical case aborts the request within a few
     * milliseconds but there may occasionally be requests that don't get aborted until several
     * seconds after the timer has been breached. Because of this, the client execution timeout
     * feature should not be used when absolute precision is needed.
     * </p>
     *
     * @param clientExecutionTimeout
     *            The amount of time (in milliseconds) to allow the client to complete the execution
     *            of an API call. A value of '0' disables this feature.
     * @return The updated ClientConfiguration object for method chaining
     */
    public LegacyClientConfiguration withClientExecutionTimeout(int clientExecutionTimeout) {
        setClientExecutionTimeout(clientExecutionTimeout);
        return this;
    }

    /**
     * Returns whether retry throttling will be used.
     * <p>
     * Retry throttling is a feature which intelligently throttles retry attempts when a
     * large percentage of requests are failing and retries are unsuccessful, particularly
     * in scenarios of degraded service health.  In these situations the client will drain its
     * internal retry capacity and slowly roll off from retry attempts until requests begin
     * to succeed again.  At that point the retry capacity pool will begin to refill and
     * retries will once again be permitted.
     * </p>
     * <p>
     * In situations where retries have been throttled this feature will effectively result in
     * fail-fast behavior from the client.  Because retries are circumvented exceptions will
     * be immediately returned to the caller if the initial request is unsuccessful.  This
     * will result in a greater number of exceptions being returned up front but prevents
     * requests being tied up attempting subsequent retries which are also likely to fail.
     * </p>
     *
     * @return true if retry throttling will be used
     */
    public boolean useThrottledRetries() {
        return throttleRetries;
    }

    /**
     * Sets whether throttled retries should be used
     * <p>
     * Retry throttling is a feature which intelligently throttles retry attempts when a
     * large percentage of requests are failing and retries are unsuccessful, particularly
     * in scenarios of degraded service health.  In these situations the client will drain its
     * internal retry capacity and slowly roll off from retry attempts until requests begin
     * to succeed again.  At that point the retry capacity pool will begin to refill and
     * retries will once again be permitted.
     * </p>
     * <p>
     * In situations where retries have been throttled this feature will effectively result in
     * fail-fast behavior from the client.  Because retries are circumvented exceptions will
     * be immediately returned to the caller if the initial request is unsuccessful.  This
     * will result in a greater number of exceptions being returned up front but prevents
     * requests being tied up attempting subsequent retries which are also likely to fail.
     * </p>
     *
     * @param use
     *            true if throttled retries should be used
     */
    public void setUseThrottleRetries(boolean use) {
        this.throttleRetries = use;
    }

    /**
     * Sets whether throttled retries should be used
     * <p>
     * Retry throttling is a feature which intelligently throttles retry attempts when a
     * large percentage of requests are failing and retries are unsuccessful, particularly
     * in scenarios of degraded service health.  In these situations the client will drain its
     * internal retry capacity and slowly roll off from retry attempts until requests begin
     * to succeed again.  At that point the retry capacity pool will begin to refill and
     * retries will once again be permitted.
     * </p>
     * <p>
     * In situations where retries have been throttled this feature will effectively result in
     * fail-fast behavior from the client.  Because retries are circumvented exceptions will
     * be immediately returned to the caller if the initial request is unsuccessful.  This
     * will result in a greater number of exceptions being returned up front but prevents
     * requests being tied up attempting subsequent retries which are also likely to fail.
     * </p>

     * @param use
     *            true if throttled retries should be used
     * @return The updated ClientConfiguration object.
     */
    public LegacyClientConfiguration withThrottledRetries(boolean use) {
        setUseThrottleRetries(use);
        return this;
    }

    /**
     * Set the maximum number of consecutive failed retries that the client will permit before
     * throttling all subsequent retries of failed requests.
     * <p>
     * Note: This does not guarantee that each failed request will be retried up to this many times.
     * Depending on the configured {@link RetryPolicy} and the number of past failed and successful
     * requests, the actual number of retries attempted may be less.
     * <p>
     * This has a default value of {@link #DEFAULT_MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING}.
     *
     * @param maxConsecutiveRetriesBeforeThrottling The maximum number of consecutive retries.
     */
    public void setMaxConsecutiveRetriesBeforeThrottling(int maxConsecutiveRetriesBeforeThrottling) {
        this.maxConsecutiveRetriesBeforeThrottling = ValidationUtils.assertIsPositive(maxConsecutiveRetriesBeforeThrottling,
                "maxConsecutiveRetriesBeforeThrottling");
    }

    /**
     * Set the maximum number of consecutive failed retries that the client will permit before
     * throttling all subsequent retries of failed requests.
     * <p>
     * Note: This does not guarantee that each failed request will be retried up to this many times.
     * Depending on the configured {@link RetryPolicy} and the number of past failed and successful
     * requests, the actual number of retries attempted may be less.
     * <p>
     * This has a default value of {@link #DEFAULT_MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING}.
     *
     * @param maxConsecutiveRetriesBeforeThrottling The maximum number of consecutive retries.
     *
     * @return This object for chaining.
     */
    public LegacyClientConfiguration withMaxConsecutiveRetriesBeforeThrottling(int maxConsecutiveRetriesBeforeThrottling) {
        setMaxConsecutiveRetriesBeforeThrottling(maxConsecutiveRetriesBeforeThrottling);
        return this;
    }

    /**
     * @return Set the maximum number of consecutive failed retries that the client will permit
     *     before throttling all subsequent retries of failed requests.
     */
    public int getMaxConsecutiveRetriesBeforeThrottling() {
        return maxConsecutiveRetriesBeforeThrottling;
    }

    /**
     * Checks if gzip compression is used
     *
     * @return if gzip compression is used
     */
    public boolean useGzip() {
        return useGzip;
    }

    /**
     * Sets whether gzip compression should be used
     *
     * @param use
     *            whether gzip compression should be used
     */
    public void setUseGzip(boolean use) {
        this.useGzip = use;
    }

    /**
     * Sets whether gzip compression should be used
     *
     * @param use
     *            whether gzip compression should be used
     * @return The updated ClientConfiguration object.
     */
    public LegacyClientConfiguration withGzip(boolean use) {
        setUseGzip(use);
        return this;
    }

    /**
     * Returns the name of the signature algorithm to use for signing requests made by this client.
     * If not set or explicitly set to null, the client will choose a signature algorithm to use
     * based on a configuration file of supported signature algorithms for the service and region.
     * <p>
     * Most users do not need to concern themselves with which signature algorithm is being used, as
     * the defaults will be sufficient. This setting exists only so advanced users can opt in to
     * newer signature protocols which have not yet been made the default for a particular
     * service/region.
     * <p>
     * Not all services support all signature algorithms, and configuring an unsupported signature
     * algorithm will lead to authentication failures. Use me at your own risk, and only after
     * consulting the documentation for the service to ensure it actually does supports your chosen
     * algorithm.
     * <p>
     * If non-null, the name returned from this method is used to look up a {@code Signer} class
     * implementing the chosen algorithm by the {@code software.amazon.awssdk.auth.SignerFactory} class.
     *
     * @return The signature algorithm to use for this client, or null to use the default.
     */
    public String getSignerOverride() {
        return signerOverride;
    }

    /**
     * Sets the name of the signature algorithm to use for signing requests made by this client. If
     * not set or explicitly set to null, the client will choose a signature algorithm to use based
     * on a configuration file of supported signature algorithms for the service and region.
     * <p>
     * Most users do not need to concern themselves with which signature algorithm is being used, as
     * the defaults will be sufficient. This setting exists only so advanced users can opt in to
     * newer signature protocols which have not yet been made the default for a particular
     * service/region.
     * <p>
     * Not all services support all signature algorithms, and configuring an unsupported signature
     * algorithm will lead to authentication failures. Use me at your own risk, and only after
     * consulting the documentation for the service to ensure it actually does supports your chosen
     * algorithm.
     * <p>
     * If non-null, the name returned from this method is used to look up a {@code Signer} class
     * implementing the chosen algorithm by the {@code software.amazon.awssdk.auth.SignerFactory} class.
     *
     * @param value
     *            The signature algorithm to use for this client, or null to use the default.
     */
    public void setSignerOverride(final String value) {
        signerOverride = value;
    }

    /**
     * Sets the name of the signature algorithm to use for signing requests made by this client. If
     * not set or explicitly set to null, the client will choose a signature algorithm to use based
     * on a configuration file of supported signature algorithms for the service and region.
     * <p>
     * Most users do not need to concern themselves with which signature algorithm is being used, as
     * the defaults will be sufficient. This setting exists only so advanced users can opt in to
     * newer signature protocols which have not yet been made the default for a particular
     * service/region.
     * <p>
     * Not all services support all signature algorithms, and configuring an unsupported signature
     * algorithm will lead to authentication failures. Use me at your own risk, and only after
     * consulting the documentation for the service to ensure it actually does supports your chosen
     * algorithm.
     * <p>
     * If non-null, the name returned from this method is used to look up a {@code Signer} class
     * implementing the chosen algorithm by the {@code software.amazon.awssdk.auth.SignerFactory} class.
     *
     * @param value
     *            The signature algorithm to use for this client, or null to use the default.
     * @return The updated ClientConfiguration object.
     */
    public LegacyClientConfiguration withSignerOverride(final String value) {
        setSignerOverride(value);
        return this;
    }

    /**
     * Adds a header to be added on all requests and returns the {@link LegacyClientConfiguration} object
     *
     * @param name
     *            the name of the header
     * @param value
     *            the value of the header
     *
     * @return The updated ClientConfiguration object.
     */
    public LegacyClientConfiguration withHeader(String name, String value) {
        addHeader(name, value);
        return this;
    }

    /**
     * Adds a header to be added on all requests
     *
     * @param name
     *            the name of the header
     * @param value
     *            the value of the header
     */
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Returns headers to be added to all requests
     *
     * @return headers to be added to all requests
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }
}
