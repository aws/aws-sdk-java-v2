package software.amazon.awssdk.services.query.auth.scheme.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeParams;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeProvider;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
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
                                                                    .useDualStackEndpoint(params.useDualStackEndpoint()).useFipsEndpoint(params.useFipsEndpoint())
                                                                    .credentialScope(params.credentialScope()).endpointId(params.endpointId())
                                                                    .defaultTrueParam(params.defaultTrueParam()).defaultStringParam(params.defaultStringParam())
                                                                    .deprecatedParam(params.deprecatedParam()).booleanContextParam(params.booleanContextParam())
                                                                    .stringContextParam(params.stringContextParam()).operationContextParam(params.operationContextParam()).build();
        Endpoint endpoint = CompletableFutureUtils.joinLikeSync(DELEGATE.resolveEndpoint(endpointParameters));
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
                    options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                                .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, sigv4AuthScheme.signingName())
                                                .putSignerProperty(AwsV4HttpSigner.REGION_NAME, sigv4AuthScheme.signingRegion())
                                                .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, !sigv4AuthScheme.disableDoubleEncoding()).build());
                    break;
                case "sigv4a":
                    SigV4aAuthScheme sigv4aAuthScheme = Validate.isInstanceOf(SigV4aAuthScheme.class, authScheme,
                                                                              "Expecting auth scheme of class SigV4AuthScheme, got instead object of class %s", authScheme.getClass()
                                                                                                                                                                          .getName());
                    RegionSet regionSet = RegionSet.create(sigv4aAuthScheme.signingRegionSet());
                    options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4a")
                                                .putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, sigv4aAuthScheme.signingName())
                                                .putSignerProperty(AwsV4aHttpSigner.REGION_SET, regionSet)
                                                .putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, !sigv4aAuthScheme.disableDoubleEncoding()).build());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown auth scheme: " + name);
            }
        }
        return Collections.unmodifiableList(options);
    }
}
