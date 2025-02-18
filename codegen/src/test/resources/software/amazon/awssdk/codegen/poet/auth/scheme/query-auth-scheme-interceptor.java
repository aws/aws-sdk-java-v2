package software.amazon.awssdk.services.query.auth.scheme.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeParams;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeProvider;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class QueryAuthSchemeInterceptor implements ExecutionInterceptor {
    private static Logger LOG = Logger.loggerFor(QueryAuthSchemeInterceptor.class);

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        List<AuthSchemeOption> authOptions = resolveAuthOptions(context, executionAttributes);
        SelectedAuthScheme<? extends Identity> selectedAuthScheme = selectAuthScheme(authOptions, executionAttributes);
        putSelectedAuthScheme(executionAttributes, selectedAuthScheme);
    }

    private List<AuthSchemeOption> resolveAuthOptions(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        QueryAuthSchemeProvider authSchemeProvider = Validate.isInstanceOf(QueryAuthSchemeProvider.class,
                                                                           executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER),
                                                                           "Expected an instance of QueryAuthSchemeProvider");
        QueryAuthSchemeParams params = authSchemeParams(context.request(), executionAttributes);
        return authSchemeProvider.resolveAuthScheme(params);
    }

    private SelectedAuthScheme<? extends Identity> selectAuthScheme(List<AuthSchemeOption> authOptions,
                                                                    ExecutionAttributes executionAttributes) {
        MetricCollector metricCollector = executionAttributes.getAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);
        Map<String, AuthScheme<?>> authSchemes = executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES);
        IdentityProviders identityProviders = executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);
        List<Supplier<String>> discardedReasons = new ArrayList<>();
        for (AuthSchemeOption authOption : authOptions) {
            AuthScheme<?> authScheme = authSchemes.get(authOption.schemeId());
            SelectedAuthScheme<? extends Identity> selectedAuthScheme = trySelectAuthScheme(authOption, authScheme,
                                                                                            identityProviders, discardedReasons, metricCollector, executionAttributes);
            if (selectedAuthScheme != null) {
                if (!discardedReasons.isEmpty()) {
                    LOG.debug(() -> String.format("%s auth will be used, discarded: '%s'", authOption.schemeId(),
                                                  discardedReasons.stream().map(Supplier::get).collect(Collectors.joining(", "))));
                }
                return selectedAuthScheme;
            }
        }
        throw SdkException
            .builder()
            .message(
                "Failed to determine how to authenticate the user: "
                + discardedReasons.stream().map(Supplier::get).collect(Collectors.joining(", "))).build();
    }

    private QueryAuthSchemeParams authSchemeParams(SdkRequest request, ExecutionAttributes executionAttributes) {
        String operation = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
        QueryAuthSchemeParams.Builder builder = QueryAuthSchemeParams.builder().operation(operation);
        Region region = executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION);
        builder.region(region);
        return builder.build();
    }
    private <T extends Identity> SelectedAuthScheme<T> trySelectAuthScheme(AuthSchemeOption authOption, AuthScheme<T> authScheme,
                                                                           IdentityProviders identityProviders, List<Supplier<String>> discardedReasons, MetricCollector metricCollector,
                                                                           ExecutionAttributes executionAttributes) {
        if (authScheme == null) {
            discardedReasons.add(() -> String.format("'%s' is not enabled for this request.", authOption.schemeId()));
            return null;
        }
        IdentityProvider<T> identityProvider = authScheme.identityProvider(identityProviders);
        if (identityProvider == null) {
            discardedReasons
                .add(() -> String.format("'%s' does not have an identity provider configured.", authOption.schemeId()));
            return null;
        }
        HttpSigner<T> signer;
        try {
            signer = authScheme.signer();
        } catch (RuntimeException e) {
            discardedReasons.add(() -> String.format("'%s' signer could not be retrieved: %s", authOption.schemeId(),
                                                     e.getMessage()));
            return null;
        }
        ResolveIdentityRequest.Builder identityRequestBuilder = ResolveIdentityRequest.builder();
        authOption.forEachIdentityProperty(identityRequestBuilder::putProperty);
        CompletableFuture<? extends T> identity;
        SdkMetric<Duration> metric = getIdentityMetric(identityProvider);
        if (metric == null) {
            identity = identityProvider.resolveIdentity(identityRequestBuilder.build());
        } else {
            identity = MetricUtils.reportDuration(() -> identityProvider.resolveIdentity(identityRequestBuilder.build()),
                                                  metricCollector, metric);
        }
        return new SelectedAuthScheme<>(identity, signer, authOption);
    }

    private SdkMetric<Duration> getIdentityMetric(IdentityProvider<?> identityProvider) {
        Class<?> identityType = identityProvider.identityType();
        if (identityType == AwsCredentialsIdentity.class) {
            return CoreMetric.CREDENTIALS_FETCH_DURATION;
        }
        if (identityType == TokenIdentity.class) {
            return CoreMetric.TOKEN_FETCH_DURATION;
        }
        return null;
    }

    private <T extends Identity> void putSelectedAuthScheme(ExecutionAttributes attributes,
                                                            SelectedAuthScheme<T> selectedAuthScheme) {
        SelectedAuthScheme<?> existingAuthScheme = attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (existingAuthScheme != null) {
            AuthSchemeOption.Builder selectedOption = selectedAuthScheme.authSchemeOption().toBuilder();
            existingAuthScheme.authSchemeOption().forEachIdentityProperty(selectedOption::putIdentityPropertyIfAbsent);
            existingAuthScheme.authSchemeOption().forEachSignerProperty(selectedOption::putSignerPropertyIfAbsent);
            selectedAuthScheme = new SelectedAuthScheme<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(),
                                                          selectedOption.build());
        }
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);
    }
}
