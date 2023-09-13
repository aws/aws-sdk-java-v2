package software.amazon.awssdk.services.query;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.internal.QueryProtocolCustomTestInterceptor;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.protocols.query.interceptor.QueryParametersToBodyInterceptor;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Internal implementation of {@link QueryAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultQueryAsyncClientBuilder extends DefaultQueryBaseClientBuilder<QueryAsyncClientBuilder, QueryAsyncClient>
    implements QueryAsyncClientBuilder {
    @Override
    public DefaultQueryAsyncClientBuilder endpointProvider(QueryEndpointProvider endpointProvider) {
        clientConfiguration.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
        return this;
    }

    @Override
    public DefaultQueryAsyncClientBuilder tokenProvider(SdkTokenProvider tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final QueryAsyncClient buildClient() {
        SdkClientConfiguration clientConfiguration = super.asyncClientConfiguration();
        this.validateClientOptions(clientConfiguration);
        QueryServiceClientConfiguration serviceClientConfiguration = initializeServiceClientConfig(clientConfiguration);
        List<ExecutionInterceptor> interceptors = clientConfiguration.option(SdkClientOption.EXECUTION_INTERCEPTORS);
        List<ExecutionInterceptor> queryParamsToBodyInterceptor = Collections
            .singletonList(new QueryParametersToBodyInterceptor());
        List<ExecutionInterceptor> customizationInterceptors = new ArrayList<>();
        customizationInterceptors.add(new QueryProtocolCustomTestInterceptor());
        interceptors = CollectionUtils.mergeLists(queryParamsToBodyInterceptor, interceptors);
        interceptors = CollectionUtils.mergeLists(customizationInterceptors, interceptors);
        clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors)
                                                 .build();
        QueryAsyncClient client = new DefaultQueryAsyncClient(serviceClientConfiguration, clientConfiguration);
        return client;
    }

    private QueryServiceClientConfiguration initializeServiceClientConfig(SdkClientConfiguration clientConfig) {
        URI endpointOverride = null;
        EndpointProvider endpointProvider = clientConfig.option(SdkClientOption.ENDPOINT_PROVIDER);
        if (clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN) != null
            && Boolean.TRUE.equals(clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
            endpointOverride = clientConfig.option(SdkClientOption.ENDPOINT);
        }
        return QueryServiceClientConfiguration.builder().overrideConfiguration(overrideConfiguration())
                                              .region(clientConfig.option(AwsClientOption.AWS_REGION)).endpointOverride(endpointOverride)
                                              .endpointProvider(endpointProvider).build();
    }
}