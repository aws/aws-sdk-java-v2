package software.amazon.awssdk.services.json.internal;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.identity.spi.TokenIdentity;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class EnvironmentTokenMetricsInterceptor implements ExecutionInterceptor {
    private final String tokenFromEnv;

    public EnvironmentTokenMetricsInterceptor(String tokenFromEnv) {
        this.tokenFromEnv = tokenFromEnv;
    }

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        SelectedAuthScheme<?> selectedAuthScheme = executionAttributes
            .getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (selectedAuthScheme != null && selectedAuthScheme.authSchemeOption().schemeId().equals(BearerAuthScheme.SCHEME_ID)
            && selectedAuthScheme.identity().isDone()) {
            if (selectedAuthScheme.identity().getNow(null) instanceof TokenIdentity) {
                TokenIdentity configuredToken = (TokenIdentity) selectedAuthScheme.identity().getNow(null);
                if (configuredToken.token().equals(tokenFromEnv)) {
                    executionAttributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS).addMetric(
                        BusinessMetricFeatureId.BEARER_SERVICE_ENV_VARS.value());
                }
            }
        }
    }
}
