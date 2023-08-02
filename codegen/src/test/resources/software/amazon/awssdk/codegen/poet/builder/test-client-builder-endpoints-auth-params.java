package software.amazon.awssdk.services.minis3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.auth.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.spi.AuthScheme;
import software.amazon.awssdk.services.minis3.auth.scheme.MiniS3AuthSchemeProvider;
import software.amazon.awssdk.services.minis3.auth.scheme.internal.MiniS3AuthSchemeInterceptor;
import software.amazon.awssdk.services.minis3.endpoints.MiniS3EndpointProvider;
import software.amazon.awssdk.services.minis3.endpoints.internal.MiniS3EndpointAuthSchemeInterceptor;
import software.amazon.awssdk.services.minis3.endpoints.internal.MiniS3RequestSetEndpointInterceptor;
import software.amazon.awssdk.services.minis3.endpoints.internal.MiniS3ResolveEndpointInterceptor;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal base class for {@link DefaultMiniS3ClientBuilder} and {@link DefaultMiniS3AsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultMiniS3BaseClientBuilder<B extends MiniS3BaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
    @Override
    protected final String serviceEndpointPrefix() {
        return "mini-s3-service-endpoint";
    }

    @Override
    protected final String serviceName() {
        return "MiniS3";
    }

    @Override
    protected final SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
        return config.merge(c -> c.option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
                .option(SdkClientOption.AUTH_SCHEME_PROVIDER, defaultAuthSchemeProvider())
                .option(SdkClientOption.AUTH_SCHEMES, defaultAuthSchemes())
                .option(SdkAdvancedClientOption.SIGNER, defaultSigner())
                .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
        endpointInterceptors.add(new MiniS3AuthSchemeInterceptor());
        endpointInterceptors.add(new MiniS3ResolveEndpointInterceptor());
        endpointInterceptors.add(new MiniS3EndpointAuthSchemeInterceptor());
        endpointInterceptors.add(new MiniS3RequestSetEndpointInterceptor());
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory
                .getInterceptors("software/amazon/awssdk/services/minis3/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        interceptors = CollectionUtils.mergeLists(endpointInterceptors, interceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        SdkClientConfiguration.Builder builder = config.toBuilder();
        builder.option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors);
        return builder.build();
    }

    private Signer defaultSigner() {
        return AwsS3V4Signer.create();
    }

    @Override
    protected final String signingName() {
        return "mini-s3-service";
    }

    private MiniS3EndpointProvider defaultEndpointProvider() {
        return MiniS3EndpointProvider.defaultProvider();
    }

    public B authSchemeProvider(MiniS3AuthSchemeProvider authSchemeProvider) {
        clientConfiguration.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
        return thisBuilder();
    }

    private MiniS3AuthSchemeProvider defaultAuthSchemeProvider() {
        return MiniS3AuthSchemeProvider.defaultProvider();
    }

    private Map<String, AuthScheme<?>> defaultAuthSchemes() {
        Map<String, AuthScheme<?>> schemes = new HashMap<>(1);
        AwsV4AuthScheme awsV4AuthScheme = AwsV4AuthScheme.create();
        schemes.put(awsV4AuthScheme.schemeId(), awsV4AuthScheme);
        return Collections.unmodifiableMap(schemes);
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
        Validate.notNull(c.option(SdkAdvancedClientOption.SIGNER),
                "The 'overrideConfiguration.advancedOption[SIGNER]' must be configured in the client builder.");
    }
}
