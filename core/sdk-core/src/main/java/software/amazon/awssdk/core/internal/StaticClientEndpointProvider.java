package software.amazon.awssdk.core.internal;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class StaticClientEndpointProvider implements ClientEndpointProvider {
    private final URI clientEndpoint;
    private final boolean isEndpointOverridden;

    public StaticClientEndpointProvider(URI clientEndpoint, boolean isEndpointOverridden) {
        this.clientEndpoint = Validate.paramNotNull(clientEndpoint, "clientEndpoint");
        this.isEndpointOverridden = isEndpointOverridden;
        Validate.paramNotNull(clientEndpoint.getScheme(), "The URI scheme of endpointOverride");
    }

    @Override
    public URI clientEndpoint() {
        return this.clientEndpoint;
    }

    @Override
    public boolean isEndpointOverridden() {
        return this.isEndpointOverridden;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StaticClientEndpointProvider that = (StaticClientEndpointProvider) o;

        if (isEndpointOverridden != that.isEndpointOverridden) {
            return false;
        }
        return clientEndpoint.equals(that.clientEndpoint);
    }

    @Override
    public int hashCode() {
        int result = clientEndpoint.hashCode();
        result = 31 * result + (isEndpointOverridden ? 1 : 0);
        return result;
    }
}
