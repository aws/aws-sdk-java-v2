import software.amazon.awssdk.endpoints.EndpointAttributeKey;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Link between an endpoint property and the {@link AwsEndpointAttribute} it represents.
 *
 @param <T> the {@link AwsEndpointAttribute} type
 */
@SdkInternalApi
public interface EndpointAttributeProvider<T> {
    String propertyName();
    EndpointAttributeKey<T> attributeKey();
    T attributeValue(Value value);
}
