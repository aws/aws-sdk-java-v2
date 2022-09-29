package software.amazon.awssdk.services.query;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.protocols.query.interceptor.QueryParametersToBodyInterceptor;
import software.amazon.awssdk.services.query.rules.QueryClientContextParams;
import software.amazon.awssdk.services.query.rules.QueryEndpointProvider;
import software.amazon.awssdk.services.query.rules.internal.QueryRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.query.rules.internal.QueryResolveEndpointInterceptor;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Internal base class for {@link DefaultQueryClientBuilder} and {@link DefaultQueryAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultQueryBaseClientBuilder<B extends QueryBaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
    @Override
    protected final String serviceEndpointPrefix() {
        return "query-service";
    }

    @Override
    protected final String serviceName() {
        return "Query";
    }

    @Override
    protected final SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
        return config.merge(c -> c.option(SdkAdvancedClientOption.SIGNER, defaultSigner())
                                  .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                  .option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider()));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory
            .getInterceptors("software/amazon/awssdk/services/query/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        additionalInterceptors.add(new QueryResolveEndpointInterceptor());
        additionalInterceptors.add(new QueryRequestSetEndpointInterceptor());
        additionalInterceptors.add(new QueryParametersToBodyInterceptor());
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        return config.toBuilder().option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors)
                     .option(SdkClientOption.CLIENT_CONTEXT_PARAMS, clientContextParams.build()).build();
    }

    private Signer defaultSigner() {
        return Aws4Signer.create();
    }

    @Override
    protected final String signingName() {
        return "query-service";
    }

    private QueryEndpointProvider defaultEndpointProvider() {
        return QueryEndpointProvider.defaultProvider();
    }

    public B booleanContextParam(Boolean booleanContextParam) {
        clientContextParams.put(QueryClientContextParams.BOOLEAN_CONTEXT_PARAM, booleanContextParam);
        return thisBuilder();
    }

    public B stringContextParam(String stringContextParam) {
        clientContextParams.put(QueryClientContextParams.STRING_CONTEXT_PARAM, stringContextParam);
        return thisBuilder();
    }
}
