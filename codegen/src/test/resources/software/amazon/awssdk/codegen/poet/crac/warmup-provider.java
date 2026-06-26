package software.amazon.awssdk.services.query.internal.crac;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class QueryWarmUpProvider implements SdkWarmUpProvider {
    /**
     * Warms up the service's request path so the Just-In-Time compiled code is captured in a CRaC snapshot.
     */
    @Override
    public void warmUp() {
    }
}
