package software.amazon.awssdk.services.query.rules.internal;

import java.util.List;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.SignerLoader;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.rules.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.rules.AwsEndpointProviderUtils;
import software.amazon.awssdk.awscore.rules.EndpointAuthScheme;
import software.amazon.awssdk.awscore.util.SignerOverrideUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.core.signer.Signer;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class QueryEndpointAuthSchemeInterceptor implements ExecutionInterceptor {
    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        Endpoint resolvedEndpoint = executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);
        AwsRequest request = (AwsRequest) context.request();
        if (resolvedEndpoint.headers() != null) {
            request = AwsEndpointProviderUtils.addHeaders(request, resolvedEndpoint.headers());
        }
        List<EndpointAuthScheme> authSchemes = resolvedEndpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
        if (authSchemes == null) {
            return request;
        }
        EndpointAuthScheme chosenAuthScheme = AwsEndpointProviderUtils.chooseAuthScheme(authSchemes);
        Supplier<Signer> signerProvider = signerProvider(chosenAuthScheme);
        AwsEndpointProviderUtils.setSigningParams(executionAttributes, chosenAuthScheme);
        return SignerOverrideUtils.overrideSignerIfNotOverridden(request, executionAttributes, signerProvider);
    }

    private Supplier<Signer> signerProvider(EndpointAuthScheme authScheme) {
        switch (authScheme.name()) {
            case "sigv4":
                return Aws4Signer::create;
            case "sigv4a":
                return SignerLoader::getSigV4aSigner;
            default:
                break;
        }
        throw SdkClientException.create("Don't know how to create signer for auth scheme: " + authScheme.name());
    }
}
