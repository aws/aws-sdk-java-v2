package software.amazon.awssdk.services.query;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.internal.QueryProtocolCustomTestInterceptor;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.protocols.query.interceptor.QueryParametersToBodyInterceptor;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Internal implementation of {@link QueryClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultQueryClientBuilder extends DefaultQueryBaseClientBuilder<QueryClientBuilder, QueryClient> implements
                                                                                                             QueryClientBuilder {
    @Override
    public DefaultQueryClientBuilder endpointProvider(QueryEndpointProvider endpointProvider) {
        clientConfiguration.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
        return this;
    }

    @Override
    public DefaultQueryClientBuilder tokenProvider(SdkTokenProvider tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final QueryClient buildClient() {
        SdkClientConfiguration clientConfiguration = super.syncClientConfiguration();
        List<ExecutionInterceptor> interceptors = clientConfiguration.option(SdkClientOption.EXECUTION_INTERCEPTORS);
        List<ExecutionInterceptor> queryParamsToBodyInterceptor = new ArrayList<>(
            Arrays.asList(new QueryParametersToBodyInterceptor()));
        List<ExecutionInterceptor> customizationInterceptors = new ArrayList<>();
        customizationInterceptors.add(new QueryProtocolCustomTestInterceptor());
        interceptors = CollectionUtils.mergeLists(queryParamsToBodyInterceptor, interceptors);
        interceptors = CollectionUtils.mergeLists(customizationInterceptors, interceptors);
        clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors)
                                                 .build();
        this.validateClientOptions(clientConfiguration);
        URI endpointOverride = null;
        if (clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) != null
            && Boolean.TRUE.equals(clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
            endpointOverride = clientConfiguration.option(SdkClientOption.ENDPOINT);
        }
        QueryServiceClientConfiguration serviceClientConfiguration = QueryServiceClientConfiguration.builder()
                                                                                                    .overrideConfiguration(overrideConfiguration()).region(clientConfiguration.option(AwsClientOption.AWS_REGION))
                                                                                                    .endpointOverride(endpointOverride).build();
        return new DefaultQueryClient(serviceClientConfiguration, clientConfiguration);
    }
}
