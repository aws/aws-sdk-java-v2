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

    /**
     * Get a new builder for creating a {@link QueryAuthSchemeParams}.
     */
    static Builder builder() {
        return DefaultQueryAuthSchemeParams.builder();
    }

    /**
     * Returns the operation for which to resolve the auth scheme.
     */
    String operation();

    /**
     * Returns the region. The region is optional. The region parameter may be used with "aws.auth#sigv4" auth scheme.
     * By default, the region will be empty.
     */
    Optional<String> region();

    /**
     * A builder for a {@link QueryAuthSchemeParams}.
     */
    interface Builder {
        /**
         * Set the operation for which to resolve the auth scheme.
         */
        Builder operation(String operation);

        /**
         * Set the region. The region parameter may be used with the "aws.auth#sigv4" auth scheme.
         */
        Builder region(String region);

        /**
         * Returns a {@link QueryAuthSchemeParams} object that is created from the properties that have been set on the builder.
         */
        QueryAuthSchemeParams build();
    }
}
