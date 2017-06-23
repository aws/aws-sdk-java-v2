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

package software.amazon.awssdk.client.builder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.auth.SignerFactory;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.loader.DefaultSdkHttpClientFactory;
import software.amazon.awssdk.internal.auth.DefaultSignerProvider;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.runtime.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;

/**
 * Base class for all service specific client builders.
 *
 * @param <SubclassT>    Concrete builder type, used for better fluent methods.
 * @param <TypeToBuildT> Type that this builder builds.
 */
@NotThreadSafe
@SdkProtectedApi
@ReviewBeforeRelease("Remove when S3 is migrated to new builder pattern")
public abstract class AwsClientBuilder<SubclassT extends AwsClientBuilder, TypeToBuildT> {

    /**
     * Default Region Provider chain. Used only when the builder is not explicitly configured with a
     * region.
     */
    private static final AwsRegionProvider DEFAULT_REGION_PROVIDER = new DefaultAwsRegionProviderChain();

    /**
     * {@link AwsRegionProvider} to use when no explicit region or endpointConfiguration is configured.
     * This is currently not exposed for customization by customers.
     */
    private final AwsRegionProvider regionProvider;

    private AwsCredentialsProvider credentials;
    private LegacyClientConfiguration clientConfig;
    private RequestMetricCollector metricsCollector;
    private Region region;
    private List<RequestHandler> requestHandlers;
    private EndpointConfiguration endpointConfiguration;

    protected AwsClientBuilder() {
        this(DEFAULT_REGION_PROVIDER);
    }

    @SdkTestInternalApi
    protected AwsClientBuilder(AwsRegionProvider regionProvider) {
        this.regionProvider = regionProvider;
    }

    /**
     * Gets the AWSCredentialsProvider currently configured in the builder.
     */
    public final AwsCredentialsProvider getCredentials() {
        return this.credentials;
    }

    /**
     * Sets the AWSCredentialsProvider used by the client. If not specified the default is {@link
     * DefaultCredentialsProvider}.
     *
     * @param credentialsProvider New AWSCredentialsProvider to use.
     */
    public final void setCredentials(AwsCredentialsProvider credentialsProvider) {
        this.credentials = credentialsProvider;
    }

    /**
     * Sets the AWSCredentialsProvider used by the client. If not specified the default is {@link
     * DefaultCredentialsProvider}.
     *
     * @param credentialsProvider New AWSCredentialsProvider to use.
     * @return This object for method chaining.
     */
    public final SubclassT withCredentials(AwsCredentialsProvider credentialsProvider) {
        setCredentials(credentialsProvider);
        return getSubclass();
    }

    /**
     * If the builder isn't explicitly configured with credentials we use the {@link
     * DefaultCredentialsProvider}.
     */
    private AwsCredentialsProvider resolveCredentials() {
        return (credentials == null) ? new DefaultCredentialsProvider() : credentials;
    }

    /**
     * Gets the ClientConfiguration currently configured in the builder
     */
    public final LegacyClientConfiguration getClientConfiguration() {
        return this.clientConfig;
    }

    /**
     * Sets the ClientConfiguration to be used by the client. If not specified the default is
     * typically {@link PredefinedLegacyClientConfigurations#defaultConfig} but may differ per service.
     *
     * @param config Custom configuration to use
     */
    public final void setClientConfiguration(LegacyClientConfiguration config) {
        this.clientConfig = config;
    }

    /**
     * Sets the ClientConfiguration to be used by the client. If not specified the default is
     * typically {@link PredefinedLegacyClientConfigurations#defaultConfig} but may differ per service.
     *
     * @param config Custom configuration to use
     * @return This object for method chaining.
     */
    public final SubclassT withClientConfiguration(LegacyClientConfiguration config) {
        setClientConfiguration(config);
        return getSubclass();
    }

    /**
     * If not explicit client configuration is provided we consult the {@link
     * LegacyClientConfigurationFactory} of the service. If an explicit configuration is provided we use
     * ClientConfiguration's copy constructor to avoid mutation.
     */
    private LegacyClientConfiguration resolveClientConfiguration() {
        return (clientConfig == null) ? new LegacyClientConfiguration() :
                new LegacyClientConfiguration(clientConfig);
    }

    /**
     * Gets the {@link RequestMetricCollector} in use by the builder.
     */
    public final RequestMetricCollector getMetricsCollector() {
        return this.metricsCollector;
    }

