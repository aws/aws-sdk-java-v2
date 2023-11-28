import java.util.List;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface EndpointAuthSchemeStrategy {
   EndpointAuthScheme chooseAuthScheme(List<EndpointAuthScheme> authSchemes);

   List<EndpointAuthScheme> createAuthSchemes(Value authSchemesValue);
}
