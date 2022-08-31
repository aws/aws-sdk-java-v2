package software.amazon.awssdk.services.query.rules;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.rules.model.Endpoint;

@SdkPublicApi
interface QueryEndpointProvider {
    Endpoint resolveEndpoint(QueryEndpointParameters endpointParams);
}
