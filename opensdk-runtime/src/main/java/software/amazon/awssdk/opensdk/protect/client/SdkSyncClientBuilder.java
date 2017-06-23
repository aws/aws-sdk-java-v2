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

package software.amazon.awssdk.opensdk.protect.client;

import java.net.BindException;
import java.net.ConnectException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.auth.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.NoOpSigner;
import software.amazon.awssdk.auth.RequestSigner;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.auth.SignerAsRequestSigner;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.loader.DefaultSdkHttpClientFactory;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.opensdk.config.ConnectionConfiguration;
import software.amazon.awssdk.opensdk.config.ProxyConfiguration;
import software.amazon.awssdk.opensdk.config.TimeoutConfiguration;
import software.amazon.awssdk.opensdk.internal.auth.IamSignerFactory;
import software.amazon.awssdk.opensdk.internal.auth.SignerProviderAdapter;
import software.amazon.awssdk.opensdk.internal.config.ApiGatewayClientConfiguration;
import software.amazon.awssdk.opensdk.internal.config.ClientConfigurationAdapter;
import software.amazon.awssdk.opensdk.protect.auth.IamRequestSigner;
import software.amazon.awssdk.opensdk.protect.auth.RequestSignerRegistry;
import software.amazon.awssdk.opensdk.retry.RetryPolicyBuilder;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.retry.v2.RetryPolicy;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.util.VersionInfoUtils;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Base class for all Open SDK client builders.
 *
 * @param <SubclassT>    Concrete builder for better fluent setters.
 * @param <TypeToBuildT> Type being built by concrete builder.
 */
public abstract class SdkSyncClientBuilder<SubclassT extends SdkSyncClientBuilder, TypeToBuildT> {

    private static final String USER_AGENT_PREFIX = "apig-java";
    private static final String UA_NAME_VERSION_SEPERATOR = "/";
    private final ApiGatewayClientConfiguration apiGatewayClientConfiguration
            = new ApiGatewayClientConfiguration();
    /*
     * Different services may have custom client configuration factories to vend defaults
     * tailored for that service.
     */
    private AwsCredentialsProvider iamCredentials;
    private String endpoint;
    private String apiKey;
    private String region = defaultRegion();
    private RetryPolicy retryPolicy;
    private RequestSignerRegistry signerRegistry = new RequestSignerRegistry();

    protected SdkSyncClientBuilder() {
    }

    protected void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    protected void setIamCredentials(AwsCredentialsProvider iamCredentials) {
        this.iamCredentials = iamCredentials;
    }

    protected void setIamRegion(String iamRegion) {
        this.region = iamRegion;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public SubclassT endpoint(String endpoint) {
        setEndpoint(endpoint);
        return getSubclass();
    }

    /**
     * Sets the optional proxy configuration of a client.
     *
     * @param proxyConfiguration The proxy configuration of the client.
     */
    public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
        apiGatewayClientConfiguration.setProxyConfiguration(proxyConfiguration);
    }

    /**
     * Sets the optional proxy configuration of a client.
     *
     * @param proxyConfiguration The proxy configuration of the client.
     * @return This object for method chaining.
     */
    public SubclassT proxyConfiguration(ProxyConfiguration proxyConfiguration) {
        setProxyConfiguration(proxyConfiguration);
        return getSubclass();
    }

    /**
     * Sets the optional timeouts used by the client.
     *
     * @param timeoutConfiguration The {@link TimeoutConfiguration} object with the custom timeouts.
     */
    public void setTimeoutConfiguration(TimeoutConfiguration timeoutConfiguration) {
        apiGatewayClientConfiguration.setTimeoutConfiguration(timeoutConfiguration);
    }

    /**
     * Sets the optional timeouts used by the client.
     *
     * @param timeoutConfiguration The {@link TimeoutConfiguration} object with the custom timeouts.
     * @return This object for method chaining.
     */
    public SubclassT timeoutConfiguration(TimeoutConfiguration timeoutConfiguration) {
        setTimeoutConfiguration(timeoutConfiguration);
        return getSubclass();
    }

    /**
     * Sets various optional options related to the http connection pool and connections.
     *
     * @param connectionConfiguration The {@link ConnectionConfiguration} object with the custom values.
     */
    public void setConnectionConfiguration(ConnectionConfiguration connectionConfiguration) {
        apiGatewayClientConfiguration.setConnectionConfiguration(connectionConfiguration);
    }

