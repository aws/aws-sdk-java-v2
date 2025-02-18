package software.amazon.awssdk.services.query.auth.scheme.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeParams;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeProvider;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryAuthSchemeProvider implements QueryAuthSchemeProvider {
    private static final DefaultQueryAuthSchemeProvider DEFAULT = new DefaultQueryAuthSchemeProvider();

    private static final QueryAuthSchemeProvider MODELED_RESOLVER = ModeledQueryAuthSchemeProvider.create();

    private static final QueryEndpointProvider DELEGATE = QueryEndpointProvider.defaultProvider();

    private DefaultQueryAuthSchemeProvider() {
    }

    public static QueryAuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(QueryAuthSchemeParams params) {
        QueryEndpointParams endpointParameters = QueryEndpointParams.builder().region(params.region())
                                                                    .defaultTrueParam(params.defaultTrueParam()).defaultStringParam(params.defaultStringParam())
                                                                    .deprecatedParam(params.deprecatedParam()).booleanContextParam(params.booleanContextParam())
                                                                    .stringContextParam(params.stringContextParam()).operationContextParam(params.operationContextParam()).build();
        Endpoint endpoint = CompletableFutureUtils.joinLikeSync(endpointProvider(params).resolveEndpoint(endpointParameters));
        List<EndpointAuthScheme> authSchemes = endpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
        if (authSchemes == null) {
            return MODELED_RESOLVER.resolveAuthScheme(params);
        }
        List<AuthSchemeOption> options = new ArrayList<>();
        for (EndpointAuthScheme authScheme : authSchemes) {
            String name = authScheme.name();
            switch (name) {
                case "sigv4":
                    SigV4AuthScheme sigv4AuthScheme = Validate.isInstanceOf(SigV4AuthScheme.class, authScheme,
                                                                            "Expecting auth scheme of class SigV4AuthScheme, got instead object of class %s", authScheme.getClass()
                                                                                                                                                                        .getName());
                    AuthSchemeOption sigv4AuthSchemeOption = AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID)
                                                                             .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, sigv4AuthScheme.signingName())
                                                                             .putSignerProperty(AwsV4HttpSigner.REGION_NAME, sigv4AuthScheme.signingRegion())
                                                                             .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, !sigv4AuthScheme.disableDoubleEncoding()).build();
                    options.add(sigv4AuthSchemeOption);
                    break;
                case "sigv4a":
                    SigV4aAuthScheme sigv4aAuthScheme = Validate.isInstanceOf(SigV4aAuthScheme.class, authScheme,
                                                                              "Expecting auth scheme of class SigV4AuthScheme, got instead object of class %s", authScheme.getClass()
                                                                                                                                                                          .getName());
                    RegionSet regionSet = Optional.ofNullable(params.regionSet()).orElseGet(
                        () -> Optional.ofNullable(sigv4aAuthScheme.signingRegionSet())
                                      .filter(set -> !CollectionUtils.isNullOrEmpty(set)).map(RegionSet::create).orElse(null));
                    AuthSchemeOption sigv4aAuthSchemeOption = AuthSchemeOption.builder().schemeId(AwsV4aAuthScheme.SCHEME_ID)
                                                                              .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, sigv4aAuthScheme.signingName())
                                                                              .putSignerProperty(AwsV4aHttpSigner.REGION_SET, regionSet)
                                                                              .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, !sigv4aAuthScheme.disableDoubleEncoding()).build();
                    options.add(sigv4aAuthSchemeOption);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown auth scheme: " + name);
            }
        }
        return Collections.unmodifiableList(options);
    }

    private QueryEndpointProvider endpointProvider(QueryAuthSchemeParams params) {
        if (params instanceof QueryEndpointResolverAware) {
            QueryEndpointResolverAware endpointAwareParams = (QueryEndpointResolverAware) params;
            QueryEndpointProvider endpointProvider = endpointAwareParams.endpointProvider();
            if (endpointProvider != null) {
                return endpointProvider;
            }
        }
        return DELEGATE;
    }
}
