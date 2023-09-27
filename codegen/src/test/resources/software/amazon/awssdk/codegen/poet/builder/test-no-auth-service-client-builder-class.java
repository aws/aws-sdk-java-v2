package software.amazon.awssdk.services.database;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.database.endpoints.DatabaseEndpointProvider;
import software.amazon.awssdk.services.database.endpoints.internal.DatabaseRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.database.endpoints.internal.DatabaseResolveEndpointInterceptor;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Internal base class for {@link DefaultDatabaseClientBuilder} and {@link DefaultDatabaseAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultDatabaseBaseClientBuilder<B extends DatabaseBaseClientBuilder<B, C>, C> extends
                                                                                              AwsDefaultClientBuilder<B, C> {
    @Override
    protected final String serviceEndpointPrefix() {
        return "database-service-endpoint";
    }

    @Override
    protected final String serviceName() {
        return "Database";
    }

    @Override
    protected final SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
        return config.merge(c -> c.option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
                                  .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
        endpointInterceptors.add(new DatabaseResolveEndpointInterceptor());
        endpointInterceptors.add(new DatabaseRequestSetEndpointInterceptor());
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory
            .getInterceptors("software/amazon/awssdk/services/database/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        interceptors = CollectionUtils.mergeLists(endpointInterceptors, interceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        SdkClientConfiguration.Builder builder = config.toBuilder();
        builder.option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors);
        return builder.build();
    }

    @Override
    protected final String signingName() {
        return "database-service";
    }

    private DatabaseEndpointProvider defaultEndpointProvider() {
        return DatabaseEndpointProvider.defaultProvider();
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
    }
}
