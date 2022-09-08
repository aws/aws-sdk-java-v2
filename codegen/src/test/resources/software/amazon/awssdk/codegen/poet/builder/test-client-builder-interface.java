package software.amazon.awssdk.services.json;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.services.json.rules.JsonEndpointProvider;

/**
 * This includes configuration specific to Json Service that is supported by both {@link JsonClientBuilder} and
 * {@link JsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
public interface JsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends AwsClientBuilder<B, C> {
    B serviceConfiguration(ServiceConfiguration serviceConfiguration);

    default B serviceConfiguration(Consumer<ServiceConfiguration.Builder> serviceConfiguration) {
        return serviceConfiguration(ServiceConfiguration.builder().applyMutation(serviceConfiguration).build());
    }

    /**
     * Set the {@link JsonEndpointProvider} implementation that will be used by the client to determine the endpoint for
     * each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    B endpointProvider(JsonEndpointProvider endpointProvider);
}
