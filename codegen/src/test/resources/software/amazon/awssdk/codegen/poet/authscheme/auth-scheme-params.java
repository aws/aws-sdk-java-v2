package software.amazon.awssdk.services.query.authscheme;

import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.query.authscheme.internal.DefaultQueryAuthSchemeParams;

/**
 * The parameters object used to resolve the auth schemes for the Query service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface QueryAuthSchemeParams {

    static Builder builder() {
        return DefaultQueryAuthSchemeParams.builder();
    }

    String operation();
    Optional<String> region();

    interface Builder {
        Builder operation(String operation);
        Builder region(String region);

        QueryAuthSchemeParams build();
    }
}
