package software.amazon.awssdk.services.query.endpoints.internal;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class QueryRequestSetEndpointInterceptor implements ExecutionInterceptor {
    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        if (AwsEndpointProviderUtils.endpointIsDiscovered(executionAttributes)) {
            return context.httpRequest();
        }
        Endpoint endpoint = (Endpoint) executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);
        return AwsEndpointProviderUtils.setUri(context.httpRequest(),
                                               executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT), endpoint.url());
    }
}
