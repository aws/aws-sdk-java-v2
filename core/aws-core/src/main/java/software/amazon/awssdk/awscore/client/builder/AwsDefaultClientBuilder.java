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

package software.amazon.awssdk.awscore.client.builder;

import static software.amazon.awssdk.core.client.config.SdkClientOption.CONFIGURED_RETRY_CONFIGURATOR;
import static software.amazon.awssdk.core.client.config.SdkClientOption.CONFIGURED_RETRY_MODE;
import static software.amazon.awssdk.core.client.config.SdkClientOption.CONFIGURED_RETRY_STRATEGY;
import static software.amazon.awssdk.core.client.config.SdkClientOption.RETRY_STRATEGY;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.awscore.endpoint.AwsClientEndpointProvider;
import software.amazon.awssdk.awscore.endpoint.DualstackEnabledProvider;
import software.amazon.awssdk.awscore.endpoint.FipsEnabledProvider;
import software.amazon.awssdk.awscore.eventstream.EventStreamInitialRequestInterceptor;
import software.amazon.awssdk.awscore.interceptor.HelpfulUnknownHostExceptionInterceptor;
import software.amazon.awssdk.awscore.interceptor.TraceIdExecutionInterceptor;
import software.amazon.awssdk.awscore.internal.defaultsmode.AutoDefaultsModeDiscovery;
import software.amazon.awssdk.awscore.internal.defaultsmode.DefaultsModeConfiguration;
import software.amazon.awssdk.awscore.internal.defaultsmode.DefaultsModeResolver;
import software.amazon.awssdk.awscore.retry.AwsRetryPolicy;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.SdkInternalTestAdvancedClientOption;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.internal.BaseRetryStrategy;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.AttributeMap.LazyValueSource;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

/**
 * An SDK-internal implementation of the methods in {@link AwsClientBuilder}, {@link AwsAsyncClientBuilder} and
 * {@link AwsSyncClientBuilder}. This implements all methods required by those interfaces, allowing service-specific builders to
 * just
 * implement the configuration they wish to add.
 *
 * <p>By implementing both the sync and async interface's methods, service-specific builders can share code between their sync
 * and
 * async variants without needing one to extend the other. Note: This only defines the methods in the sync and async builder
 * interfaces. It does not implement the interfaces themselves. This is because the sync and async client builder interfaces both
 * require a type-constrained parameter for use in fluent chaining, and a generic type parameter conflict is introduced into the
 * class hierarchy by this interface extending the builder interfaces themselves.</p>
 *
 * <p>Like all {@link AwsClientBuilder}s, this class is not thread safe.</p>
 *
 * @param <BuilderT> The type of builder, for chaining.
 * @param <ClientT> The type of client generated by this builder.
 */
