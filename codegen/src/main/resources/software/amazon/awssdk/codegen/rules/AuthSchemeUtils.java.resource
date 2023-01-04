import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.utils.Logger;

@SdkProtectedApi
public final class AuthSchemeUtils {
    private static final Logger LOG = Logger.loggerFor(AuthSchemeUtils.class);

    private static final String SIGV4_NAME = "sigv4";
    private static final String SIGV4A_NAME = "sigv4a";
    private static final Set<String> KNOWN_AUTH_SCHEMES;

    static {
        Set<String> schemes = new HashSet<>();
        schemes.add(SIGV4_NAME);
        schemes.add(SIGV4A_NAME);
        KNOWN_AUTH_SCHEMES = Collections.unmodifiableSet(schemes);
    }

    private AuthSchemeUtils() {
    }

    /**
     * Per the spec, the auth schemes list is ordered by preference, so we simply iterate over the list until we find an
     * auth scheme we recognize.
     */
    public static EndpointAuthScheme chooseAuthScheme(List<EndpointAuthScheme> authSchemes) {
        for (EndpointAuthScheme authScheme : authSchemes) {
            if (KNOWN_AUTH_SCHEMES.contains(authScheme.name())) {
                return authScheme;
            }
        }
        throw SdkClientException.create("Endpoint did not contain any known auth schemes: " + authSchemes);
    }

    public static List<EndpointAuthScheme> createAuthSchemes(Value authSchemesValue) {
        Value.Array schemesArray = authSchemesValue.expectArray();

        List<EndpointAuthScheme> authSchemes = new ArrayList<>();
        for (int i = 0; i < schemesArray.size(); ++i) {
            Value.Record scheme = schemesArray.get(i).expectRecord();

            String authSchemeName = scheme.get(Identifier.of("name")).expectString();
            switch (authSchemeName) {
                case SIGV4A_NAME: {
                    SigV4aAuthScheme.Builder schemeBuilder = SigV4aAuthScheme.builder();

                    Value signingName = scheme.get(Identifier.of("signingName"));
                    if (signingName != null) {
                        schemeBuilder.signingName(signingName.expectString());
                    }

                    Value signingRegionSet = scheme.get(Identifier.of("signingRegionSet"));
                    if (signingRegionSet != null) {
                        Value.Array signingRegionSetArray = signingRegionSet.expectArray();
                        for (int j = 0; j < signingRegionSetArray.size(); ++j) {
                            schemeBuilder.addSigningRegion(signingRegionSetArray.get(j).expectString());
                        }
                    }

                    Value disableDoubleEncoding = scheme.get(Identifier.of("disableDoubleEncoding"));
                    if (disableDoubleEncoding != null) {
                        schemeBuilder.disableDoubleEncoding(disableDoubleEncoding.expectBool());
                    }

                    authSchemes.add(schemeBuilder.build());
                }
                break;
                case SIGV4_NAME: {
                    SigV4AuthScheme.Builder schemeBuilder = SigV4AuthScheme.builder();

                    Value signingName = scheme.get(Identifier.of("signingName"));
                    if (signingName != null) {
                        schemeBuilder.signingName(signingName.expectString());
                    }

                    Value signingRegion = scheme.get(Identifier.of("signingRegion"));
                    if (signingRegion != null) {
                        schemeBuilder.signingRegion(signingRegion.expectString());
                    }

                    Value disableDoubleEncoding = scheme.get(Identifier.of("disableDoubleEncoding"));
                    if (disableDoubleEncoding != null) {
                        schemeBuilder.disableDoubleEncoding(disableDoubleEncoding.expectBool());
                    }

                    authSchemes.add(schemeBuilder.build());
                }
                break;
                default:
                    LOG.debug(() -> "Ignoring unknown auth scheme: " + authSchemeName);
                    break;
            }
        }
        return authSchemes;
    }

    public static void setSigningParams(ExecutionAttributes executionAttributes, EndpointAuthScheme authScheme) {
        if (authScheme instanceof SigV4AuthScheme) {
            setSigV4SigningParams(executionAttributes, (SigV4AuthScheme) authScheme);
        } else if (authScheme instanceof SigV4aAuthScheme) {
            setSigV4aAuthSigningParams(executionAttributes, (SigV4aAuthScheme) authScheme);
        } else {
            throw SdkClientException.create("Don't know how to set signing params for auth scheme: " + authScheme.name());
        }
    }

    private static void setSigV4SigningParams(ExecutionAttributes executionAttributes, SigV4AuthScheme sigV4AuthScheme) {
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE,
                                         !sigV4AuthScheme.disableDoubleEncoding());

        if (sigV4AuthScheme.signingName() != null) {
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, sigV4AuthScheme.signingName());
        }

        if (sigV4AuthScheme.signingRegion() != null) {
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION,
                                             Region.of(sigV4AuthScheme.signingRegion()));
        }
    }

    private static void setSigV4aAuthSigningParams(ExecutionAttributes executionAttributes, SigV4aAuthScheme sigV4aAuthScheme) {
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE,
                                         !sigV4aAuthScheme.disableDoubleEncoding());

        if (sigV4aAuthScheme.signingName() != null) {
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, sigV4aAuthScheme.signingName());
        }

        if (sigV4aAuthScheme.signingRegionSet() != null) {
            if (sigV4aAuthScheme.signingRegionSet().size() > 1) {
                throw SdkClientException.create("Don't know how to set scope of > 1 region");
            }

            if (sigV4aAuthScheme.signingRegionSet().isEmpty()) {
                throw SdkClientException.create("Signing region set is empty");
            }

            String scope = sigV4aAuthScheme.signingRegionSet().get(0);
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE, RegionScope.create(scope));
        }
    }
}
