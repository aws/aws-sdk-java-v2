import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.Logger;

// TODO(sra-identity-auth): seems like this can be SdkInternalApi, similar to other .resource files in this folder,
//  since they are generated in each service module
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

    public static <T extends Identity> void putSelectedAuthScheme(ExecutionAttributes attributes,
                                                                  SelectedAuthScheme<T> selectedAuthScheme) {
        SelectedAuthScheme<?> existingAuthScheme = attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (existingAuthScheme != null) {
            AuthSchemeOption.Builder selectedOption = selectedAuthScheme.authSchemeOption().toBuilder();
            existingAuthScheme.authSchemeOption().forEachIdentityProperty(selectedOption::putIdentityPropertyIfAbsent);
            existingAuthScheme.authSchemeOption().forEachSignerProperty(selectedOption::putSignerPropertyIfAbsent);
            selectedAuthScheme = new SelectedAuthScheme<>(selectedAuthScheme.identity(),
                                                          selectedAuthScheme.signer(),
                                                          selectedOption.build());
        }

        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);
    }
}
