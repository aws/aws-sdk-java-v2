package software.amazon.awssdk.services.xml;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.services.xml.endpoints.XmlClientContextParams;
import software.amazon.awssdk.services.xml.endpoints.XmlEndpointProvider;
import software.amazon.awssdk.services.xml.internal.s3express.DefaultS3ExpressIdentityProvider;

/**
 * Internal implementation of {@link XmlClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultXmlClientBuilder extends DefaultXmlBaseClientBuilder<XmlClientBuilder, XmlClient> implements XmlClientBuilder {
    @Override
    public DefaultXmlClientBuilder endpointProvider(XmlEndpointProvider endpointProvider) {
        clientConfiguration.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
        return this;
    }

    @Override
    public DefaultXmlClientBuilder tokenProvider(SdkTokenProvider tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final XmlClient buildClient() {
        SdkClientConfiguration clientConfiguration = super.syncClientConfiguration();
        this.validateClientOptions(clientConfiguration);
        XmlServiceClientConfiguration serviceClientConfiguration = initializeServiceClientConfig(clientConfiguration);
        XmlClient client = new DefaultXmlClient(serviceClientConfiguration, clientConfiguration);
        DefaultS3ExpressIdentityProvider identityProvider = new DefaultS3ExpressIdentityProvider(client);
        identityProvider.addSelfToRef(clientContextParams.get(XmlClientContextParams.S3_EXPRESS_IDENTITY_PROVIDER));
        return client;
    }

    private XmlServiceClientConfiguration initializeServiceClientConfig(SdkClientConfiguration clientConfig) {
        URI endpointOverride = null;
        EndpointProvider endpointProvider = clientConfig.option(SdkClientOption.ENDPOINT_PROVIDER);
        if (clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN) != null
            && Boolean.TRUE.equals(clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
            endpointOverride = clientConfig.option(SdkClientOption.ENDPOINT);
        }
        return XmlServiceClientConfiguration.builder().overrideConfiguration(overrideConfiguration())
                                            .region(clientConfig.option(AwsClientOption.AWS_REGION)).endpointOverride(endpointOverride)
                                            .endpointProvider(endpointProvider).build();
    }
}