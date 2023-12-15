import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface EndpointAuthSchemeStrategyFactory extends Supplier<EndpointAuthSchemeStrategy> {
   EndpointAuthSchemeStrategy endpointAuthSchemeStrategy();

   @Override
   default EndpointAuthSchemeStrategy get() {
       return endpointAuthSchemeStrategy();
   }
}