    /**
     * Sets a custom RequestMetricCollector to use for the client.
     *
     * @param metrics Custom RequestMetricCollector to use.
     */
    public final void setMetricsCollector(RequestMetricCollector metrics) {
        this.metricsCollector = metrics;
    }

    /**
     * Sets a custom RequestMetricCollector to use for the client.
     *
     * @param metrics Custom RequestMetricCollector to use.
     * @return This object for method chaining.
     */
    public final SubclassT withMetricsCollector(RequestMetricCollector metrics) {
        setMetricsCollector(metrics);
        return getSubclass();
    }

    /**
     * Gets the region in use by the builder.
     */
    public final String getRegion() {
        return region == null ? null : region.value();
    }

    /**
     * Sets the region to be used by the client. This will be used to determine both the
     * service endpoint (eg: https://sns.us-west-1.amazonaws.com) and signing region (eg: us-west-1)
     * for requests. If neither region or endpoint configuration {@link #setEndpointConfiguration(EndpointConfiguration)}
     * are explicitly provided in the builder the {@link #DEFAULT_REGION_PROVIDER} is consulted.
     *
     * @param region Region to use
     */
    public final void setRegion(String region) {
        withRegion(Region.of(region));
    }

    /**
     * Sets the region to be used by the client. This will be used to determine both the
     * service endpoint (eg: https://sns.us-west-1.amazonaws.com) and signing region (eg: us-west-1)
     * for requests. If neither region or endpoint configuration {@link #setEndpointConfiguration(EndpointConfiguration)}
     * are explicitly provided in the builder the {@link #DEFAULT_REGION_PROVIDER} is consulted.
     *
     * @param region Region to use, this will be used to determine both service endpoint
     *               and the signing region
     * @return This object for method chaining.
     */
    public SubclassT withRegion(Region region) {
        this.region = region;
        return getSubclass();
    }

    /**
     * Gets the service endpointConfiguration in use by the builder
     */
    public final EndpointConfiguration getEndpoint() {
        return endpointConfiguration;
    }

    /**
     * Sets the endpoint configuration (service endpoint & signing region) to be used for requests. If neither region
     * {@link #setRegion(String)} or endpoint configuration are explicitly provided in the builder the
     * {@link #DEFAULT_REGION_PROVIDER} is consulted.
     * <p>
     * <p><b>Only use this if using a non-standard service endpoint - the recommended approach for configuring a client is to use
     * {@link #setRegion(String)}</b>
     *
     * @param endpointConfiguration The endpointConfiguration to use
     */
    public final void setEndpointConfiguration(EndpointConfiguration endpointConfiguration) {
        withEndpointConfiguration(endpointConfiguration);
    }

    /**
     * Sets the endpoint configuration (service endpoint & signing region) to be used for requests. If neither region
     * {@link #withRegion(Region)} or endpoint configuration are explicitly provided in the builder the
     * {@link #DEFAULT_REGION_PROVIDER} is consulted.
     * <p>
     * <p><b>Only use this if using a non-standard service endpoint - the recommended approach for configuring a client is to use
     * {@link #withRegion(Region)}</b>
     *
     * @param endpointConfiguration The endpointConfiguration to use
     * @return This object for method chaining.
     */
    public final SubclassT withEndpointConfiguration(EndpointConfiguration endpointConfiguration) {
        this.endpointConfiguration = endpointConfiguration;
        return getSubclass();
    }

    /**
     * Gets the list of request handlers in use by the builder.
     */
    public final List<RequestHandler> getRequestHandlers() {
        return this.requestHandlers == null ? null :
                Collections.unmodifiableList(this.requestHandlers);
    }

    /**
     * Sets the request handlers to use in the client.
     *
     * @param handlers Request handlers to use for client.
     */
    public final void setRequestHandlers(RequestHandler... handlers) {
        this.requestHandlers = Arrays.asList(handlers);
    }

    /**
     * Sets the request handlers to use in the client.
     *
     * @param handlers Request handlers to use for client.
     * @return This object for method chaining.
     */
    public final SubclassT withRequestHandlers(RequestHandler... handlers) {
        setRequestHandlers(handlers);
        return getSubclass();
    }

    /**
     * Request handlers are copied to a new list to avoid mutation, if no request handlers are
     * provided to the builder we supply an empty list.
     */
    private List<RequestHandler> resolveRequestHandlers() {
        return (requestHandlers == null) ? new ArrayList<RequestHandler>() :
                new ArrayList<RequestHandler>(requestHandlers);
    }

