import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class DefaultEndpointAuthSchemeStrategy implements EndpointAuthSchemeStrategy {
    private static final Logger LOG = Logger.loggerFor(DefaultEndpointAuthSchemeStrategy.class);

    private final Map<String, Function<Value.Record, EndpointAuthScheme>> knownAuthSchemesMapping;

    public DefaultEndpointAuthSchemeStrategy(Map<String, Function<Value.Record, EndpointAuthScheme>> knownAuthSchemesMapping) {
        this.knownAuthSchemesMapping = knownAuthSchemesMapping;
    }

    @Override
    public EndpointAuthScheme chooseAuthScheme(List<EndpointAuthScheme> authSchemes) {
        return authSchemes.stream()
                          .filter(scheme -> knownAuthSchemesMapping.containsKey(scheme.name()))
                          .findFirst()
                          .orElseThrow(() -> SdkClientException.create("Endpoint did not contain any known auth schemes: " + authSchemes));
    }

    @Override
    public List<EndpointAuthScheme> createAuthSchemes(Value authSchemesValue) {
        Value.Array schemesArray = authSchemesValue.expectArray();
        List<EndpointAuthScheme> authSchemes = new ArrayList<>();
        for (int i = 0; i < schemesArray.size(); ++i) {
            Value.Record scheme = schemesArray.get(i).expectRecord();
            String authSchemeName = scheme.get(Identifier.of("name")).expectString();
            Function<Value.Record, EndpointAuthScheme> mapper = knownAuthSchemesMapping.get(authSchemeName);
            if (mapper == null) {
                LOG.debug(() -> "Ignoring unknown auth scheme: " + authSchemeName);
                continue;
            }
            authSchemes.add(mapper.apply(scheme));
        }
        return authSchemes;
    }
}

