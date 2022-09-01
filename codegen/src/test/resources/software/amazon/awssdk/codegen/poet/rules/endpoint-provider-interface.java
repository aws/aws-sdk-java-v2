package software.amazon.awssdk.services.query.rules;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.services.query.rules.internal.DefaultQueryEndpointProvider;

@SdkPublicApi
public interface QueryEndpointProvider {
    Endpoint resolveEndpoint(QueryEndpointParameters endpointParams);

    static QueryEndpointProvider defaultProvider() {
        return new DefaultQueryEndpointProvider();
    }
}