    /**
     * Sets various optional options related to the http connection pool and connections.
     *
     * @param connectionConfiguration The {@link ConnectionConfiguration} object with the custom values.
     * @return This object for method chaining.
     */
    public SubclassT connectionConfiguration(ConnectionConfiguration connectionConfiguration) {
        setConnectionConfiguration(connectionConfiguration);
        return getSubclass();
    }

    /**
     * Sets a custom retry policy to use in the event of an error. See {@link RetryPolicyBuilder} for a declarative way to create
     * a retry policy.
     *
     * @param retryPolicy Custom retry policy to use for the client.
     */
    public SubclassT retryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return getSubclass();
    }

    /**
     * Sets a custom retry policy to use in the event of an error. See {@link RetryPolicyBuilder} for a declarative way to create
     * a retry policy.
     *
     * @param retryPolicy Custom retry policy to use for the client.
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    protected abstract URI defaultEndpoint();

    protected abstract String defaultRegion();

    protected Signer defaultIamSigner() {
        return new NoOpSigner();
    }

    protected IamSignerFactory signerFactory() {
        return new IamSignerFactory(region);
    }

    public final TypeToBuildT build() {
        return build(new BuilderParams());
    }

    protected abstract TypeToBuildT build(AwsSyncClientParams params);

    @SuppressWarnings("unchecked")
    private SubclassT getSubclass() {
        return (SubclassT) this;
    }

    protected SubclassT signer(RequestSigner requestSigner, Class<? extends RequestSigner> signerType) {
        signerRegistry = signerRegistry.register(requestSigner, signerType);
        return getSubclass();
    }

    /**
     * Returns the default retry policy for ApiGateway clients.
     */
    @ReviewBeforeRelease("This has removed ConnectTimeoutException due to a dependency on the Apache impl. We may want" +
                         "to define a generic connect exception in the HTTP SPI to communicate this back to the core.")
    private RetryPolicy getDefaultRetryPolicy() {
        return RetryPolicyBuilder.standard()
                                 .retryOnExceptions(ConnectException.class, BindException.class)
                                 .retryOnStatusCodes(429)
                                 .backoffStrategy(PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY_V2)
                                 .maxNumberOfRetries(PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY)
                                 .build();
    }

    private class BuilderParams extends AwsSyncClientParams {

        private AwsCredentialsProvider resolveCredentials() {
            return iamCredentials == null ? new AnonymousCredentialsProvider() : iamCredentials;
        }

        @Override
        public AwsCredentialsProvider getCredentialsProvider() {
            return resolveCredentials();
        }

        @Override
        public LegacyClientConfiguration getClientConfiguration() {
            return resolveClientConfiguration();
        }

        private LegacyClientConfiguration resolveClientConfiguration() {
            LegacyClientConfiguration config = ClientConfigurationAdapter
                    .adapt(apiGatewayClientConfiguration, new LegacyClientConfiguration());

            if (apiKey != null) {
                config.addHeader("x-api-key", apiKey);
            }
            config.setUserAgentPrefix(USER_AGENT_PREFIX + UA_NAME_VERSION_SEPERATOR + VersionInfoUtils.getVersion());
            return config;
        }

        @Override
        public RequestMetricCollector getRequestMetricCollector() {
            return null;
        }

        @Override
        public List<RequestHandler> getRequestHandlers() {
            return Collections.emptyList();
        }

        @Override
        public SignerProvider getSignerProvider() {
            if (iamCredentials != null) {
                signerRegistry = signerRegistry
                        .register(new SignerAsRequestSigner(defaultIamSigner(), iamCredentials), IamRequestSigner.class);
            }
            return new SignerProviderAdapter(signerRegistry);
        }

        @Override
        public URI getEndpoint() {
            return endpoint != null ? URI.create(endpoint) : defaultEndpoint();
        }

        @Override
        public RetryPolicy getRetryPolicy() {
            return retryPolicy == null ? getDefaultRetryPolicy() : retryPolicy;
        }

        @Override
        @ReviewBeforeRelease("Revisit when we integrate APIG back")
        public SdkHttpClient sdkHttpClient() {
            return new DefaultSdkHttpClientFactory().createHttpClientWithDefaults(AttributeMap.empty());
        }
    }

}
