package software.amazon.awssdk.services.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.services.xml.endpoints.XmlClientContextParams;
import software.amazon.awssdk.services.xml.endpoints.XmlEndpointProvider;
import software.amazon.awssdk.services.xml.endpoints.internal.XmlEndpointAuthSchemeInterceptor;
import software.amazon.awssdk.services.xml.endpoints.internal.XmlRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.xml.endpoints.internal.XmlResolveEndpointInterceptor;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal base class for {@link DefaultXmlClientBuilder} and {@link DefaultXmlAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultXmlBaseClientBuilder<B extends XmlBaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
    @Override
    protected final String serviceEndpointPrefix() {
        return "xml-service";
    }

    @Override
    protected final String serviceName() {
        return "Xml";
    }

    @Override
    protected final SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
        return config.merge(c -> c.option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
                                  .option(SdkAdvancedClientOption.SIGNER, defaultSigner())
                                  .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                  .option(AwsClientOption.TOKEN_PROVIDER, defaultTokenProvider())
                                  .option(SdkAdvancedClientOption.TOKEN_SIGNER, defaultTokenSigner()));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(
        SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
        endpointInterceptors.add(new XmlResolveEndpointInterceptor());
        endpointInterceptors.add(new XmlEndpointAuthSchemeInterceptor());
        endpointInterceptors.add(new XmlRequestSetEndpointInterceptor());
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory.getInterceptors("software/amazon/awssdk/services/xml/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        interceptors = CollectionUtils.mergeLists(endpointInterceptors, interceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        clientContextParams.put(XmlClientContextParams.S3_EXPRESS_IDENTITY_PROVIDER, new AtomicReference<>(null));
        return config.toBuilder()
                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors)
                         .option(SdkClientOption.CLIENT_CONTEXT_PARAMS, clientContextParams.build()).build();
    }

    private Signer defaultSigner() {
        return Aws4Signer.create();
    }

    @Override
    protected final String signingName() {
        return "xml-service";
    }

    private XmlEndpointProvider defaultEndpointProvider() {
        return XmlEndpointProvider.defaultProvider();
    }

    private SdkTokenProvider defaultTokenProvider() {
        return DefaultAwsTokenProvider.create();
    }

    private Signer defaultTokenSigner() {
        return BearerTokenSigner.create();
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
        Validate.notNull(c.option(SdkAdvancedClientOption.SIGNER),
                         "The 'overrideConfiguration.advancedOption[SIGNER]' must be configured in the client builder.");
        Validate.notNull(c.option(SdkAdvancedClientOption.TOKEN_SIGNER),
                         "The 'overrideConfiguration.advancedOption[TOKEN_SIGNER]' must be configured in the client builder.");
        Validate.notNull(c.option(AwsClientOption.TOKEN_PROVIDER),
                         "The 'overrideConfiguration.advancedOption[TOKEN_PROVIDER]' must be configured in the client builder.");
    }
}