@SdkProtectedApi
public abstract class AwsDefaultClientBuilder<BuilderT extends AwsClientBuilder<BuilderT, ClientT>, ClientT>
    extends SdkDefaultClientBuilder<BuilderT, ClientT>
    implements AwsClientBuilder<BuilderT, ClientT> {
    private static final Logger log = Logger.loggerFor(AwsClientBuilder.class);
    private static final String DEFAULT_ENDPOINT_PROTOCOL = "https";
    private static final String[] FIPS_SEARCH = {"fips-", "-fips"};
    private static final String[] FIPS_REPLACE = {"", ""};

    private final AutoDefaultsModeDiscovery autoDefaultsModeDiscovery;

    protected AwsDefaultClientBuilder() {
        super();
        autoDefaultsModeDiscovery = new AutoDefaultsModeDiscovery();
    }

    @SdkTestInternalApi
    AwsDefaultClientBuilder(SdkHttpClient.Builder defaultHttpClientBuilder,
                            SdkAsyncHttpClient.Builder defaultAsyncHttpClientFactory,
                            AutoDefaultsModeDiscovery autoDefaultsModeDiscovery) {
        super(defaultHttpClientBuilder, defaultAsyncHttpClientFactory);
        this.autoDefaultsModeDiscovery = autoDefaultsModeDiscovery;
    }

    /**
     * Implemented by child classes to define the endpoint prefix used when communicating with AWS. This constitutes the first
     * part of the URL in the DNS name for the service. Eg. in the endpoint "dynamodb.amazonaws.com", this is the "dynamodb".
     *
     * <p>For standard services, this should match the "endpointPrefix" field in the AWS model.</p>
     */
    protected abstract String serviceEndpointPrefix();

    /**
     * Implemented by child classes to define the signing-name that should be used when signing requests when communicating with
     * AWS.
     */
    protected abstract String signingName();

    /**
     * Implemented by child classes to define the service name used to identify the request in things like metrics.
     */
    protected abstract String serviceName();

    @Override
    protected final SdkClientConfiguration mergeChildDefaults(SdkClientConfiguration configuration) {
        SdkClientConfiguration config = mergeServiceDefaults(configuration);
        config = config.merge(c -> c.option(AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION, true)
                                    .option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, false)
                                    .option(AwsClientOption.SERVICE_SIGNING_NAME, signingName())
                                    .option(SdkClientOption.SERVICE_NAME, serviceName())
                                    .option(AwsClientOption.ENDPOINT_PREFIX, serviceEndpointPrefix()));
        return mergeInternalDefaults(config);
    }

    /**
     * Optionally overridden by child classes to define service-specific default configuration.
     */
    protected SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration configuration) {
        return configuration;
    }

    /**
     * Optionally overridden by child classes to define internal default configuration.
     */
    protected SdkClientConfiguration mergeInternalDefaults(SdkClientConfiguration configuration) {
        return configuration;
    }

    /**
     * Return a client configuration object, populated with the following chain of priorities.
     * <ol>
     *     <li>Defaults vended from {@link DefaultsMode} </li>
     *     <li>AWS Global Defaults</li>
     * </ol>
     */
    @Override
    protected final SdkClientConfiguration finalizeChildConfiguration(SdkClientConfiguration configuration) {
        configuration = finalizeServiceConfiguration(configuration);
        configuration = finalizeAwsConfiguration(configuration);
        return configuration;
    }

    private SdkClientConfiguration finalizeAwsConfiguration(SdkClientConfiguration configuration) {
        return configuration.toBuilder()
                            .option(SdkClientOption.EXECUTION_INTERCEPTORS, addAwsInterceptors(configuration))
                            .lazyOptionIfAbsent(AwsClientOption.AWS_REGION, this::resolveRegion)
                            .lazyOptionIfAbsent(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED, this::resolveDualstackEndpointEnabled)
                            .lazyOptionIfAbsent(AwsClientOption.FIPS_ENDPOINT_ENABLED, this::resolveFipsEndpointEnabled)
                            .lazyOption(AwsClientOption.DEFAULTS_MODE, this::resolveDefaultsMode)
                            .lazyOptionIfAbsent(SdkClientOption.DEFAULT_RETRY_MODE, this::resolveDefaultRetryMode)
                            .lazyOption(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT,
                                        this::resolveDefaultS3UsEast1RegionalEndpoint)
                            .lazyOptionIfAbsent(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER,
                                                this::resolveCredentialsIdentityProvider)
                            // Set CREDENTIALS_PROVIDER, because older clients may be relying on it
                            .lazyOptionIfAbsent(AwsClientOption.CREDENTIALS_PROVIDER, this::resolveCredentialsProvider)
                            .lazyOptionIfAbsent(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, this::resolveClientEndpointProvider)
                            // Set ENDPOINT and ENDPOINT_OVERRIDDEN, because older clients may be relying on it
                            .lazyOptionIfAbsent(SdkClientOption.ENDPOINT, this::resolveEndpoint)
                            .lazyOptionIfAbsent(SdkClientOption.ENDPOINT_OVERRIDDEN, this::resolveEndpointOverridden)
                            .lazyOption(AwsClientOption.SIGNING_REGION, this::resolveSigningRegion)
                            .lazyOption(SdkClientOption.HTTP_CLIENT_CONFIG, this::resolveHttpClientConfig)
                            .applyMutation(this::configureRetryPolicy)
                            .applyMutation(this::configureRetryStrategy)
                            .lazyOptionIfAbsent(SdkClientOption.IDENTITY_PROVIDERS, this::resolveIdentityProviders)
                            .build();
    }

    /**
     * Apply the client override configuration to the provided configuration.
     */
    @Override
    protected final SdkClientConfiguration setOverrides(SdkClientConfiguration configuration) {
        if (overrideConfig == null) {
            return configuration;
        }
        SdkClientConfiguration.Builder builder = configuration.toBuilder();
        overrideConfig.retryStrategy().ifPresent(retryStrategy -> builder.option(RETRY_STRATEGY, retryStrategy));
        overrideConfig.retryMode().ifPresent(retryMode -> builder.option(RETRY_STRATEGY,
                                                                         AwsRetryStrategy.forRetryMode(retryMode)));
        overrideConfig.retryStrategyConfigurator().ifPresent(configurator -> {
            RetryStrategy.Builder<?, ?> defaultBuilder = AwsRetryStrategy.defaultRetryStrategy().toBuilder();
            configurator.accept(defaultBuilder);
            builder.option(RETRY_STRATEGY, defaultBuilder.build());
        });
        builder.putAll(overrideConfig);

        checkEndpointOverriddenOverride(configuration, builder);

        // Forget anything we configured in the override configuration else it might be re-applied.
        builder.option(CONFIGURED_RETRY_MODE, null);
        builder.option(CONFIGURED_RETRY_STRATEGY, null);
        builder.option(CONFIGURED_RETRY_CONFIGURATOR, null);
        return builder.build();
    }

    /**
     * Check {@link SdkInternalTestAdvancedClientOption#ENDPOINT_OVERRIDDEN_OVERRIDE} to see if we should override the
     * value returned by {@link SdkClientOption#CLIENT_ENDPOINT_PROVIDER}'s isEndpointOverridden.
     */
    private void checkEndpointOverriddenOverride(SdkClientConfiguration configuration, SdkClientConfiguration.Builder builder) {
        Optional<Boolean> endpointOverriddenOverride =
            overrideConfig.advancedOption(SdkInternalTestAdvancedClientOption.ENDPOINT_OVERRIDDEN_OVERRIDE);
        endpointOverriddenOverride.ifPresent(override -> {
            ClientEndpointProvider clientEndpoint = configuration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER);
            builder.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                           ClientEndpointProvider.create(clientEndpoint.clientEndpoint(), override));
        });
    }

    /**
     * Return HTTP related defaults with the following chain of priorities.
     * <ol>
     * <li>Service-Specific Defaults</li>
     * <li>Defaults vended by {@link DefaultsMode}</li>
     * </ol>
     */
    private AttributeMap resolveHttpClientConfig(LazyValueSource config) {
        AttributeMap attributeMap = serviceHttpConfig();
        return mergeSmartHttpDefaults(config, attributeMap);
    }

    /**
     * Optionally overridden by child classes to define service-specific HTTP configuration defaults.
     */
    protected AttributeMap serviceHttpConfig() {
        return AttributeMap.empty();
    }

    private IdentityProviders resolveIdentityProviders(LazyValueSource config) {
        // By default, all AWS clients get an identity provider for AWS credentials. Child classes may override this to specify
        // AWS credentials and another identity type like Bearer credentials.
        return IdentityProviders.builder()
                                .putIdentityProvider(config.get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER))
                                .build();
    }

    private DefaultsMode resolveDefaultsMode(LazyValueSource config) {
        DefaultsMode configuredMode = config.get(AwsClientOption.CONFIGURED_DEFAULTS_MODE);
        if (configuredMode == null) {
            configuredMode = DefaultsModeResolver.create()
                                                 .profileFile(config.get(SdkClientOption.PROFILE_FILE_SUPPLIER))
                                                 .profileName(config.get(SdkClientOption.PROFILE_NAME))
                                                 .resolve();
        }

        if (configuredMode != DefaultsMode.AUTO) {
            return configuredMode;
        }

        return autoDefaultsModeDiscovery.discover(config.get(AwsClientOption.AWS_REGION));
    }

    private RetryMode resolveDefaultRetryMode(LazyValueSource config) {
        return DefaultsModeConfiguration.defaultConfig(config.get(AwsClientOption.DEFAULTS_MODE))
                                        .get(SdkClientOption.DEFAULT_RETRY_MODE);
    }

    private String resolveDefaultS3UsEast1RegionalEndpoint(LazyValueSource config) {
        return DefaultsModeConfiguration.defaultConfig(config.get(AwsClientOption.DEFAULTS_MODE))
                                        .get(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT);
    }

    /**
     * Optionally overridden by child classes to derive service-specific configuration from the default-applied configuration.
     */
    protected SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration configuration) {
        return configuration;
    }

    /**
     * Merged the HTTP defaults specified for each {@link DefaultsMode}
     */
    private AttributeMap mergeSmartHttpDefaults(LazyValueSource config, AttributeMap attributeMap) {
        DefaultsMode defaultsMode = config.get(AwsClientOption.DEFAULTS_MODE);
        return attributeMap.merge(DefaultsModeConfiguration.defaultHttpConfig(defaultsMode));
    }

    /**
     * Resolve the signing region from the default-applied configuration.
     */
    private Region resolveSigningRegion(LazyValueSource config) {
        return ServiceMetadata.of(serviceEndpointPrefix())
                              .signingRegion(config.get(AwsClientOption.AWS_REGION));
    }

    /**
     * Specify the client endpoint provider to use for the client, if the client didn't specify one itself.
     * <p>
     * This is only used for older client versions. Newer clients specify this value themselves.
     */
    private ClientEndpointProvider resolveClientEndpointProvider(LazyValueSource config) {
        ServiceMetadataAdvancedOption<String> useGlobalS3EndpointProperty =
            ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT;
        return AwsClientEndpointProvider.builder()
                                        .serviceEndpointPrefix(serviceEndpointPrefix())
                                        .defaultProtocol(DEFAULT_ENDPOINT_PROTOCOL)
                                        .region(config.get(AwsClientOption.AWS_REGION))
                                        .profileFile(config.get(SdkClientOption.PROFILE_FILE_SUPPLIER))
                                        .profileName(config.get(SdkClientOption.PROFILE_NAME))
                                        .putAdvancedOption(useGlobalS3EndpointProperty,
                                                           config.get(useGlobalS3EndpointProperty))
                                        .dualstackEnabled(config.get(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED))
                                        .fipsEnabled(config.get(AwsClientOption.FIPS_ENDPOINT_ENABLED))
                                        .build();
    }

    /**
     * Resolve the client endpoint. This code is only needed by old SDK client versions. Newer SDK client versions
     * resolve this information from the client endpoint provider.
     */
    private URI resolveEndpoint(LazyValueSource config) {
        return config.get(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint();
    }

    /**
     * Resolve whether the endpoint was overridden by the customer. This code is only needed by old SDK client
     * versions. Newer SDK client versions resolve this information from the client endpoint provider.
     */
    private boolean resolveEndpointOverridden(LazyValueSource config) {
        return config.get(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).isEndpointOverridden();
    }

    /**
     * Resolve the region that should be used based on the customer's configuration.
     */
    private Region resolveRegion(LazyValueSource config) {
        Boolean defaultRegionDetectionEnabled = config.get(AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION);
        if (defaultRegionDetectionEnabled != null && !defaultRegionDetectionEnabled) {
            throw new IllegalStateException("No region was configured, and use-region-provider-chain was disabled.");
        }

        Supplier<ProfileFile> profileFile = config.get(SdkClientOption.PROFILE_FILE_SUPPLIER);
        String profileName = config.get(SdkClientOption.PROFILE_NAME);
        return DefaultAwsRegionProviderChain.builder()
                                            .profileFile(profileFile)
                                            .profileName(profileName)
                                            .build()
                                            .getRegion();
    }

    /**
     * Resolve whether a dualstack endpoint should be used for this client.
     */
    private Boolean resolveDualstackEndpointEnabled(LazyValueSource config) {
        Supplier<ProfileFile> profileFile = config.get(SdkClientOption.PROFILE_FILE_SUPPLIER);
        String profileName = config.get(SdkClientOption.PROFILE_NAME);
        return DualstackEnabledProvider.builder()
                                       .profileFile(profileFile)
                                       .profileName(profileName)
                                       .build()
                                       .isDualstackEnabled()
                                       .orElse(null);
    }

    /**
     * Resolve whether a fips endpoint should be used for this client.
     */
    private Boolean resolveFipsEndpointEnabled(LazyValueSource config) {
        Supplier<ProfileFile> profileFile = config.get(SdkClientOption.PROFILE_FILE_SUPPLIER);
        String profileName = config.get(SdkClientOption.PROFILE_NAME);
        return FipsEnabledProvider.builder()
                                  .profileFile(profileFile)
                                  .profileName(profileName)
                                  .build()
                                  .isFipsEnabled()
                                  .orElse(null);
    }

    /**
     * Resolve the credentials that should be used based on the customer's configuration.
     */
    private IdentityProvider<? extends AwsCredentialsIdentity> resolveCredentialsIdentityProvider(LazyValueSource config) {
        return DefaultCredentialsProvider.builder()
                                         .profileFile(config.get(SdkClientOption.PROFILE_FILE_SUPPLIER))
                                         .profileName(config.get(SdkClientOption.PROFILE_NAME))
                                         .build();
    }

    private AwsCredentialsProvider resolveCredentialsProvider(LazyValueSource config) {
        return CredentialUtils.toCredentialsProvider(config.get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER));
    }

    private void configureRetryPolicy(SdkClientConfiguration.Builder config) {
        RetryPolicy policy = config.option(SdkClientOption.RETRY_POLICY);
        if (policy != null && policy.additionalRetryConditionsAllowed()) {
            config.option(SdkClientOption.RETRY_POLICY, AwsRetryPolicy.addRetryConditions(policy));
        }
    }

    private void configureRetryStrategy(SdkClientConfiguration.Builder config) {
        RetryStrategy strategy = config.option(SdkClientOption.RETRY_STRATEGY);
        if (strategy != null) {
            // TODO(10/09/24) This is a temporal workaround and not a long term solution. It will fail to add the SDK and AWS
            //  defaults if the users add one or more if their own retry predicates. A long term fix is needed that can
            //  "remember" which defaults have been already applied, e.g.,
            //    if (strategy.shouldAddDefaults("aws")) {
            //       strategy = strategy.toBuilder()
            //                          .applyMutation(AwsRetryStrategy::applyDefaults)
            //                          .markDefaultsAdded("aws")
            //                          .build();
            //    }
            if (strategy.maxAttempts() > 1
                && (strategy instanceof BaseRetryStrategy)
                && !((BaseRetryStrategy) strategy).hasRetryPredicates()
            ) {
                RetryStrategy.Builder<?, ?> builder = strategy.toBuilder();
                SdkDefaultRetryStrategy.configureStrategy(builder);
                AwsRetryStrategy.configureStrategy(builder);
                config.option(SdkClientOption.RETRY_STRATEGY, builder.build());
            }
            return;
        }
        config.lazyOption(SdkClientOption.RETRY_STRATEGY, this::resolveAwsRetryStrategy);
    }

    private RetryStrategy resolveAwsRetryStrategy(LazyValueSource config) {
        RetryMode retryMode = RetryMode.resolver()
                                       .profileFile(config.get(SdkClientOption.PROFILE_FILE_SUPPLIER))
                                       .profileName(config.get(SdkClientOption.PROFILE_NAME))
                                       .defaultRetryMode(config.get(SdkClientOption.DEFAULT_RETRY_MODE))
                                       .resolve();
        return AwsRetryStrategy.forRetryMode(retryMode);
    }

    @Override
    public final BuilderT region(Region region) {
        Region regionToSet = region;
        Boolean fipsEnabled = null;

        if (region != null) {
            Pair<Region, Optional<Boolean>> transformedRegion = transformFipsPseudoRegionIfNecessary(region);
            regionToSet = transformedRegion.left();
            fipsEnabled = transformedRegion.right().orElse(null);
        }

        clientConfiguration.option(AwsClientOption.AWS_REGION, regionToSet);
        if (fipsEnabled != null) {
            clientConfiguration.option(AwsClientOption.FIPS_ENDPOINT_ENABLED, fipsEnabled);
        }
        return thisBuilder();
    }

    public final void setRegion(Region region) {
        region(region);
    }

    @Override
    public BuilderT dualstackEnabled(Boolean dualstackEndpointEnabled) {
        clientConfiguration.option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED, dualstackEndpointEnabled);
        return thisBuilder();
    }

    public final void setDualstackEnabled(Boolean dualstackEndpointEnabled) {
        dualstackEnabled(dualstackEndpointEnabled);
    }

    @Override
    public BuilderT fipsEnabled(Boolean dualstackEndpointEnabled) {
        clientConfiguration.option(AwsClientOption.FIPS_ENDPOINT_ENABLED, dualstackEndpointEnabled);
        return thisBuilder();
    }

    public final void setFipsEnabled(Boolean fipsEndpointEnabled) {
        fipsEnabled(fipsEndpointEnabled);
    }

    public final void setCredentialsProvider(AwsCredentialsProvider credentialsProvider) {
        credentialsProvider(credentialsProvider);
    }

    @Override
    public final BuilderT credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> identityProvider) {
        clientConfiguration.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, identityProvider);
        return thisBuilder();
    }

    public final void setCredentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> identityProvider) {
        credentialsProvider(identityProvider);
    }

    private List<ExecutionInterceptor> addAwsInterceptors(SdkClientConfiguration config) {
        List<ExecutionInterceptor> interceptors = awsInterceptors();
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        return interceptors;
    }

    private List<ExecutionInterceptor> awsInterceptors() {
        return Arrays.asList(new HelpfulUnknownHostExceptionInterceptor(),
                             new EventStreamInitialRequestInterceptor(),
                             new TraceIdExecutionInterceptor());
    }

    @Override
    public final BuilderT defaultsMode(DefaultsMode defaultsMode) {
        clientConfiguration.option(AwsClientOption.CONFIGURED_DEFAULTS_MODE, defaultsMode);
        return thisBuilder();
    }

    public final void setDefaultsMode(DefaultsMode defaultsMode) {
        defaultsMode(defaultsMode);
    }

    /**
     * If the region is a FIPS pseudo region (contains "fips"), this method returns a pair of values, the left side being the
     * region with the "fips" string removed, and the right being {@code true}. Otherwise, the region is returned
     * unchanged, and the right will be empty.
     */
    private static Pair<Region, Optional<Boolean>> transformFipsPseudoRegionIfNecessary(Region region) {
        String id = region.id();
        String newId = StringUtils.replaceEach(id, FIPS_SEARCH, FIPS_REPLACE);
        if (!newId.equals(id)) {
            log.info(() -> String.format("Replacing input region %s with %s and setting fipsEnabled to true", id, newId));
            return Pair.of(Region.of(newId), Optional.of(true));
        }

        return Pair.of(region, Optional.empty());
    }

}
