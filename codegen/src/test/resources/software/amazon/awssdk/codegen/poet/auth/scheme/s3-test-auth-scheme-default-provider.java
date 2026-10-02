package software.amazon.awssdk.services.s3.auth.scheme.internal;

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
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.authscheme.S3ExpressEndpointAuthScheme;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultS3AuthSchemeProvider implements S3AuthSchemeProvider {
    private static final DefaultS3AuthSchemeProvider DEFAULT = new DefaultS3AuthSchemeProvider();

    private static final S3AuthSchemeProvider FALLBACK_RESOLVER = FallbackS3AuthSchemeProvider.create();

    private static final S3EndpointProvider DELEGATE = S3EndpointProvider.defaultProvider();

    private DefaultS3AuthSchemeProvider() {
    }

    public static S3AuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams params) {
        S3EndpointParams endpointParameters = S3EndpointParams.builder().bucket(params.bucket()).region(params.region())
                                                              .useFips(params.useFips()).useDualStack(params.useDualStack()).endpoint(params.endpoint())
                                                              .forcePathStyle(params.forcePathStyle()).accelerate(params.accelerate())
                                                              .useGlobalEndpoint(params.useGlobalEndpoint()).useObjectLambdaEndpoint(params.useObjectLambdaEndpoint())
                                                              .key(params.key()).prefix(params.prefix()).copySource(params.copySource())
                                                              .disableAccessPoints(params.disableAccessPoints())
                                                              .disableMultiRegionAccessPoints(params.disableMultiRegionAccessPoints()).useArnRegion(params.useArnRegion())
                                                              .useS3ExpressControlEndpoint(params.useS3ExpressControlEndpoint())
                                                              .disableS3ExpressSessionAuth(params.disableS3ExpressSessionAuth()).deleteObjectKeys(params.deleteObjectKeys())
                                                              .build();
        Endpoint endpoint = CompletableFutureUtils.joinLikeSync(endpointProvider(params).resolveEndpoint(endpointParameters));
        List<EndpointAuthScheme> authSchemes = endpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
        if (authSchemes == null) {
            return FALLBACK_RESOLVER.resolveAuthScheme(params);
        }
        List<AuthSchemeOption> options = new ArrayList<>();
        for (EndpointAuthScheme authScheme : authSchemes) {
            String name = authScheme.name();
            switch (name) {
                case "sigv4":
                    SigV4AuthScheme sigv4AuthScheme = Validate.isInstanceOf(SigV4AuthScheme.class, authScheme,
                                                                            "Expecting auth scheme of class SigV4AuthScheme, got instead object of class %s", authScheme.getClass()
                                                                                                                                                                        .getName());
                    AuthSchemeOption sigv4AuthSchemeOption = applySigV4FamilyDefaults(
                        AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID)
                                        .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, sigv4AuthScheme.signingName())
                                        .putSignerProperty(AwsV4HttpSigner.REGION_NAME, sigv4AuthScheme.signingRegion())
                                        .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, !sigv4AuthScheme.disableDoubleEncoding()),
                        params).build();
                    options.add(sigv4AuthSchemeOption);
                    break;
                case "sigv4a":
                    SigV4aAuthScheme sigv4aAuthScheme = Validate.isInstanceOf(SigV4aAuthScheme.class, authScheme,
                                                                              "Expecting auth scheme of class SigV4AuthScheme, got instead object of class %s", authScheme.getClass()
                                                                                                                                                                          .getName());
                    RegionSet regionSet = Optional.ofNullable(params.regionSet()).orElseGet(
                        () -> Optional.ofNullable(sigv4aAuthScheme.signingRegionSet())
                                      .filter(set -> !CollectionUtils.isNullOrEmpty(set)).map(RegionSet::create).orElse(null));
                    AuthSchemeOption sigv4aAuthSchemeOption = applySigV4FamilyDefaults(
                        AuthSchemeOption.builder().schemeId(AwsV4aAuthScheme.SCHEME_ID)
                                        .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, sigv4aAuthScheme.signingName())
                                        .putSignerProperty(AwsV4aHttpSigner.REGION_SET, regionSet)
                                        .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, !sigv4aAuthScheme.disableDoubleEncoding()),
                        params).build();
                    options.add(sigv4aAuthSchemeOption);
                    break;
                case "sigv4-s3express":
                    S3ExpressEndpointAuthScheme s3ExpressAuthScheme = Validate.isInstanceOf(S3ExpressEndpointAuthScheme.class,
                                                                                            authScheme, "Expecting auth scheme of class S3ExpressAuthScheme, got instead object of class %s",
                                                                                            authScheme.getClass().getName());
                    AuthSchemeOption s3ExpressAuthSchemeOption = applySigV4FamilyDefaults(
                        AuthSchemeOption
                            .builder()
                            .schemeId(S3ExpressAuthScheme.SCHEME_ID)
                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, s3ExpressAuthScheme.signingName())
                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME, s3ExpressAuthScheme.signingRegion())
                            .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE,
                                               !s3ExpressAuthScheme.disableDoubleEncoding()), params).build();
                    options.add(s3ExpressAuthSchemeOption);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown auth scheme: " + name);
            }
        }
        return Collections.unmodifiableList(options);
    }

    private S3EndpointProvider endpointProvider(S3AuthSchemeParams params) {
        if (params instanceof S3EndpointResolverAware) {
            S3EndpointResolverAware endpointAwareParams = (S3EndpointResolverAware) params;
            S3EndpointProvider endpointProvider = endpointAwareParams.endpointProvider();
            if (endpointProvider != null) {
                return endpointProvider;
            }
        }
        return DELEGATE;
    }

    private static AuthSchemeOption.Builder applySigV4FamilyDefaults(AuthSchemeOption.Builder option, S3AuthSchemeParams params) {
        switch (params.operation()) {
            case "UploadPart":
            case "PutObject":
                option.putSignerPropertyIfAbsent(AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED, false)
                      .putSignerPropertyIfAbsent(AwsV4HttpSigner.DOUBLE_URL_ENCODE, false)
                      .putSignerPropertyIfAbsent(AwsV4HttpSigner.NORMALIZE_PATH, false)
                      .putSignerPropertyIfAbsent(AwsV4HttpSigner.CHUNK_ENCODING_ENABLED, true);
                return option;
            default:
                option.putSignerPropertyIfAbsent(AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED, false)
                      .putSignerPropertyIfAbsent(AwsV4HttpSigner.DOUBLE_URL_ENCODE, false)
                      .putSignerPropertyIfAbsent(AwsV4HttpSigner.NORMALIZE_PATH, false);
                return option;
        }
    }
}