    /**
     * Resolve which signing region should be used with the client.
     */
    private Region resolveSigningRegion() {
        if (endpointConfiguration != null) {
            return Region.of(endpointConfiguration.getSigningRegion());
        }

        return region != null ? region : determineRegionFromRegionProvider();
    }

    /**
     * Builds a client with the configure properties.
     *
     * @return Client instance to make API calls with.
     */
    public abstract TypeToBuildT build();

    public abstract String getServiceName();

    public abstract String getEndpointPrefix();

    /**
     * @return An instance of AwsSyncClientParams that has all params to be used in the sync client constructor.
     */
    protected final AwsSyncClientParams getSyncClientParams() {
        return new SyncBuilderParams();
    }

    /**
     * Attempt to determine the region from the configured region provider. This will return null in the event that the
     * region provider could not determine the region automatically.
     */
    private Region determineRegionFromRegionProvider() {
        try {
            return regionProvider.getRegion();
        } catch (SdkClientException e) {
            // The AwsRegionProviderChain that is used by default throws an exception instead of returning null when
            // the region is not defined. For that reason, we have to support both throwing an exception and returning
            // null as the region not being defined.
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected final SubclassT getSubclass() {
        return (SubclassT) this;
    }

    /**
     * A container for configuration required to submit requests to a service (service endpoint and signing region)
     */
    public static final class EndpointConfiguration {
        private final String serviceEndpoint;
        private final String signingRegion;

        /**
         * @param serviceEndpoint the service endpoint either with or without the protocol
         *                        (e.g. https://sns.us-west-1.amazonaws.com or sns.us-west-1.amazonaws.com)
         * @param signingRegion   the region to use for SigV4 signing of requests (e.g. us-west-1)
         */
        public EndpointConfiguration(String serviceEndpoint, String signingRegion) {
            this.serviceEndpoint = serviceEndpoint;
            this.signingRegion = signingRegion;
        }

        public String getServiceEndpoint() {
            return serviceEndpoint;
        }

        public String getSigningRegion() {
            return signingRegion;
        }
    }

    /**
     * Presents a view of the builder to be used in a client constructor.
     */
    protected class SyncBuilderParams extends AwsAsyncClientParams {
        private final LegacyClientConfiguration clientConfig;
        private final AwsCredentialsProvider credentials;
        private final RequestMetricCollector metricsCollector;
        private final List<RequestHandler> requestHandlers;
        private final Region signingRegion;

        protected SyncBuilderParams() {
            this.clientConfig = resolveClientConfiguration();
            this.credentials = resolveCredentials();
            this.metricsCollector = AwsClientBuilder.this.metricsCollector;
            this.requestHandlers = resolveRequestHandlers();
            this.signingRegion = resolveSigningRegion();
            validateParams();
        }

        private void validateParams() {
            Validate.validState(region == null || endpointConfiguration == null,
                                "Only one of Region or EndpointConfiguration may be set.");
            Validate.validState(signingRegion != null,
                                "Signing region could not be determined. Please specify the region or endpoint.");
            Validate.validState(credentials != null, "Credentials could not be determined.");
        }

        @Override
        public AwsCredentialsProvider getCredentialsProvider() {
            return this.credentials;
        }

        @Override
        public LegacyClientConfiguration getClientConfiguration() {
            return this.clientConfig;
        }

        @Override
        public RequestMetricCollector getRequestMetricCollector() {
            return this.metricsCollector;
        }

        @Override
        public List<RequestHandler> getRequestHandlers() {
            return this.requestHandlers;
        }

        @Override
        public SignerProvider getSignerProvider() {
            Signer signer = SignerFactory.getSigner(getServiceName(), signingRegion.value());

            return new DefaultSignerProvider(signer);
        }

        @Override
        public URI getEndpoint() {
            if (endpointConfiguration != null) {
                return URI.create(endpointConfiguration.getServiceEndpoint());
            }

            return new DefaultServiceEndpointBuilder(getEndpointPrefix(), Protocol.HTTPS.toString())
                    .withRegion(signingRegion)
                    .getServiceEndpoint();
        }

        @Override
        public ScheduledExecutorService getExecutor() {
            throw new UnsupportedOperationException("ExecutorService is not used for sync client.");
        }

        @Override
        public SdkHttpClient sdkHttpClient() {
            return new DefaultSdkHttpClientFactory().createHttpClientWithDefaults(AttributeMap.empty());
        }
    }

}
